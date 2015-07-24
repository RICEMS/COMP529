/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2014, Eric Daugherty (http://www.ericdaugherty.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of the copyright holder nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************
 * For current versions and more information, please visit:
 * http://javaemailserver.sf.net/
 *
 * or contact the author at:
 * andreaskyrmegalos@hotmail.com
 *
 ******************************************************************************
 * This program is based on the CSRMail project written by Calvin Smith.
 * http://crsemail.sourceforge.net/
 ******************************************************************************
 *
 * $Rev$
 * $Date$
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.configuration;

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.utils.IOUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Add a user and optionally an association with a (new) realm using
 * a custom communication protocol over a tcp connection.
 *
 * @author Andreas Kyrmegalos
 */
public class ConnectionBasedConfigurator implements Runnable {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog(ConnectionBasedConfigurator.class);
   /** The server socket used to listen for incoming connections */
   private ServerSocket serverSocket;
   private static final SSLServerSocketFactory sslServerSocketFactory =
         ConfigurationManager.getInstance().getSSLServerSocketFactory();
   /** Reader to read data from the client */
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   private volatile boolean shutdown;
   private final ExecutorService es;
   private final boolean secure;

   static final class ExecutorThreadFactory implements ThreadFactory {

      private final AtomicInteger ai = new AtomicInteger(1);
      private final ThreadGroup tg;

      public ExecutorThreadFactory() {
         tg = new ThreadGroup("CBCGroup");
         tg.setMaxPriority(Thread.currentThread().getPriority() - 1);
         tg.setDaemon(false);
      }

      @Override
      public Thread newThread(Runnable runnable) {
         return new Thread(tg, runnable, tg.getName() + "-" + ai.getAndIncrement());
      }
   }
   
   private static class CBCFutureTask<V> extends FutureTask<V> {
      
      private final Socket socket;

      public CBCFutureTask(CBCCallable<V> callable) {
         super(callable);
         this.socket = callable.getSocket();
      }
      
      public Socket getSocket() {
         return socket;
      }
   }
   
   private static class CBCThreadPoolExecutor extends ThreadPoolExecutor {

      private final List<Socket> liveSockets = new ArrayList<Socket>();
      
      private final ReentrantLock lock = new ReentrantLock();

      public CBCThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
         super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
      }

      @Override
      protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
         if (!callable.getClass().equals(CBCCallable.class))
            throw new UnsupportedOperationException("CBCThreadPoolExecutor only accepts instances of CBCCallable.");
         return new CBCFutureTask<T>((CBCCallable)callable);
      }

      @Override
      protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
         throw new UnsupportedOperationException("CBCThreadPoolExecutor does not support a Runnable.");
      }

      @Override
      protected void beforeExecute(Thread t, Runnable r) {
         
         if (!(r instanceof CBCFutureTask))
            return;
         
         lock.lock();
         try {
            liveSockets.add(((CBCFutureTask)r).getSocket());
         } finally {
            lock.unlock();
         }
      }
      
      @Override
      protected void afterExecute(Runnable r, Throwable t) {

         if (t != null)
            log.error("", t);
         if (!(r instanceof CBCFutureTask))
            return;
         
         lock.lock();
         try {
            liveSockets.remove(((CBCFutureTask)r).getSocket());
         } finally {
            lock.unlock();
         }
      }
      
      @Override
      public List<Runnable> shutdownNow() {
         commonShutdown();
         return super.shutdownNow();
      }

      @Override
      public void shutdown() {
         commonShutdown();
         super.shutdown();
      }

      private void commonShutdown() {
         
         Iterator<Socket> iter = liveSockets.iterator();
         synchronized(this) {
            while(iter.hasNext()) {
               IOUtils.close(iter.next());
            }
            liveSockets.clear();
         }
      }
   }
   
   private AccessControlContext acc;

   public ConnectionBasedConfigurator(boolean secure) {

      this.secure = secure;
      PermissionCollection pc = new Permissions();
      InetAddress listenAddress = cm.getConfigurationAddress();
      if (listenAddress == null) {
         throw new RuntimeException("The CBC address can not be zero based");
      }
      pc.add(new SocketPermission("localhost:" + cm.getConfigurationPort(), "listen,resolve"));
      if (!secure) {
         pc.add(new SocketPermission(listenAddress.getHostAddress() + ":*", "accept,resolve"));
      }
      else {
         pc.add(new SocketPermission("*", "accept,resolve"));
      }
      acc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, pc)});

      es = new CBCThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ExecutorThreadFactory());

      try {

         acc.checkPermission(new SocketPermission("localhost:" + cm.getConfigurationPort(), "listen,resolve"));
         if (!secure) {
            serverSocket = new ServerSocket(cm.getConfigurationPort(), 5, listenAddress);
         }
         else {
            serverSocket = sslServerSocketFactory.createServerSocket(cm.getConfigurationPort(), 5, listenAddress);
            ((SSLServerSocket)serverSocket).
                     setEnabledCipherSuites(ConfigurationManager.getInstance().getEnabledCiphers());
            ((SSLServerSocket)serverSocket).
                     setEnabledProtocols(ConfigurationManager.getInstance().getEnabledProtocols());
            ((SSLServerSocket)serverSocket).setNeedClientAuth(true);
         }

         log.info("CBC will be listening for changes.");
      } catch (IOException e) {
         log.error("Could not initiate ConnectionBasedConfigurator ", e);
         Mail.getInstance().shutdown();
      }
   }

   public void shutdown() {
      log.warn("Remote Configurator going offline");
      shutdown = true;
      IOUtils.close(serverSocket);
      List<Runnable> rs = es.shutdownNow();
      for(Runnable r : rs) {
         if (!(r instanceof CBCFutureTask)) {
            return;
         }
         IOUtils.close(((CBCFutureTask)r).getSocket());
      }
   }

   @Override
   public void run() {
      try {
         //Set the socket to timeout every 10 seconds so it does not
         //just block forever.
         serverSocket.setSoTimeout(10 * 1000);
      } catch (SocketException e) {
         log.error("Error initializing Socket Timeout in ConnectionBasedConfigurator");
         throw new RuntimeException("Error initializing Socket Timeout in ConnectionBasedConfigurator");
      }

      do {

         Socket socket;
         try {
            socket = serverSocket.accept();
            acc.checkPermission(new SocketPermission(socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), "accept,resolve"));
            socket.setSoTimeout(2 * 60 * 1000);
            log.info("Configuration Connection Established. Connecting client: "+socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

            if (secure) {
               //Only applies to future connections attempting to use this session
               ((SSLSocket)socket).getSession().invalidate();
            }
            if (shutdown) {
               IOUtils.close(socket);
               return;
            }
            Future submit = es.submit(new CBCCallable(socket));
         } catch (SocketTimeoutException e) {
            //Do not log
         } catch (IOException e) {
            if (!shutdown) {
               log.error(e.getMessage(), e);
            }
         } catch (AccessControlException e) {
            if (!shutdown) {
               log.error(e.getMessage(), e);
            }
         }
      } while (!shutdown);
   }
}

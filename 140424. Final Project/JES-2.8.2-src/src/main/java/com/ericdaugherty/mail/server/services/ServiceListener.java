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

package com.ericdaugherty.mail.server.services;

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.services.pop3.Pop3Processor;
import com.ericdaugherty.mail.server.services.smtp.server.RFC5321SMTPServerSessionControl;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class listens for incoming connections on the specified port and
 * starts a new thread to process the request.  This class abstracts common
 * functionality required to start any type of service (POP3 or SMTP), reducing
 * the requirement to duplicate this code in each package.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrrmegalos (2.x branch)
 * @param <T>
 */
public class ServiceListener<T extends ConnectionProcessor> implements Runnable {

   /** Logger Category for this class. */
   private static Log log = LogFactory.getLog( ServiceListener.class );
    
   private static int totalSL;

   private static final Object lock = new Object();

   private static AtomicInteger ai;

   private static SSLServerSocketFactory sslServerSocketFactory =
         ConfigurationManager.getInstance().getSSLServerSocketFactory();

   public static void setTotalSL(int totalSL) {
      ServiceListener.totalSL = totalSL;
      ai = new AtomicInteger();
   }

   private static boolean isSLsloadingComplete() {
      return ai.get() == totalSL;
   }
    
   public static void waitAllStart() {

      if (!isSLsloadingComplete()) {
         
         if (log.isDebugEnabled())
            log.debug("ServiceListener Entering lock state.");
         synchronized (lock) {
            while (!isSLsloadingComplete()
                  && (Mail.getInstance() != null && !Mail.getInstance().isShuttingDown())) {
               try {
                  lock.wait(10 * 1000L);
               } catch (InterruptedException ex) {
                  if (log.isDebugEnabled())
                     log.debug(ex.getMessage(), ex);
               }
            } 
         }
      }
      if (log.isDebugEnabled())
         log.debug("All ServiceListener instances completed initialization.");
   }

    private final ConfigurationManager cm = ConfigurationManager.getInstance();

    private boolean useSSL;
    
    private boolean delay;
    
    private boolean initialized = true;

    /** Array of processors */
    private ConnectionProcessor[] processors;

    /** The port to listen on for incoming connections. */
    private int port;

    /** The type of class to use to handle requests. */
    private Class<T> cpc;
    
    /** An indicator that affects generation of a new SMTPProcessor instance. */
    private boolean amavis;

    /** The number of threads to create to listen on this port. */
    private int threads;

    /** Thread pool */
    private Thread[] threadPool;

    /** server socket */
    private ServerSocket serverSocket;
    
    private final String name;
    
    /**
     * Creates a new instance and stores the initial parameters.
    * @param name
    * @param port
    * @param cpc
    * @param threads
    * @param useSSL
    * @param delay
     */
    public ServiceListener(String name, int port, Class<T> cpc, int threads,
          boolean useSSL, boolean delay) {

       this.name = name;
       this.port = port;
       this.cpc = cpc;
       this.threads = threads;
       this.useSSL = useSSL;
       this.delay = delay;
    }

    /**
     * Creates a new instance and stores the initial parameters.
    * @param name
    * @param port
    * @param cpc
    * @param useSSL
    * @param amavis
    * @param threads
    * @param delay
     */
    public ServiceListener(String name, int port, Class<T> cpc, boolean amavis,
          int threads, boolean useSSL, boolean delay) {
       this(name, port, cpc, threads, useSSL, delay);
       this.amavis = amavis;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getPort() {
       return port;
    }
    
    public String getName() {
       return name;
    }

    /**
     * Entry point for the thread.  Listens for incoming connections and
     * starts a new handler thread for each.
     */
    @Override
    public void run() {

        if(log.isDebugEnabled())
           log.debug( "Starting ServiceListener on port: " + port );

        InetAddress listenAddress = cm.getListenAddress();
        try {
            serverSocket = setupServerSocket(listenAddress);
        }
        catch (IOException e) {
            String address = "localhost";
            if( listenAddress != null ) {
                address = listenAddress.getHostAddress();
            }
            log.error("Could not create ServiceListener on address: " + address + " port: " +port );
            if (!Mail.isStarted()) {
               initialized = false;
               
               synchronized(lock) {
                  if (ai.incrementAndGet() == totalSL) {
                     lock.notifyAll();
                  }
               }
               return;
            }
            throw new RuntimeException(e.getMessage());
        }

        log.info( "Accepting Connections on port: " + port );

        ConnectionProcessor processor;
        long threadCount = 0;
        String threadNameBase = Thread.currentThread().getName();

        //Initialize threadpools.
        try {

            processors = new ConnectionProcessor[threads];
            threadPool = new Thread[ threads ];

            for( int index = 0; index < threads; index++ ) {
                //Create the handler now to speed up connection time.
                processor = cpc.newInstance();
                processors[index] = processor;
                if (!processor.getClass().equals(Pop3Processor.class)) {
                   ((RFC5321SMTPServerSessionControl)processor).setUseAmavisSMTPDirectory(amavis);
                   ((RFC5321SMTPServerSessionControl)processor).setupVerifyIP();
                   if (delay)
                      processor.setDelayedStart(true);
                }
                processor.setSocket(serverSocket);

                //Create, name, and start a new thread to handle this request.
                threadPool[index] = new Thread(processor, threadNameBase + ":" + ++threadCount );
                threadPool[index].start();
            }
        }
        catch (Exception e) {
            log.error("ServiceListener Connection failed on port: " + port + ".", e );
            if (!Mail.isStarted())
               initialized = false;
        }
        if (log.isDebugEnabled())
           log.debug("A ServiceListener instance completed initialization");
        if (ai.incrementAndGet()==totalSL) {
           synchronized(lock) {
              lock.notifyAll();
           }
        }
    }

   private ServerSocket setupServerSocket(InetAddress listenAddress) throws IOException{

      ServerSocket serverSocket;
      if (!useSSL || sslServerSocketFactory == null) {
         
         // 50 is the default backlog size.
          serverSocket = new ServerSocket( port, 50, listenAddress );
      }
      else {

         // 50 is the default backlog size.
         serverSocket = sslServerSocketFactory.createServerSocket(port, 50, listenAddress);
         if ("required".equals(cpc.equals(RFC5321SMTPServerSessionControl.class)?cm.getClientAuthSMTP():cm.getClientAuthPOP3())) {
           ((SSLServerSocket)serverSocket).setNeedClientAuth(true);
         } else if ("requested".equals(cpc.equals(RFC5321SMTPServerSessionControl.class)
               ? cm.getClientAuthSMTP()
               : cm.getClientAuthPOP3())) {
            ((SSLServerSocket)serverSocket).setWantClientAuth(true);
         }
         ((SSLServerSocket)serverSocket).
               setUseClientMode(false);
         ((SSLServerSocket)serverSocket).
               setEnabledCipherSuites(cm.getEnabledCiphers());
         ((SSLServerSocket)serverSocket).
               setEnabledProtocols(ConfigurationManager.getInstance().getEnabledProtocols());
      }
      try {
         serverSocket.setSoTimeout( 10 * 1000 );
         return serverSocket;
      }
      catch (SocketException e) {
         log.error("Error while trying to set server socket timeout");
         throw e;
      }
   }
   
   public void notifyOnDomainDeleted(Domain domain) {
      if (processors != null) { 
           for (ConnectionProcessor processor : processors) {
              if (processor != null) {
                 processor.invalidate(domain);
              }
           }
        }
   }
    
    /**
     * This method notifies all processors to initiate shutdown.
     */
    public void notifyShutdown() {
       
        if (processors != null) { 
           for (ConnectionProcessor processor : processors) {
              if (processor != null) {
                 processor.shutdown();
              }
           }
        }
        IOUtils.close(serverSocket);
    }

    /**
     * All processors are allowed to stop in sequence.
     */
    public void initiateShutdown() {
       if (processors == null)
          return;
        for( int index = 0; index < threadPool.length; index++ ) {

            try{
               if (threadPool[index] != null) {
                   threadPool[index].join();
               }
            }
            catch (InterruptedException ie) {
               log.error("Was interrupted while waiting for thread to die");
            }
            threadPool[index] = null;
        }
    }
    
    public static void shutdown() {
        sslServerSocketFactory = null;
    }

   public void updateServerSocket(Map<String, Integer> portMap) {

      Integer newPort = portMap.get(name);
      if (newPort == null)
       return;
      final int port = newPort;
                
      Runnable runnable =  new Runnable() {

         @Override
         public void run() {
            InetAddress listenAddress = cm.getListenAddress();
            try {
               for (ConnectionProcessor processor : processors) {
                  processor.setUpdatingServerSocket(true);
               }
               IOUtils.close(serverSocket);
               if (port != -1) {
                  ServiceListener.this.port = port;
               }
               try {
                  Thread.sleep(500);
               } catch (InterruptedException e) {
                  if (Mail.getInstance().isShuttingDown()) {
                     return;
                  }
               }
               serverSocket = setupServerSocket(listenAddress);
            } catch (IOException e) {
               String address = "localhost";
               if (listenAddress != null) {
                  address = listenAddress.getHostAddress();
               }

               log.error("Could not create ServiceListener on address: " + address + " port: " + port
                     + ". Please select another address and/or port.");
            } finally {
               for (ConnectionProcessor processor : processors) {
                  processor.setSocket(serverSocket);
                  processor.setUpdatingServerSocket(false);
               }
            }
         }
      };
      
      new Thread(runnable).start();
   }
    
    public void start() {
       if (processors == null)
          return;
       for (ConnectionProcessor processor : processors) {
          processor.setDelayedStart(false);
       }
    }
}


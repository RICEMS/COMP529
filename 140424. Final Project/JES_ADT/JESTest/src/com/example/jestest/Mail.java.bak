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

package com.ericdaugherty.mail.server;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationManagerBackEndDb;
import com.ericdaugherty.mail.server.configuration.ConfigurationManagerCBC;
import com.ericdaugherty.mail.server.configuration.MailServicesControl;
import com.ericdaugherty.mail.server.configuration.ModuleControl;
import com.ericdaugherty.mail.server.errors.UnsupportedVersionException;
import com.ericdaugherty.mail.server.logging.jdk14.LoggerAuth;
import com.ericdaugherty.mail.server.security.JESSecurityManager;
import com.ericdaugherty.mail.server.security.PolicyHandler;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.services.smtp.client.SMTPSender;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.Resolver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Policy;
import java.util.Calendar;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.LogManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is the entrypoint for the Mail Server application.
 * It delegates services startup and monitoring to other classes.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public class Mail {

    /** Logger for this class. */
    private static final Log log = LogFactory.getLog( Mail.class );
    
    static {
       String logProperty = System.getProperty("org.apache.commons.logging.Log");
       if (logProperty != null && logProperty.equals("org.apache.commons.logging.impl.Jdk14Logger")) {
          try {
             LogManager.getLogManager().addLogger(new LoggerAuth());
          }
          catch (IOException ioe) {
             System.err.println("Unable to initialize the Jdk14Logger. " + ioe.getMessage());
          }
       }
    }
    
    private static final Object lock = new Object();

    //Singleton for the Mail instance
    private static volatile Mail instance;
    
    private Resolver dnsResolver;

    private Status status;
    private ShutdownService shutdownService;
    
    /** The ShutdownService Thread.  Started when the JVM is shutdown. */
    private Thread shutdownServiceThread;

    /** A parameter to inform threads that the server has started its operation. */
    private volatile boolean started;

    /** A parameter to inform threads that the server is shutting down. */
    private volatile boolean shuttingDown;
    
    /** A parameter to inform threads that the server is to restart. */
    private volatile boolean restart;
    
    /** A parameter to inform threads that the server is currently restarting. */
    private volatile boolean restarting;
    
    /** A parameter to indicate whether a non-standard testing mode is enabled. */
    private boolean testing;
    
    private ConfigurationManagerBackEndDb.RetainServer dbServer;
    
    private ConfigurationManagerCBC.RetainConfigurator cbConfigurator;
    
    private final Object restartLock = new Object();
    
    private Mail(){}

   private void init(String[] args) {
      
      if (Resolver.getInstance() == null)
         Resolver.instantiate();
    
      dnsResolver = Resolver.getInstance();

        // Perform the basic application startup.  If anything goes wrong here,
        // we need to abort the process.

      try {
         if (args.length > 1)
            testing = args[1].toLowerCase().equals("testing")
                  || (args.length > 2 ? args[2].toLowerCase().equals("testing") : false);

         String os = System.getProperty("os.name").toLowerCase();
         boolean isWin = os.indexOf("win")!=-1;

         // Get the 'root' directory for the mail server.
         String directory = getConfigurationDirectory( args );

         log.warn("JES Starting Up...");
         
         Class clazz = Mail.class;
         String className = clazz.getSimpleName() + ".class";
         String classPath = clazz.getResource(className).toString();
         System.setProperty("jes.version", "");
         if (classPath.toLowerCase().startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            InputStream is = null;
            try {
               is = new URL(manifestPath).openStream();
               Manifest manifest = new Manifest(is);
               Attributes attr = manifest.getMainAttributes();
               Attributes.Name name = new Attributes.Name("Specification-Version");
               if (attr.containsKey(name)) {
                  System.setProperty("jes.version", attr.getValue(name));
               }
            }
            catch (IOException e) {
               log.warn("Could not read version info from jar.");
            }
            finally {
               IOUtils.close(is);
            }
         }

         // Initialize the Configuration Manager.
         ConfigurationManager configurationManager = ConfigurationManager.initialize( directory );
         
         MailServicesControl.instantiate(testing);
         MailServicesControl msc = MailServicesControl.getInstance();

         if(!msc.isListenerServiceInitVerified()) {
            log.error("At least one ServiceListener failed to initialize propertly. Aborting...");
            shutdown();
            instance = null;
            return;
         }
         
         //Run the unix user downgrade code here
         if (!isWin) {

            int uid = -1;
            try {
               try {
                  uid = Integer.parseInt(args[1]);
               }
               catch (NumberFormatException nfe) {
                  try {
                     uid = Integer.parseInt(args[2]);
                  }
                  catch (NumberFormatException nfe1) {
                     log.error("uid not passed through main method arguments");
                  }
               }
            }
            catch (IndexOutOfBoundsException ioobe) {
               //Just ignore
            }
            
            //The uid can change only if euid==0
            if (uid>0) {
               int result;
               result = com.xlat4cast.mail.server.UnixUID.setUID(uid);
               if (result!=0) {
                  log.error("Unable to set the (real) uid to :"+uid);
                  throw new RuntimeException("Unable to set the (real) uid to :"+uid+". Aborting.");
                  
               }
               com.xlat4cast.mail.server.UnixUID.setEUID(uid);
               if (result!=0) {
                  log.error("Unable to set the euid to :"+uid);
                  throw new RuntimeException("Unable to set the euid to :"+uid+". Aborting.");
               }
               log.info("Successfully downgraded user.");
            }
         }

         //Moved here from configuration manager, otherwise (all) file operations could be root based
         ModuleControl.initialize(directory);

         if (log.isDebugEnabled()) {
            log.debug("Persisting user/realm/password updates");
         }
         configurationManager.persistUpdates();
         started = true;
         
         msc.releaseListeners();
         
         if (configurationManager.isSecurityManagerEnabled()) {
            
            PolicyHandler.getInstance().updatePolicyEntries();
            System.setSecurityManager(new JESSecurityManager());
         }

         status = new Status(isWin);
         status.start();

         //Initialize ShutdownService
         shutdownService = new ShutdownService();
         shutdownServiceThread = new Thread(shutdownService);
         Runtime.getRuntime().addShutdownHook( shutdownServiceThread );
         
         log.warn("JES started successfully.");
         
         //Just ignore, meant to test the restart process
         //new Thread(new Runnable(){public void run(){try{Thread.sleep(10*1000);} catch (InterruptedException ex) {}restart = true;instance.shutdown();}}).start();
         
         //Let's just keep the initiating thread around
         do {
            synchronized(lock) {

               while(isStarted()) {
                  try {
                     lock.wait();
                  } catch (InterruptedException ex) {
                     //Ignore
                  }
               } 
            }
            if (isRestart()) {

               log.warn("Server restarting");
               restart = false;
               restarting = true;
               // Initialize the Configuration Manager.
               configurationManager = ConfigurationManager.initialize( directory );

               MailServicesControl.instantiate(testing);
               msc = MailServicesControl.getInstance();
               if(!msc.isListenerServiceInitVerified()) {
                  log.error("At least one ServiceListener failed to initialize properly. Aborting...");
                  shutdown();
                  break;
               }
               
               if (log.isDebugEnabled()) {
                  log.debug("Persisting user/realm/password updates");
               }
               configurationManager.persistUpdates();
               started = true;

               msc.releaseListeners();

               //TODO How to handle resetting the security policy
               PolicyHandler.getInstance().updatePolicyEntries();
               Policy.getPolicy().refresh();
                  
               if (System.getSecurityManager() == null
                     && configurationManager.isSecurityManagerEnabled()) {
                  System.setSecurityManager(new JESSecurityManager());
               }

               status = new Status(isWin);
               status.start();

               log.warn("JES restarted successfully.");
               restarting = false;
               synchronized(restartLock) {
                  restartLock.notifyAll();
               }

               dbServer = null;
               cbConfigurator = null;
            }
            else {
               break;
            }
         }while(true);
         instance = null;
      } catch (RuntimeException re) {
         Resolver.shutdown();
         throw re;
      }
    }


    public static synchronized void instantiate(String[] args) {
       
       if (instance == null) {
          
          //Java version 6 or later is required
          int javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3));
          if (javaVersion < 6) {
             throw new UnsupportedVersionException("Versions of Java prior to 6 are not supported.");
          }
             
          try {
             instance = new Mail();
             instance.init(args);
          } catch (RuntimeException e) {
            System.err.println( "The application failed to initialize." );
            e.printStackTrace(System.err);
          }
       }
    }
    
    public static Mail getInstance() {
       return instance;
    }

    /**
     * If true a server shut down has been initiated.
     *
     * @return shuttingDown
     */
    public boolean isShuttingDown() {
       return shuttingDown;
    }

    /**
     * If true all operations relating to system startup have completed.
     *
     * @return started
     */
    public static boolean isStarted() {
       return instance != null && instance.started;
    }

    /**
     * If true a non-standard testing mode is enabled. All outgoing messages
     * are delivered locally, except those whose recipient belongs to the example.com
     * domain that are delivered to a directory named "testing" located in the JES
     * install path.
     *
     * @return testing
     */
    public static boolean isTesting() {
       return instance.testing;
    }
    
    public boolean isRestart() {
       return restart;
    }
    
    public boolean isRestarting() {
       return restarting;
    }
    
    public ConfigurationManagerBackEndDb.RetainServer getRetainServer() {
       return dbServer;
    }
    
    public ConfigurationManagerCBC.RetainConfigurator getRetainConfigurator() {
       return cbConfigurator;
    }
    
    public Object getRestartLock() {
       return restartLock;
    }

    public Resolver getDNSResolver() {
        return dnsResolver;
    }

    /**
     * Provides a 'safe' way for the application to shut down.  This
     * method is provided to enable compatability with the NT Service
     * wrapper class.  It defers the call to the shutdown method.
     *
     * @param args
     */
    public static void shutdown( String[] args ) {
       if (log.isDebugEnabled()) {
           log.debug( "NT Service requested application shutdown." );
       }
        instance.shutdown();
    }
    
    /**
     * Provides a 'safe' way for the application to shut down.  It will attempt
     * to stop the running threads.
     */
    public void shutdown() {

       if (shuttingDown) return;
        log.warn( "Shutting down Mail Server." );
        shuttingDown = true;
        
        if (restart) {
           dbServer = ConfigurationManager.getInstance().getRetainServer();
           cbConfigurator = ConfigurationManager.getInstance().getRetainConfigurator();
        }
        MailServicesControl mailServices = MailServicesControl.getInstance();
        if (mailServices != null)
           mailServices.shutdown();
        if (!restart) {
           ModuleControl.shutdown();
        }
        ConfigurationManager.shutdown();
        if (!restart) {
           Resolver.shutdown();
        }
        
        started = false;
        shuttingDown = false;
        if (status != null) {
           status.interrupt();
        }

        log.warn("Server shutdown complete." + (restart ? " Restarting..." : ""));
        synchronized(lock) {
           lock.notifyAll();
        }
    }
    
    public void checkRestart() {
       
       if ((restart = ConfigurationManager.getInstance().isRestart())) {
          shutdown();
       }
    }

    /**
     * The typical means of starting the application.
     * @param args
     */
    public static void main(String[] args) {
       instantiate(args);
    }

    /**
     * Parses the input parameter for the configuration directory, or defaults
     * to the local directory.
     *
     * @param args the command line arguments.
     * @return the directory to use as the 'root'.
     */
    private static String getConfigurationDirectory( String[] args ) {

        String directory = ".";
        File directoryFile;

        // First, check to see if the location was passed as a parameter.
        if (args.length > 0) {
           directory = args[0];
        }
        // Otherwise, use the default, which is the current directory.
        // The current directory is resolved in a system-dependent manner.
        else if( (directoryFile = new File( directory )).exists() ) {
            System.err.println( "Configuration Directory not specified.  Using \"" + directoryFile.getAbsolutePath() + "\"" );
        }
        // If no file was specified and the default does not exist, printing out a usage line.
        else {
            System.err.println("Usage:  java com.ericdaugherty.mail.server.Mail <configuration directory>");
            throw new RuntimeException( "Unable to load the configuration file." );
        }

        return directory;
    }
    
    /**
     * Report the status of the application.
     * 
     */
   private class Status extends Thread {

      private final boolean refresh;

      public Status(boolean refresh) {
         super( "JES Status Monitor" );
         setDaemon( true );
         this.refresh = refresh;
      }

      @Override
      public void run() {
         
         try {
            Thread.sleep(30 * 1000L);
         } catch (InterruptedException ie) {
            if (!isStarted()) {
               return;
            }
            log.error("The JES Status Monitor thread was interrupted."
                  + " Thread will continue to execute.", ie);
         }

         long sleepTime = 3 * 60 * 1000L;
         long start = System.nanoTime();
         long tenMins = start + (10 * 60 * 1000 * 1000L);
         long hourlyUpdate = start + (1 * 60 * 60 * 1000 * 1000L);
         long current, active, days, hours, mins;
         int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR), currentDay;
         Calendar calendar;
         while(!isShuttingDown()) {

            current = System.nanoTime();
            active = current - start;
            days = active /   (24L * 60L * 60L * 1000L * 1000000L);
            active -= days *  (24L * 60L * 60L * 1000L * 1000000L);
            hours = active /  (60L * 60L * 1000L * 1000000L);
            active -= hours * (60L * 60L * 1000L * 1000000L);
            mins = active /   (60L * 1000L * 1000000L);
            active -= mins *  (60L * 1000L * 1000000L);
            active /= 1000L * 1000000L;
            if (days > 0L || hours > 0L || mins > 0L) {
               StringBuilder timeactive = new StringBuilder( " Time active: ");
               if (days > 0L)  timeactive.append( days ).append(" days, ");
               if (hours > 0L) timeactive.append( hours ).append(" hours, ");
               if (mins > 0L)  timeactive.append( mins ).append(" minutes.");
               log.info(timeactive);
            }
            MailServicesControl.getInstance().logStatus();
            //TODO previously, if no active threads were detected the loop would break
            //TODO This is not currently the case. Is the break necessary though, or even proper?
            if (refresh) {
               calendar = Calendar.getInstance();
               currentDay = calendar.get(Calendar.DAY_OF_YEAR);
               if (currentDay!=dayOfYear && calendar.get(Calendar.HOUR_OF_DAY)>=4) {
                  if (log.isDebugEnabled()) {
                     log.debug("Updating ServerSockets");
                  }
                  dayOfYear = currentDay;
                  MailServicesControl.getInstance().updateServiceListeners();
               }
               calendar.clear();
            }
            if (current >= tenMins) {
               tenMins = current + (5 * 60 * 1000 * 1000L);
               MailServicesControl msc = MailServicesControl.getInstance();
               if (msc != null) {
                  SMTPSender smtpSender = msc.getSmtpSender();
                  if (smtpSender != null)
                     smtpSender.cleanup();
                  SMTPSender amavismtpSender = msc.getAmavisSmtpSender();
                  if (amavismtpSender != null)
                     amavismtpSender.cleanup();
               }
            }
            if (current >= hourlyUpdate) {
               hourlyUpdate = current + (1 * 60 * 60 * 1000 * 1000L);
               DeliveryService service = DeliveryService.getInstance();
               service.cleanup();
            }
            if (!isStarted()) {
               break;
            }
            try {
                Thread.sleep( sleepTime );
            }
            catch( InterruptedException ie ) {
               if (!isStarted()) {
                  break;
               }
               log.error("The JES Status Monitor thread was interrupted."
                     + " Thread will continue to execute.", ie);
            }
         }
      }
   }
}

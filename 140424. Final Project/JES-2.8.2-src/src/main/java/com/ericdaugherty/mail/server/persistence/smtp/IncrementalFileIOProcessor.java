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

package com.ericdaugherty.mail.server.persistence.smtp;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.support.Utils;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.FileUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A file system based SMTP persistence processor.
 *
 * @author Andreas Kyrmegalos
 */
public final class IncrementalFileIOProcessor implements SMTPMessagePersistenceProcessor {

   private static final byte[] EOL = System.getProperty("line.separator").getBytes(ConfigurationManager.getUsAsciiCharset());

   /** Logger */
   private static final Log log = LogFactory.getLog( IncrementalFileIOProcessor.class );

   /** The ConfigurationManager */
   private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   private final SMTPMessage message;
   private File messageLocation;

   IncrementalFileIOProcessor(@NonNull SMTPMessage message) {
      this.message = message;
   }

   @Override
   public void setSMTPMessagePersistenceProcessor(SMTPMessagePersistenceProcessor processor) {
      throw new UnsupportedOperationException("Cannot assign self.");
   }

    @Override
   public Object getPersistedID() {
      return messageLocation.getPath();
   }

   /**
    * @return the messageLocation
    */
   public String getMessageLocation() {
      File messageLocation = this.messageLocation;
      return messageLocation != null ? messageLocation.getPath() : null;
   }

   /**
    * @return the messageLocation
    */
   private File getMessageLocation0() {
      return messageLocation;
   }

   /**
    * @param messageLocation the messageLocation to set
    */
   public void setMessageLocation(File messageLocation) {
      this.messageLocation = messageLocation;
   }

    @Override
   public void initializePersistedMessage(String filename, boolean headersOnly) throws IOException {

        File messageFile = new File( filename );
        DelimitedInputStream reader = new DelimitedInputStream(new FileInputStream(messageFile));
        try {
            String stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            if(log.isTraceEnabled())
               log.trace( "Loading SMTP Message " + messageFile.getName() + " version " + stringLine );
            if( !Utils.FILE_VERSION.equals( stringLine ) ) {
                log.error( "Error loading SMTP Message.  Can not handle file version: " + stringLine );
                throw new IOException( "Invalid file version: " + stringLine );
            }
            // Initialize a new message with the right file location
            setMessageLocation( messageFile );

            // Load each variable
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':') + 2);
            message.setSmtpUid( stringLine );
            String fromAddress = new String(reader.readLine(), usAsciiCharset);
            fromAddress = fromAddress.substring(fromAddress.indexOf(':') + 2);
            if (fromAddress.length() == 0) {
               message.setFromAddress( new EmailAddress() );
            }
            else {
               message.setFromAddress(new EmailAddress(fromAddress));
            }
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            message.setToAddresses( Utils.inflateAddresses( stringLine ) );
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            message.setTimeReceived( new Date( Long.parseLong( stringLine ) ) );
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            message.set8bitMIME( Boolean.parseBoolean( stringLine ) );
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            message.setScheduledDelivery( new Date( Long.parseLong( stringLine ) ) );
            stringLine = new String(reader.readLine(), usAsciiCharset);
            stringLine = stringLine.substring(stringLine.indexOf(':')+2);
            message.setDeliveryAttempts( Integer.parseInt( stringLine ) );

            if (!headersOnly) {

               byte[] inputLine = reader.readLine();
               while( inputLine != null ) {
                   addDataLine( inputLine );
                   inputLine = reader.readLine();
               }
            }
        } catch( InvalidAddressException iae ) {
            throw new IOException("Unable to parse the address from the stored file.", iae);
        } catch( NumberFormatException nfe ) {
            throw new IOException("Unable to parse the data from the stored file into a number.", nfe);
        } finally {
           IOUtils.close(reader);
        }
      
   }

    @Override
    public long getSize() {
       long size = 0;
       List<byte[]> stringLines = null;
       try {
          Iterator<byte[]> iter;
          int count = 8;
          stringLines = loadIncrementally(count);
          while(stringLines.size()>0) {
             iter = stringLines.iterator();
             while(iter.hasNext()) {
                size += iter.next().length;
             }
             stringLines.clear();
             count+=250;
             stringLines = loadIncrementally(count);
          }

       } catch (IOException ex) {log.error(ex.getMessage(),ex);}
       finally {
          if (stringLines != null) {
             stringLines.clear();
          }
       }
       return size;
    }

    @Override
    public void addDataLine( byte[] line ) {
       message.incrementSize(line.length);
       message.getDataLines().add( line );
    }

    /**
     * Saves the message to the Mail Spool Directory. Used when rescheduling.
     * @param useAmavisSMTPDirectory
     * @throws java.io.IOException
     */
    @Override
    public void save(boolean useAmavisSMTPDirectory) throws IOException {

       if (!saveBegin(useAmavisSMTPDirectory)) {
          throw new IOException("Renaming or copying a message file failed.");
       }

       File messageFile = new File(getMessageLocation0().getPath().substring(0,getMessageLocation0().getPath().lastIndexOf('.')+1)+"bak");
       String messageName = messageFile.getPath();
       List<byte[]> stringLines = null;
       try {
          int count = 8;
          stringLines = loadIncrementally(count, messageName);
          if (stringLines.size()>0) {
             saveIncrement(stringLines, true, false);
             stringLines.clear();
             count+=250;
             stringLines = loadIncrementally(count, messageName);
             while(stringLines.size()>0) {
                saveIncrement(stringLines, false, true);
                stringLines.clear();
                count+=250;
                stringLines = loadIncrementally(count, messageName);
             }
          }

       }
       catch (IOException ioe) {
          message.getDataLines().clear();
          deleteMessage();
          File toFilename = new File(getMessageLocation0().getPath().substring(0,getMessageLocation0().getPath().lastIndexOf('.')) + ".ser");
          if (messageFile.renameTo(toFilename)) {
             setMessageLocation(toFilename);
          }
          throw(ioe);
       }
       finally {
          if (stringLines!=null) {
             stringLines.clear();
          }
       }
       if (saveFinish()) {
          if (messageFile.delete() || !messageFile.exists()) {
             return;
          }
       }

       throw new IOException("A file operation has failed.");
    }

   //This is the entry point for persisting the message to a SMTP directory.
   @Override
   public boolean saveBegin(boolean useAmavisSMTPDirectory) {

      //This case applies to a message currently being received through an SMTPProcessor, or a bounce message
      if(getMessageLocation0() == null) {
         
         File smtpDirectory = new File(useAmavisSMTPDirectory
               ? configurationManager.getAmavisSMTPDirectory()
               : SMTPMessagePersistenceFactory.getInstance().getSmtpDirectory());

         char[] smtpUidBuffer = new char[8];
         do {
            SMTPMessagePersistenceFactory.generateSmtpUid(smtpUidBuffer);
            String smtpUid = new String(smtpUidBuffer);
            File messageFile = SMTPMessagePersistenceFactory.getSafeToUseFile(smtpDirectory, smtpUid);
            if (messageFile != null) {
               message.setSmtpUid(smtpUid);
               setMessageLocation(messageFile);
               return true;
            }
         }while(true);
      } else {//This case applies to a message being re-scheduled
         File toFilename = new File(getMessageLocation0().getPath().substring(0,getMessageLocation0().getPath().lastIndexOf('.')) + ".bak");
         try {
            FileUtils.copyFile(getMessageLocation0(), toFilename );
         }
         catch (IOException ioe) {
            log.error("Error copying file " + getMessageLocation0().getPath() + " to " + toFilename.getPath(), ioe);
            return false;
         }
         toFilename = new File(toFilename.getPath().substring(0, toFilename.getPath().lastIndexOf('.')) + ".tmp");
         if (!toFilename.exists()) {
            deleteMessage();
            setMessageLocation(toFilename);
            return true;
         }
         return false;
      }

   }

    //This is called (multiple times) while persisting a message.
    @Override
    public void saveIncrement(List<byte[]> dataLines, boolean writeHeaders, boolean append) throws IOException {
        int length = dataLines.size();
        BufferedOutputStream bos = null;
        try {
           bos = new BufferedOutputStream(new FileOutputStream(getMessageLocation0(), append));
           if (writeHeaders) {
              bos.write( ("X-JES-File-Version: "+Utils.FILE_VERSION).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-UID: "+message.getSmtpUid()).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-MAIL-FROM: "+message.getFromAddress().toString()).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-RCPT-TO: "+Utils.flattenAddresses( message.getToAddresses() )).getBytes(usAsciiCharset));
              bos.write( EOL );
              bos.write( ("X-JES-Date: "+String.valueOf( message.getTimeReceived().getTime() )).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-8bitMIME: "+String.valueOf( message.is8bitMIME() )).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-Delivery-Date: "+String.valueOf( message.getScheduledDelivery().getTime() )).getBytes(usAsciiCharset) );
              bos.write( EOL );
              bos.write( ("X-JES-Delivery-Count: "+String.valueOf( message.getDeliveryAttempts() )).getBytes(usAsciiCharset) );
              bos.write( EOL );
           }
           for( int index = 0; index < length; index++ ) {
               bos.write( dataLines.get( index ) );
               bos.write( EOL );
           }
        } finally {
           IOUtils.close(bos);
        }

    }

    @Override
    public boolean saveFinish() {
       File toFilename = new File(getMessageLocation0().getPath().substring(0, getMessageLocation0().getPath().lastIndexOf('.')) + ".ser");
       if (getMessageLocation0().renameTo(toFilename)) {
          setMessageLocation(toFilename);
          return true;
       }
       deleteMessage();
       return false;
    }

    @Override
    public List<byte[]> loadIncrementally(int start) throws IOException{
       return loadIncrementally(start, getMessageLocation0().getPath());
    }

    @Override
    public final List<byte[]> loadIncrementally(int start, String messageName ) throws IOException {

       DelimitedInputStream reader = new DelimitedInputStream( new FileInputStream( new File(messageName) ) );
       List<byte[]> stringLines = new ArrayList<byte[]>(250);

        try {
           for (int i=0;i<start;i++) {
               reader.readLine();
           }

           byte[] inputLine = reader.readLine();
           while( inputLine != null ) {
               stringLines.add(inputLine);
               if (stringLines.size() == 250) break;
               inputLine = reader.readLine();
           }
           return stringLines;
        }
        catch (IOException ioe) {
           message.getDataLines().clear();
           throw ioe;
        }
        finally {
           IOUtils.close(reader);
        }

    }

    /**
     * Moves the message to the 'failed' Directory.
    * @throws java.io.IOException
     */
    @Override
    public void moveToFailedFolder() throws IOException {
       File failedDir = new File( configurationManager.getFailedDirectory() );

       File messageFile = getMessageLocation0();
       if( !messageFile.renameTo( new File(failedDir, messageFile.getName() ) ) )
       {
          try {
             FileUtils.copyFile(messageFile, new File(failedDir, messageFile.getName() ));
             if (!messageFile.delete() && messageFile.exists()) {
                throw new Exception();
             }
          }
          catch (Exception e) {
              throw new IOException("moveToFailedFolder failed for message " + messageFile.getPath(), e);
          }
       }
    }

    @Override
    public boolean isNotSavedInAmavis() {
       return getMessageLocation0().getPath().toUpperCase().indexOf("AMAVIS") == -1;
    }

    @Override
    public long getPersistedSize() {
       return getMessageLocation0().length();
    }

    @Override
    public boolean deleteMessage() {
       return getMessageLocation0().delete()||!getMessageLocation0().exists();
    }

    // Handling of a special case where there is only one recipient, it belongs to a local domain and the user does
    // not exist. Message is resaved in the amavis.incoming.directory to be delivered immediatelly to the postmaster.
    @Override
    public void redirectToPostmaster() throws IOException {
         File smtpDirectory = new File(configurationManager.getAmavisSMTPDirectory());
         File messageFile = new File (smtpDirectory, getMessageLocation0().getName());
         FileUtils.copyFile(getMessageLocation0(), messageFile);
         deleteMessage();
    }
}

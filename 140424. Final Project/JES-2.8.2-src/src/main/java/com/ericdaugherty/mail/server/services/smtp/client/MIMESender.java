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

package com.ericdaugherty.mail.server.services.smtp.client;

//Java Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.*;
import java.util.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports
import com.ericdaugherty.mail.server.services.smtp.MIMEConstants;
import com.ericdaugherty.mail.server.utils.Base64OutputStream;
import java.nio.charset.Charset;

/**
 * Each line of an outgoing message is parsed using RFC MIME rules.
 *
 * @author Andreas Kyrmegalos
 */
public class MIMESender implements MIMEConstants{

   /** Logger */
   private static final Log log = LogFactory.getLog( MIMESender.class );

   protected final Charset utf8Charset = ConfigurationManager.getUtf8Charset();
   protected final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   protected boolean initialHeaders, readingHeaders, convertNextPart;
   protected List<String> boundaries;
   protected int mime;
   protected Base64OutputStream b64os;
   protected byte[] bufferForb64os;
   protected int emptyStringCount, currentRead, previousRead, currentTotal;
   
   protected SenderStreamHandler smtpSSH;

   protected MIMESender() {}

   protected final void processDATA(byte[] output) throws IOException{

      if (!(initialHeaders||readingHeaders||(output.length!=0&&(output[0]==0x02D&&output[1]==0x02D)))) {
         if (!convertNextPart) {
            smtpSSH.write(output);
            smtpSSH.write(CRLF_BYTES);
            //No need to flush per line, it is extremely inefficient
            //out.flush();
         }
         else {
            if (output.length==0) {
               emptyStringCount++;
            }
            else {
               if (emptyStringCount>0) {
                  for (int i=0;i<emptyStringCount;i++) {
                     b64os.write( CRLF_BYTES );
                  }
                  emptyStringCount = 0;
               }
               /*
               b64os.write(output);
               b64os.write(CRLF_BYTES);
               b64os.flush(false);
                */
               if (previousRead>0) {
                  b64os.write(bufferForb64os);
                  bufferForb64os = null;
                  b64os.write(CRLF_BYTES);
                  currentTotal = Math.min(output.length, 57-previousRead);
                  b64os.write(output,0,currentTotal);
                  if (output.length<=currentTotal) {
                     b64os.write(CRLF_BYTES);
                     currentTotal = previousRead = 0;
                     return;
                  }
                  previousRead = 0;
               }
               do{
                  currentRead = Math.min(57, output.length-currentTotal);
                  if (currentRead==57) {
                     b64os.write(output,currentTotal,currentRead);
                     currentTotal+=currentRead;
                  }
               }while(currentRead==57);
               if (currentRead<57) {
                  bufferForb64os = new byte[currentRead];
                  System.arraycopy(output, currentTotal, bufferForb64os, 0, currentRead);
                  previousRead = currentRead;
               }
               currentTotal = 0;
            }
         }
      }
      else {
         if (previousRead>0) {
            b64os.write(bufferForb64os);
            for (int i=bufferForb64os.length-1;i>=0;i--) {
                bufferForb64os[i] = 0;
             }
            bufferForb64os = null;
            b64os.write(CRLF_BYTES);
            previousRead = 0;
         }
         if (emptyStringCount>0) {
            for (int i=0;i<emptyStringCount-1;i++) {
               b64os.write( CRLF_BYTES );
            }
            emptyStringCount = 0;
            b64os.close();
            smtpSSH.write( CRLF_BYTES );
            //No need to flush per line, it is extremely inefficient
            //out.flush();
         }
         else if (convertNextPart && !readingHeaders) {
            b64os.close();
         }
         convertNextPart = false;
         String input = process(output);
         if (initialHeaders) {
            String capitalized = input.toUpperCase(ConfigurationManager.LOCALE);
            if(capitalized.startsWith(MIMECONTENT_TYPE) && mime == MIME_UNDEFINED) {
               if (capitalized.indexOf(MIMEMULTIPART)!=-1) {
                  mime = MIME_MULTIPART;
                  if (capitalized.indexOf(MIMEMULTIPART)!=-1) {
                     if (input.lastIndexOf("\"")!=-1) {
                        boundaries.add(input.substring(input.indexOf("\"")+1,input.lastIndexOf("\"")));
                     }
                     else {
                        boundaries.add(input.substring(input.lastIndexOf("=")+1));
                     }
                  }
               }
               else {
                  readingHeaders = true;
                  initialHeaders = false;
               }

            }
            else if (mime == MIME_MULTIPART) {
               String possibleBoundary = "";
               try {
                  if (input.lastIndexOf("\"")!=-1) {
                     possibleBoundary = input.substring(input.indexOf("\"")+1,input.lastIndexOf("\""));
                  }
                  else {
                     possibleBoundary = input.substring(input.lastIndexOf("=")+1);
                  }
               }
               catch (IndexOutOfBoundsException iobe) {}
               if (capitalized.indexOf(MIMEMULTIPART)!=-1) {
                  boundaries.add(possibleBoundary);
               }
               if (possibleBoundary.length()>0&&boundaries.contains(possibleBoundary)) {
                  readingHeaders = true;
                  initialHeaders = false;
                  mime = MIME_UNDEFINED;
               }

            }
            write ( output );

         }
         else if (readingHeaders) {
            String capitalized = input.toUpperCase(ConfigurationManager.LOCALE);
            if (capitalized.startsWith(MIMECONTENT_TYPE)) {
               if (capitalized.indexOf(MIMEMULTIPART)!=-1) {
                  mime = MIME_MULTIPART;
               }
               else {
                  mime = MIME_UNDEFINED;
               }
               write ( output );
            }
            else if ( mime != MIME_MULTIPART ) {
               if (capitalized.startsWith(MIMECONTENT_TRANSFER_ENCODING) && capitalized.indexOf(MIME8BIT)!=-1) {
                 convertNextPart = true;
                 b64os = new Base64OutputStream(smtpSSH.getActiveOutputStream());
               }
               else if (input.equals("")) {
                  readingHeaders = false;
               }
               write ( output );
            }
            else if ( input.equals("")) {
               readingHeaders = false;
               write ( output );
            }
         }
         else {
            Iterator<String> iter = boundaries.iterator();
            while (iter.hasNext()) {
               if (input.indexOf(iter.next())!=-1) {
                  if (convertNextPart) {
                     convertNextPart = false;
                  }
                  readingHeaders = true;
                  write ( output );
                  return;
               }
            }
         }
      }
   }

    /**
     * This method converts a sequence of bytes to a String
     *
     * @param input a sequence of bytes from the incoming stream
     * @return a string corresponding to a line of input from the stream encoded
     * in US-ASCII
     */
    private String process(byte[] input) {
      int length = input.length;
      if (length==0) return "";
      return new String(input, 0, length, usAsciiCharset);
    }

    /**
     * Writes the specified output message to the client.
     */
    private void write( byte[] message ) throws IOException {
        if (message!=null) {
            if( log.isDebugEnabled() )
               log.debug( "Writing: " + new String(message) );
            smtpSSH.write( message );
            smtpSSH.write( CRLF_BYTES );
        }
    }


    static final String CRLF_STRING = "\r\n";
    static final byte[] CRLF_BYTES = new byte[]{0x0d,0x0a};

}

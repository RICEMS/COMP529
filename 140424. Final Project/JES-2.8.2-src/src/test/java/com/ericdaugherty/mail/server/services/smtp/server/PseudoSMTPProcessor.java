/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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
 * $Rev: 334 $
 * $Date: 2014-01-04 02:56:07 +0100 (Sat, 04 Jan 2014) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.services.smtp.server;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.services.ProcessorStreamHandler;
import com.ericdaugherty.mail.server.services.smtp.server.support.AddDataLine;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * This class processes the data in the same manner as the SMTPProcessor
 * handles the data stream after receiving the DATA command.
 *
 * @author Andreas Kyrmegalos
 */
public class PseudoSMTPProcessor extends MIMEProcessor {
   
   private static final Log log = LogFactory.getLog(PseudoSMTPProcessor.class);

   private static final byte[] EOL = System.getProperty("line.separator").getBytes(ConfigurationManager.getUtf8Charset());

   private File outputFile;
   private byte[] output;
   private int finalBufferSize = 32;
   private boolean endOfMessage = false;
   private ProcessorStreamHandler smtpSH;

   private final class TestAddDataLine implements AddDataLine {

      private List stringLines = new ArrayList(250);

      @Override
      public void addDataLine(byte[] line) throws IOException {
         stringLines.add(line);
         if (stringLines.size() == 250) {
            saveIncrement(stringLines, true);
            stringLines.clear();
         }
      }

      @Override
      public void flush() throws IOException {
         if (stringLines.size() > 0) {
            saveIncrement(stringLines, true);
         }
         stringLines.clear();
         stringLines = null;
      }
   }

   //This is called (multiple times) while persisting a message.
   private void saveIncrement(List<byte[]> dataLines, boolean append) throws IOException {
      BufferedOutputStream bos = null;
      try {
         bos = new BufferedOutputStream(new FileOutputStream(outputFile, append));
         for (byte[] dataLine:dataLines) {
            bos.write(dataLine);
            bos.write(EOL);
         }
      } catch (FileNotFoundException e) {
         log.error("", e);
      } finally {
         IOUtils.close(bos);
      }

   }

   public void process(String filename, String domain) {

      setDomain(new Domain(domain));

      outputFile = new File(filename + ".converted");

      addDataLine = new PseudoSMTPProcessor.TestAddDataLine();
      mimeBody = new MIMEProcessor.MIMEBody(null);
      
      try {
         smtpSH = new ProcessorStreamHandler(new FileInputStream(new File(filename)), (OutputStream)null);
         do {
            readInputStream();
         }while(!endOfMessage);
         addDataLine.flush();
      } catch (IOException ioe) {
         log.error("", ioe);
      }
      finally {
         if (smtpSH != null) {
            IOUtils.close(smtpSH.getActiveInputStream());
         }
      }
   }

   private int lastByteInLatestBuffer = 0x00;

   private void readInputStream() throws IOException {

      
      byte[] buffer = new byte[finalBufferSize + 5];
      int currentRead = smtpSH.read(buffer, 0, finalBufferSize);
      if (currentRead == -1) {
         return;
      }
      if (currentRead > 0) {
         buffer = handleMalformedCRLF(buffer, currentRead);
         currentRead = ByteUtils.getIntegerFromNetworkByteOrder(buffer, buffer.length-4, 4);
      }
      constructLineOfText(currentRead, buffer);
   }
   
   //Check that there is a 0x0d before each 0x0a. If not, add it.
   //This is necessary since MIMEProcessor expects data to respect
   //the 0x0d,0x0a line termination. *nix OSes store data using 0x0a
   //as a line separator unlike windows which uses 0x0d,0x0a. Macs
   //prefer the 0x0d flavor. JES 2.x has not been tested with any
   //OSX systems and incompatibilities are almost certainly present.
   private byte[] handleMalformedCRLF(byte[] buffer, int currentRead) {
      byte aByte;
      int newCurrentRead = currentRead;
      for (int i=0;i<newCurrentRead;i++) {
         aByte = buffer[i];
         if (aByte==0x0a) {

            if (i>0&&buffer[i-1]==0x0d) continue;
            if (i >= 0 && lastByteInLatestBuffer != 0x0d) {
               newCurrentRead++;
               if (buffer.length-4<newCurrentRead+1) {
                  byte[] newBuffer = new byte[buffer.length+1];
                  System.arraycopy(buffer, 0, newBuffer, 0, i+1);
                  System.arraycopy(buffer, i, newBuffer, i+1, newCurrentRead-i);
                  buffer = newBuffer;
               }
               else {
                  for (int j=newCurrentRead-1;j>=i;j--) {
                     buffer[j+1] = buffer[j];
                  }
               }
               buffer[i] = 0x0d;
               i++;
            }
         }
      }
      ByteUtils.getNetworkByteOrderFromInt(newCurrentRead, buffer, buffer.length-4, 4);
      if (newCurrentRead > 0) {
         lastByteInLatestBuffer = buffer[newCurrentRead-1];
      }
      return buffer;
   }

   private void constructLineOfText(int currentRead, byte[] buffer) throws IOException {

      int nextByte, previousRead = 0;
      for (int i = 0; i < currentRead; i++) {
         if (buffer[i] == 0x0d || buffer[i] == 0x0a) {
            if (i + 1 == currentRead) {
               nextByte = smtpSH.read();
               lastByteInLatestBuffer = nextByte;
               if (nextByte != -1) {
                  buffer[i + 1] = (byte) nextByte;
                  constructLineOfText3(buffer, true, previousRead, i);
               } //Perhaps a truncated end of DATA transmission. Check if it so.
               else if (output == null && i - previousRead == 1 && buffer[previousRead] == 0x2E) {
                  endOfMessage = true;
               }
               break;
            } else {
               i = constructLineOfText3(buffer, false, previousRead, i);
               if (endOfMessage) {
                  return;
               }
               previousRead = i + 1;
            }
         }
         if (i == currentRead - 1) {
            output = constructLineOfText2(output, buffer, previousRead, currentRead);
            finalBufferSize *= 2;
         }
      }
   }

   private byte[] constructLineOfText2(byte[] output, byte[] buffer, int startSegmentCount, int currentSegmentCount) {

      if (output != null) {
         int tempOutputLength;
         byte[] tempOutput;
         tempOutputLength = output.length;
         tempOutput = new byte[tempOutputLength];
         System.arraycopy(output, 0, tempOutput, 0, tempOutputLength);
         output = new byte[tempOutputLength + currentSegmentCount - startSegmentCount];
         System.arraycopy(tempOutput, 0, output, 0, tempOutputLength);
         System.arraycopy(buffer, startSegmentCount, output, tempOutputLength, currentSegmentCount - startSegmentCount);
      } else {
         output = new byte[currentSegmentCount - startSegmentCount];
         System.arraycopy(buffer, startSegmentCount, output, 0, currentSegmentCount - startSegmentCount);
      }
      return output;
   }

   private int constructLineOfText3(byte[] buffer, boolean increment, int previousRead, int i) throws IOException {

      if (buffer[i + 1] == 0x0a) {
         i++;
         output = constructLineOfText2(output, buffer, previousRead, i - 1);
         int outputLength = output.length;
         if (outputLength > 16 && outputLength <= 128) {
            finalBufferSize = outputLength + 2;
         }
         if (endOfMessage = checkEndOfDATA(output)) {
            return -1;
         }
         processDATA(output);
         if (buffer[i - 1] == 0x0a) {
            processDATA(new byte[0]);
         }
         output = null;
      } else {
         if (increment) {
            i++;
         }
         if (buffer[i] == 0x0a && output != null && output[output.length - 1] == 0x0d) {
            output = constructLineOfText2(output, output, 0, output.length);
            processDATA(output);
            output = null;
         } else {
            output = constructLineOfText2(output, buffer, previousRead, i + 1);
         }
      }
      return i;
   }
}

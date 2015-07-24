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

package com.ericdaugherty.mail.server.utils;

//Java Imports
import java.io.*;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
/**
 * This class adds to another InputStream the ability to read a line of bytes
 * off the underlying stream source. The assumption is that the source bytes
 * correspond to a sequence of characters. It is desirable in certain situations
 * to be able to read a delimited series of bytes rather than have them converted
 * to a stream of characters first, since it might be possible that the character
 * set is unknown at a stage of the process or that the overhead needed to use
 * the proper one is unacceptable or that it simply is not necessary to have the
 * bytes converted to characters. One such case is when JES reads a message from
 * an I/O source where the EOL is platform-dependent.
 *
 * @author Andreas Kyrmegalos
 */
public class DelimitedInputStream extends PushbackInputStream{
   
   private static final String DEFAULT_ENCODING = "UTF-8";
   
   private final int encBytes;
   
   private final boolean little;

   /** A byte array corresponding to an EOL */
   private final byte[] delimiter;

   /** A flag to indicate the size of the EOL */
   private final boolean twoByteDelimiter;

   /** The size of the pushback buffer */
   private final int maxBufferSize;
   
   private final LineReader lineReader;
   
   private final String encoding;
   
   public String getEncoding() {
      return encoding;
   }

   /** A constructor using a fixed buffer size
    * @param in
    */
   public DelimitedInputStream(InputStream in) {
      this(in,16);
   }

   /** A constructor using an application defined delimiter and a fixed buffer size.
    * @param in
    * @param delimiter
    */
   public  DelimitedInputStream(InputStream in, byte[] delimiter) {
      this(in,16,delimiter);
   }

   /** A constructor using an application defined buffer size
    * @param in
    * @param maxBufferSize
    */
   public  DelimitedInputStream(InputStream in, int maxBufferSize) {
      this(in, maxBufferSize, System.getProperty("line.separator").getBytes(ConfigurationManager.getUtf8Charset()));
   }

   /** A constructor using an application defined buffer size and delimiter
    * @param in
    * @param maxBufferSize
    * @param delimiter
    */
   public  DelimitedInputStream(InputStream in, int maxBufferSize, byte[] delimiter) {
      super(in,maxBufferSize);
      if (delimiter.length > 2) {
         throw new IllegalArgumentException("a delimiter can consist of at most 2 bytes.");
      }
      this.maxBufferSize = maxBufferSize;
      this.delimiter = delimiter.clone();
      this.twoByteDelimiter = delimiter.length > 1;
      lineReader = new LineReader();
      this.encoding = DEFAULT_ENCODING;
      this.encBytes = 1;
      this.little = true;
   }

   /** A constructor using an application defined buffer size and auto delimiter
    * @param in
    * @param maxBufferSize
    * @param auto
    * @throws java.io.IOException
    */
   public  DelimitedInputStream(InputStream in, int maxBufferSize, boolean auto) throws IOException{
      super(in,maxBufferSize);
      this.maxBufferSize = maxBufferSize;
      this.delimiter = null;
      this.twoByteDelimiter = false;
      if (auto) {
         lineReader = new AutoLineReader();
      }
      else {
         lineReader = new LineReader();
      }
      String encoding = DEFAULT_ENCODING;
      int encBytes = 1;
      boolean little = true;
      
      //FF FE little endian unicode
      //FE FF big endian unicode
      //EF BB BF UTF-8
      if (available()>1) {
         int aByte = read();
         if (aByte==0xFF) {
            aByte = read();
            if (aByte!=0xFE) {
               unread(aByte);
               unread(0xFF);
            }
            else {
               encoding = "UnicodeLittle";
               encBytes = 2;
            }
         }
         else if (aByte==0xFE) {
            aByte = read();
            if (aByte!=0xFF) {
               unread(aByte);
               unread(0xFE);
            }
            else {
               encoding = "UTF-16";
               encBytes = 2;
               little = false;
            }

         }
         else if (aByte==0xEF) {
            aByte = read();
            if (aByte!=0xBB||available()==0) {
               unread(aByte);
               unread(0xEF);
            }
            else {
               aByte = read();
               if (aByte!=0xBF) {
                  unread(aByte);
                  unread(0xBB);
                  unread(0xEF);
               }
               else {
                  encoding = "UTF-8";
               }
            }
         }
         else {
            unread(aByte);
         }
      }
         
      this.encoding = encoding;
      this.encBytes = encBytes;
      this.little = little;
   }
   
   public byte[] readLine() throws IOException {
      return lineReader.readLine();
   }
   
   private class AutoLineReader extends LineReader {
      
      @Override
      public byte[] readLine() throws IOException {
         
         byte[] buffer = new byte[maxBufferSize];
         int currentRead = read(buffer,0,maxBufferSize);
         if (currentRead == -1)
            return null;
         for (int i = 0;i < currentRead;i++) {
            int current = i;
            if (buffer[i] == LF) {
               if (i - encBytes >= 0) {
                  if (buffer[i - encBytes] == CR) {
                     current = i - encBytes - (little ? 0 : 1);
                  }
                  byte[] returnBuffer = new byte[current];
                  System.arraycopy(buffer, 0, returnBuffer, 0, current);
                  unread(buffer,i+encBytes-(little?0:1),currentRead-i-encBytes+(little?0:1));
                  return returnBuffer;
               }
               else {
                  return new byte[0];
               }
            }
            else if (buffer[i]==CR) {
               if (i+1==currentRead) {
                  if (available()>encBytes-1) {
                     int nextByte = read();
                     int previousByte = nextByte;
                     if (encBytes==2&&nextByte==0) {
                        nextByte = read();
                     }
                     if ((byte)nextByte==LF) {
                        byte[] returnBuffer = new byte[0];
                        if (i>0) {
                           current = i-(little?0:1);
                           returnBuffer = new byte[current];
                           System.arraycopy(buffer, 0, returnBuffer, 0, current);
                        }
                        //i+=encBytes+(little?0:1);//It does not appear as if it
                        //needs to be set to any value.
                        return returnBuffer;
                     }
                     else if (nextByte!=-1){
                        unread((byte)nextByte);
                        if (encBytes==2) {
                           unread((byte)previousByte);
                        }
                     }
                  }
                  else {
                     current = i-(little?0:1);
                     byte[] returnBuffer = new byte[current];
                     System.arraycopy(buffer, 0, returnBuffer, 0, current);
                     return returnBuffer;
                  }
               } 
            }
            if (i == currentRead-1) {
               if (currentRead==buffer.length) {
                  byte[] moreBuffer = readLine();
                  if (moreBuffer == null) {
                     return buffer;
                  }
                  else {
                     byte[] returnBuffer = new byte[moreBuffer.length+currentRead];
                     System.arraycopy(buffer, 0, returnBuffer, 0, currentRead);
                     System.arraycopy(moreBuffer,0,returnBuffer,currentRead,moreBuffer.length);
                     return returnBuffer;
                  }
               }
               else {
                  byte[] returnBuffer = new byte[currentRead];
                     System.arraycopy(buffer, 0, returnBuffer, 0, currentRead);
                     return returnBuffer;
               }
            }
         }

         //Should never get here, but the compiler complains otherwise
         return null;
      }
   }
      
   private static final byte LF = 0x0A;
   private static final byte CR = 0x0D;   

   private class LineReader {
      /**
       * The method to extract a single line of bytes
       *
       * @return byte[] a line of bytes without a trailing EOL
       *
       */
      public byte[] readLine() throws IOException{

         byte[] buffer = new byte[maxBufferSize];
         int currentRead = read(buffer,0,maxBufferSize);
         if (currentRead == -1)
            return null;
         for (int i = 0;i < currentRead;i++) {
            if (buffer[i] == delimiter[0]) {
               if (twoByteDelimiter) {
                  if (i + 1 == currentRead) {
                     if (available() > 0) {
                        int nextByte = read();
                        if ((byte)nextByte == delimiter[1]) {
                           byte[] returnBuffer = new byte[i];
                           System.arraycopy(buffer, 0, returnBuffer, 0, i);
                           i++;
                           return returnBuffer;
                        }
                        else if (nextByte != -1){
                           unread((byte)nextByte);
                        }
                     }
                     else {
                        byte[] returnBuffer = new byte[i];
                        System.arraycopy(buffer, 0, returnBuffer, 0, i);
                        return returnBuffer;
                     }
                  }
                  else if (buffer[i+1]==delimiter[1]) {
                     byte[] returnBuffer = new byte[i];
                     System.arraycopy(buffer, 0, returnBuffer, 0, i);
                     i++;
                     unread(buffer,i+1,currentRead-i-1);
                     return returnBuffer;
                  }
               }
               else {
                  byte[] returnBuffer = new byte[i];
                  System.arraycopy(buffer, 0, returnBuffer, 0, i);
                  unread(buffer,i+1,currentRead-i-1);
                  return returnBuffer;
               }

            }
            if (i==currentRead-1) {
               if (currentRead==buffer.length) {
                  byte[] moreBuffer = readLine();
                  if (moreBuffer==null) {
                     return buffer;
                  }
                  else {
                     byte[] returnBuffer = new byte[moreBuffer.length+currentRead];
                     System.arraycopy(buffer, 0, returnBuffer, 0, currentRead);
                     System.arraycopy(moreBuffer,0,returnBuffer,currentRead,moreBuffer.length);
                     return returnBuffer;
                  }
               }
               else {
                  byte[] returnBuffer = new byte[currentRead];
                     System.arraycopy(buffer, 0, returnBuffer, 0, currentRead);
                     return returnBuffer;
               }
            }
         }

         //Should never get here, but the compiler complains otherwise
         return null;
      }
   }
}

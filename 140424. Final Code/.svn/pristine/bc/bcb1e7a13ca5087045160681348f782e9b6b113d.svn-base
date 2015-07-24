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

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

/**
 * Each smtp/pop3 session hands control of its streams and socket to an instance
 * of this class.
 *
 * @author Andreas Kyrmegalos
 */
public class ProcessorStreamHandler {

   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   protected OutputStream outputStream;
   private OutputStream secureOutputStream;

   protected BufferedInputStream inputStream;
   private BufferedInputStream secureInputStream;
   protected BufferedReader inputReader;

   protected OutputStream activeOutputStream;
   protected InputStream activeInputStream;

   private ProcessorStreamHandler.SimpleStreamHandler streamHandler;
   private DataInputStream saslInputStream;
   private DataOutputStream saslOutputStream;
   
   public ProcessorStreamHandler(InputStream is, OutputStream os) throws IOException {
      streamHandler = new ProcessorStreamHandler.SimpleStreamHandler();
      if (os != null) {
         outputStream = os;
         activeOutputStream = new BufferedOutputStream(outputStream, 4096);
      }
      if (is != null) {
         inputStream = new BufferedInputStream(is);
         inputReader = new BufferedReader(new InputStreamReader(inputStream, ConfigurationManager.getUsAsciiCharset()));
         activeInputStream = inputStream;
      }
   }

   public ProcessorStreamHandler(Socket socket) throws IOException {
      streamHandler = new ProcessorStreamHandler.SimpleStreamHandler();
      outputStream = socket.getOutputStream();
      activeOutputStream = new BufferedOutputStream(outputStream, 4096);
      inputStream = new BufferedInputStream(socket.getInputStream());
      inputReader = new BufferedReader(new InputStreamReader(inputStream, ConfigurationManager.getUsAsciiCharset()));
      activeInputStream = inputStream;
   }

   //Only called when the stream is secured after a successful STARTSTLS command
   public void setSecureStreams(Socket socket) throws IOException {
      if (secureOutputStream == null) {
         activeOutputStream.flush();
         secureOutputStream = socket.getOutputStream();
         activeOutputStream = new BufferedOutputStream(secureOutputStream,4096);
      }
      if (secureInputStream == null) {
         secureInputStream = new BufferedInputStream(socket.getInputStream());
         inputReader = new BufferedReader(new InputStreamReader(secureInputStream, ConfigurationManager.getUsAsciiCharset()));
         activeInputStream = secureInputStream;
      }
   }

   public OutputStream getActiveOutputStream() {
      return activeOutputStream;
   }

   public InputStream getActiveInputStream() {
      return activeInputStream;
   }

   public void setSaslServer(SaslServer saslServer) throws IOException {
      
      activeOutputStream.flush();
      streamHandler = new ProcessorStreamHandler.SaslStreamHandler(saslServer);
      saslInputStream = new DataInputStream(secureInputStream!=null?secureInputStream:inputStream);
      saslOutputStream = new DataOutputStream(activeOutputStream);
   }
   
   public int available() throws IOException {
      return streamHandler.available();
   }
   
   public int read() throws IOException {
      return streamHandler.read();
   }

   public int read(byte[] output, int offset, int length) throws IOException{
      return streamHandler.read(output, offset, length);
   }

   public String readLine() throws IOException {
      return streamHandler.readLine();
   }

   public void print(String line) throws IOException{
      streamHandler.print(line);
   }

   public void write(byte[] line) throws IOException{
      streamHandler.write(line);
   }

   private class SimpleStreamHandler {
      
      public int available() throws IOException {
         return activeInputStream.available();
      }

      public int read() throws IOException {
         return activeInputStream.read();
      }

      public int read(byte[] output, int offset, int length) throws IOException {
         return activeInputStream.read(output, offset, length);
      }

      public String readLine() throws IOException {
         return inputReader.readLine();
      }

      public void print(String line) throws IOException{
         write(line.getBytes(usAsciiCharset));
      }

      public void write(byte[] line) throws IOException {
         activeOutputStream.write(line);
         activeOutputStream.write( CRLF_BYTES );
         activeOutputStream.flush();
      }

   }

   private class SaslStreamHandler extends ProcessorStreamHandler.SimpleStreamHandler {

      private byte[] buffer = {};
      private int bufOffset, bufLength;
      private final SaslServer saslServer;
      private final int sndMaxBuffer;
//      private final int rcvMaxBuffer;
      
      public SaslStreamHandler(SaslServer saslServer) {
         this.saslServer = saslServer;
//         int rcvMaxBufferTemp;
//         try {
//            rcvMaxBufferTemp = Integer.parseInt((String)saslServer.getNegotiatedProperty(Sasl.MAX_BUFFER));
//         } catch (NumberFormatException e) {
//            rcvMaxBufferTemp = 65536;
//         }
//         this.rcvMaxBuffer = rcvMaxBufferTemp;
         int sndMaxBufferTemp;
         try {
            sndMaxBufferTemp = Integer.parseInt((String)saslServer.getNegotiatedProperty(Sasl.RAW_SEND_SIZE));
         } catch (NumberFormatException e) {
            sndMaxBufferTemp = 65536;
         }
         this.sndMaxBuffer = sndMaxBufferTemp;
      }

      @Override
      public int read() throws IOException {
         if (bufLength > 0) {
            try {
               return buffer[bufOffset++]&0xFF;
            }
            finally {
               bufLength--;
               if (bufLength==0) {
                  buffer = new byte[0];
                  bufOffset = 0;
               }
            }
         } else {
            byte[] received = new byte[saslInputStream.readInt()];
            saslInputStream.readFully(received);
            received = saslServer.unwrap(received, 0, received.length);
            buffer = new byte[received.length-1];
            System.arraycopy(received, 1, buffer, 0, received.length-1);
            bufOffset = 0;
            bufLength = received.length-1;
            return received[0]&0xFF;
         }
      }

      @Override
      public int read(byte[] output, int offset, int length) throws IOException {
         if (bufLength>0) {
            if (bufLength>length) {
               System.arraycopy(buffer, bufOffset, output, offset, length);
               bufOffset += length;
               bufLength -= length;
               return length;
            } else {
               System.arraycopy(buffer, bufOffset, output, offset, bufLength);
               buffer = new byte[0];
               bufOffset = 0;
               try {
                  return bufLength;
               }
               finally {
                  bufLength = 0;
               }
            }
         } else {
            byte[] received = new byte[saslInputStream.readInt()];
            saslInputStream.readFully(received);
            received = saslServer.unwrap(received, 0, received.length);
            if (received.length > length) {
               System.arraycopy(received, 0, output, offset, length);
               buffer = new byte[received.length-length];
               System.arraycopy(received, length, buffer, 0, received.length-length);
               bufOffset = 0;
               bufLength = received.length-length;
               return length;
            } else {
               System.arraycopy(received, 0, output, offset, received.length);
               buffer = new byte[0];
               bufOffset = 0;
               bufLength = 0;
               return received.length;
            }
         }
      }

      @Override
      public String readLine() throws IOException {
         byte[] received = new byte[saslInputStream.readInt()];
         saslInputStream.readFully(received);
         return new String(saslServer.unwrap(received, 0, received.length-2), ConfigurationManager.getUsAsciiCharset());
      }

      @Override
      public void print(String line) throws IOException {
         write(line.getBytes(usAsciiCharset));
      }

      @Override
      public void write(byte[] line) throws IOException {
         if (line.length + 2 <= sndMaxBuffer) {
            byte[] send = new byte[line.length + 2];
            System.arraycopy(line, 0, send, 0, line.length);
            System.arraycopy(CRLF_BYTES, 0, send, line.length, 2);
            send = saslServer.wrap(send, 0, send.length);
            saslOutputStream.writeInt(send.length);
            saslOutputStream.write(send);
            saslOutputStream.flush();
         }
         else {
             int count;
             byte[] send;
             for (int i = 0; i < line.length; i += sndMaxBuffer) {

               count = (line.length - i) < sndMaxBuffer ? (line.length+2 - i) : sndMaxBuffer;
               send = new byte[count];
               if (count == sndMaxBuffer) {
                  send = saslServer.wrap(line, i, count);
               } else {
                  System.arraycopy(line, i, send, 0, count-2);
                  System.arraycopy(CRLF_BYTES, 0, send, count-2, 2);
                  send = saslServer.wrap(send, i, count);
               }
               saslOutputStream.writeInt(send.length);
               saslOutputStream.write(send);
             }
             saslOutputStream.flush();
         }
      }

   }

   private static final byte[] CRLF_BYTES = new byte[]{0x0d,0x0a};
}

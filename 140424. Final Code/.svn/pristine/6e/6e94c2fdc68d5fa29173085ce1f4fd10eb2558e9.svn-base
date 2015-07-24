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

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class SenderStreamHandler {
   
   protected OutputStream outputStream;
   private OutputStream secureOutputStream;

   protected BufferedInputStream inputStream;
   private BufferedInputStream secureInputStream;
   protected BufferedReader inputReader;
   
   protected OutputStream activeOutputStream;
   protected PrintWriter printWriter;
   protected InputStream activeInputStream;

   private final SimpleStreamHandler streamHandler;

   public SenderStreamHandler(Socket socket) throws IOException {
      streamHandler = new SimpleStreamHandler();
      outputStream = socket.getOutputStream();
      printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, ConfigurationManager.getUsAsciiCharset())));
      activeOutputStream = outputStream;
      inputStream = new BufferedInputStream(socket.getInputStream());
      inputReader = new BufferedReader(new InputStreamReader( inputStream, ConfigurationManager.getUsAsciiCharset() ));
      activeInputStream = inputStream;
   }

   public void setSecureStreams(Socket socket) throws IOException {
      if (secureOutputStream == null) {
         printWriter.flush();
         secureOutputStream = socket.getOutputStream();
         printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(secureOutputStream, ConfigurationManager.getUsAsciiCharset())));
         activeOutputStream = secureOutputStream;
      }
      if (secureInputStream == null) {
         secureInputStream = new BufferedInputStream(socket.getInputStream());
         inputReader = new BufferedReader(new InputStreamReader( secureInputStream, ConfigurationManager.getUsAsciiCharset() ));
         activeInputStream = secureInputStream;
      }
   }

   public OutputStream getActiveOutputStream() {
      return activeOutputStream;
   }

   public InputStream getActiveInputStream() {
      return activeInputStream;
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
         printWriter.write(line);
         printWriter.write(CRLF_STRING);
         printWriter.flush();
      }

      public void write(byte[] line) throws IOException{
         activeOutputStream.write(line);
      }

   }
   
   private static final String CRLF_STRING = "\r\n";
}

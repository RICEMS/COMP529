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

//Java imports
import java.io.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Base64OutputStream extends FilterOutputStream {

   private Base64 base64;
   
   private boolean closeStream;

   public Base64OutputStream(OutputStream out) {
      this(out,false);
   }

   public Base64OutputStream(OutputStream out, boolean closeStream) {
      super(out);
      base64 = new Base64();
      this.closeStream = closeStream;
   }

   public Base64OutputStream(OutputStream out, byte[] lineBrake, byte padding, int lineLength, boolean closeStream) {
      super(out);
      base64 = new Base64(lineBrake, padding, lineLength);
      this.closeStream = closeStream;
   }

   @Override
   public void write(int b) throws IOException {
      write(new byte[]{(byte)b});
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      
      if (len>0&&len<=b.length-off) {
         base64.encodeMIME(b, off, len);
         flush(false);
      }
   }

   @Override
   public void flush() throws IOException {
      
      flush(true);
   }
   
   private void flush(boolean flush) throws IOException {
      
      byte[] drain = base64.drainEncodedOutputMIME();
      if (drain.length > 0) {
         out.write(drain);
         if (flush) {
            out.flush();
         }
      }
   }

   @Override
   public void close() throws IOException {
      
      out.write(base64.doFinalEncodeMIME());
      out.flush();
      if (closeStream) out.close();
      base64.dispose();
      base64 = null;
   }
}

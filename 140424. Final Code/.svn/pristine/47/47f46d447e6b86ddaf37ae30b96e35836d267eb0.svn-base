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

package com.ericdaugherty.mail.server.logging.jdk14;

//Java imports
import com.ericdaugherty.mail.server.utils.IOUtils;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.logging.LogManager;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class LoggingConfigLoader {
   
   public LoggingConfigLoader() throws IOException, SecurityException {
      
	String jdk14Config = System.getProperty("java.util.logging.config.file");
	if (jdk14Config == null) {
	    jdk14Config = System.getProperty("java.home");
          
	    jdk14Config = new File(new File(jdk14Config, "lib"), "logging.properties").getCanonicalPath();
	}
      final String lineSeparator = System.getProperty("line.separator");
      CharBuffer cb = CharBuffer.allocate(20480);
      String line, key;
      String logsDir = System.getProperty("jes.install.directory")+File.separator+"logs"+File.separator;
      BufferedReader br = new BufferedReader(new FileReader(jdk14Config));
      int pos;
      char symbol;
      try {
         while((line = br.readLine())!=null) {
            if (line.startsWith("#")) {
               cb.put(line);
               cb.put(lineSeparator);
               continue;
            }
            else if (line.trim().isEmpty()) {
               cb.put(lineSeparator);
               continue;
            }
            pos = line.indexOf('=');
            if (pos==-1) {
               pos = line.indexOf(' ');
            }
            symbol = line.charAt(pos);
            key = line.substring(0, pos+1);
            cb.put(key);
            if(key.endsWith("pattern"+symbol)) {
               cb.put(logsDir);
            }
            cb.put(line.substring(pos+1));
            cb.put(lineSeparator);
         }
         cb.flip();
         Charset charset = Charset.defaultCharset();
         ByteBuffer bb = charset.encode(cb);
         byte[] in = new byte[bb.remaining()];
         bb.get(in);
         BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(in));
         try {
            LogManager.getLogManager().readConfiguration(bis);
         }
         finally {
            IOUtils.close(bis);
         }
      }
      finally {
         IOUtils.close(br);
      }
      
      
   }
}

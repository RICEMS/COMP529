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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class JESFormatter extends Formatter {

   private static final String lineSeparator = System.getProperty("line.separator");
   private MessageFormat formatter = new MessageFormat(pattern);
   private static final String pattern = "{0,date,yyyy-MM-dd} {0,time,HH:mm:ss,SSS} -";
   private Date timestamp = new Date();
   private Object[] arguments = new Object[1];

   public synchronized String format(LogRecord record) {

      timestamp.setTime(record.getMillis());
      arguments[0] = timestamp;
      String clazz = record.getSourceClassName();
      String message = record.getMessage();
      Throwable thrown = record.getThrown();

      StringBuffer sb = new StringBuffer(128);
      formatter.format(arguments, sb, null);
      if (clazz != null) {
         sb.append(' ').append(clazz.substring(clazz.lastIndexOf('.')+1)).append(' ').append('-');
      }
      sb.append(' ');
      sb.append(message);
      sb.append(lineSeparator);
      if (thrown != null) {
         StringWriter sw = new StringWriter(1024);
         PrintWriter pw = new PrintWriter(sw);
         try {
            thrown.printStackTrace(pw);
            sb.append(sw.toString());
         } catch (Exception ex) {
            //No worries
         } finally {
            IOUtils.close(pw);
         }
      }
      return sb.toString();
   }
}

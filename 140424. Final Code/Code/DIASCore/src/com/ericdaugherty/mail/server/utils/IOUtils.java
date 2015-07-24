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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class IOUtils {
   
   private static final Log log = LogFactory.getLog(IOUtils.class);
   
   public static void close(@CheckForNull Closeable closeable) {
      if (closeable == null)
         return;
      try {
         closeable.close();
      } catch (IOException ex) {
         if (log.isTraceEnabled())
            log.trace("", ex);
      }
   }
   
   public static void close(@CheckForNull Socket socket) {
      if (socket == null)
         return;
      if (socket.isClosed())
         return;
      try {
         socket.close();
      } catch (IOException ex) {
         if (log.isTraceEnabled())
            log.trace("", ex);
      }
   }
   
   public static void close(@CheckForNull ServerSocket socket) {
      if (socket == null)
         return;
      if (socket.isClosed())
         return;
      try {
         socket.close();
      } catch (IOException ex) {
         if (log.isTraceEnabled())
            log.trace("", ex);
      }
   }
   
   public static void close(@CheckForNull Connection connection) {
      if (connection == null)
         return;
      try {
         if (connection.isClosed())
            return;
         connection.close();
      } catch (SQLException ex) {
         if (log.isTraceEnabled())
            log.trace("", ex);
      }
   }
   
   public static void rollback(@CheckForNull Connection connection) {
      if (connection == null)
         return;
      try {
         if (connection.isClosed())
            return;
         connection.rollback();
      } catch (SQLException ex) {
         if (log.isTraceEnabled())
            log.trace("", ex);
      }
   }
}

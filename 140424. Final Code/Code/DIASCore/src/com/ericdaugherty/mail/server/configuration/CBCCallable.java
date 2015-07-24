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

package com.ericdaugherty.mail.server.configuration;

import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.configuration.cbc.CBCResponseException;
import com.ericdaugherty.mail.server.configuration.cbc.ConnectorCommand;
import com.ericdaugherty.mail.server.utils.IOUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 * @param <V>
 */
public final class CBCCallable<V> implements Callable<V> {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog(ConnectionBasedConfigurator.class);
   
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   
   private final int maxLineCount = 1000;
   private final int maxCommandCount = 20;
   
   /** Socket connection to the client */
   private final Socket socket;

   public CBCCallable(@NonNull Socket socket) {
      this.socket = socket;
   }
   
   public Socket getSocket() {
      return socket;
   }

   @Override
   public V call(){

      BufferedReader reader = null;
      InputStream is = null;
      OutputStream os = null;
      try {
         is = socket.getInputStream();
         os = socket.getOutputStream();
         os.flush();
         reader = new BufferedReader(new InputStreamReader(is, ConfigurationManager.getUtf8Charset()));
         List<String> response = processLines(reader);
         writeNormalOutput(os, response);
      } catch (IOException ioe) {
         log.error("", ioe);
      } catch (CBCResponseException ex) {
         log.error("", ex);
         
         if (os != null) {
            try {
               os.write(FAILURE);
               os.write(ex.getCause().getLocalizedMessage().getBytes(ConfigurationManager.getUtf8Charset()));
               os.write(NORMAL_END);
               os.flush();
            } catch (IOException ioe) {
               log.error("", ioe);
            }
         }
      }
      finally {
         IOUtils.close(reader);
         IOUtils.close(is);
         IOUtils.close(os);
         IOUtils.close(socket);
      }
      return null;
   }

   private List<String> processLines(BufferedReader reader) throws CBCResponseException, IOException {

      try {
         BackEndType backEndType = cm.getBackEndType();
         LinkedList<CBCExecutor> executors = new LinkedList<CBCExecutor>();
         boolean lastReadDot = false;
         int lineCount = 0;
         OUTER_LINE_READER:
         for(String line; (line = reader.readLine()) != null;) {
            ++lineCount;
            line = line.trim();
            lastReadDot = false;
            if (log.isDebugEnabled())
               if (line.toLowerCase().indexOf("password") == -1) {
                  log.debug(line);
               } else {
                  log.debug("entry contains sensitive data.");
               }
            ConnectorCommand command = ConnectorCommand.getByLabel(line);
            if (command == null) {
               if (lineCount == maxLineCount)
                  break;
               continue;
            }
            if (!command.isSupportedBackEnd(backEndType))
               throw new CBCResponseException("The \"" + command.getLabel() + "\""
                     + " command is not supported for the current BackEnd type"
                     + " of \"" + backEndType + "\". Request rejected.");
            List<String> lines = new ArrayList<String>();
            while((line = reader.readLine()) != null) {
               ++lineCount;
               lastReadDot = false;
               line = line.trim();
               if (log.isDebugEnabled())
                  if (line.toLowerCase().indexOf("password") == -1) {
                     log.debug(line);
                  } else {
                     log.debug("entry contains sensitive data.");
                  }
               if (line.equals(".")) {
                  lastReadDot = true;
                  CBCExecutor cbcExecutor = command.getCBCExecutor(lines);
                  executors.add(cbcExecutor);
                  if (!reader.ready()
                        || lineCount == maxLineCount
                        || executors.size() == maxCommandCount)
                     break OUTER_LINE_READER;
                  break;
               }
               lines.add(line);
               if (lineCount == maxLineCount)
                  break OUTER_LINE_READER;
            }
         }
         if (executors.isEmpty())
            throw new CBCResponseException("No requests received.");
         //Be strict about message format
         if (!lastReadDot)
            throw new CBCResponseException("End of command character . (dot) does "
                  + "not form the last line of the incoming message from client "
                  + socket.getInetAddress() + ". "
                  + "Request rejected.");
         //Be strict about message format
         CBCExecutor first = executors.getFirst();
         //Be strict about message format
         //If Config tasks, only accept one
         if (!first.updateBackEnd() && executors.size() > 1)
            throw new CBCResponseException("Configuration related tasks cannot "
                  + "be batch-processed. "
                  + "Request rejected.");
         //Be strict about message format
         //Don't accept BackEnd updates and Config tasks in the same batch
         for (CBCExecutor executor : executors) {
            if (first.updateBackEnd() != executor.updateBackEnd())
               throw new CBCResponseException("Requests are restricted to either "
                     + "batched BackEnd updates or a single Configuration task. "
                     + "Request rejected.");
         }
         //Be strict about message format
         if (!first.updateBackEnd()) {
            log.info("Configuration command successfully received.");
            first.processLines();
            List<String> response = new LinkedList<String>();
            response.add(first.getResponse());
            return response;
         }
         
         log.info("Backend command batch successfully received.");
         return cm.updateBackEndThroughConnection(executors);
      } catch (CBCResponseException e) {
         throw new CBCResponseException(e);
      } catch (PersistException e) {
         throw new CBCResponseException(e);
      }
   }
   
   private void writeNormalOutput(OutputStream os, List<String> response) throws IOException {
      
      Charset utf8Charset = ConfigurationManager.getUtf8Charset();
      for (String singleResponse : response) {
         os.write(singleResponse.getBytes(utf8Charset));
         os.write(LINE_END);
      }
      os.write(NORMAL_END);
      os.flush();
   }
   
   public static String getSuccessResponse() {
      return SUCCESS;
   }
   
   private static final String SUCCESS = "success";
   private static final byte[] FAILURE = "error:".getBytes();
   private static final byte[] LINE_END = new byte[]{0x0d, 0x0a};
   private static final byte[] NORMAL_END = new byte[]{0x2e, 0x0d, 0x0a};
}

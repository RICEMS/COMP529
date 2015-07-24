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
 * $Rev: 334 $
 * $Date: 2014-01-04 02:56:07 +0100 (Sat, 04 Jan 2014) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.dbAccess;

//Java imports
import java.sql.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class ProcessEnvelope {

   public void executeNull(ExecuteProcess ep) throws SQLException {
      SQLException exception = null;
      int count = 4;
      try {
         do {
            try {
               ep.executeProcessReturnNull();
               break;
            } catch (SQLException e) {
               String sqlState = e.getSQLState();
               if ("08S01".equals(sqlState) || "40001".equals(sqlState)) {
                  count--;
                  if (count == 0) {
                     exception = e;
                     break;
                  }
               }
               else {
                  exception = e;
                  break;
               }
            }
         }while(count > 0);
         if (exception != null) {
            exception.printStackTrace();
            if (ep.getConnection() != null)
               ep.getConnection().close();
            throw exception;
         }
      }
      catch (RuntimeException e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         dbFinish(ep);
      }
   }
   
   public Object executeObject(ExecuteProcess ep) throws SQLException {
      SQLException exception = null;
      Object result = null;
      int count = 4;
      try {
         do {
            try {
               result = ep.executeProcessReturnObject();
               break;
            } catch (SQLException e) {
               String sqlState = e.getSQLState();
               if ("08S01".equals(sqlState) || "40001".equals(sqlState)) {
                  count--;
                  if (count == 0) {
                     exception = e;
                     break;
                  }
               }
               else {
                  exception = e;
                  break;
               }
            }
         }while(count > 0);
         if (exception != null) {
            exception.printStackTrace();
            if (ep.getConnection() != null)
               ep.getConnection().close();
            throw exception;
         }
         return result;
      }
      catch (RuntimeException e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         dbFinish(ep);
      }
   }
   
   private void dbFinish(ExecuteProcess ep) {

      if (ep.closeResultSet() && ep.getResultSet() != null) {
         try {
            ep.getResultSet().close();
         } catch (SQLException ex) {
            //just ignore
         }
      }
      if (ep.closePreparedStatement() && ep.getPreparedStatement() != null) {
         try {
            ep.getPreparedStatement().close();
         } catch (SQLException ex) {
            //just ignore
         }
      }
      if (ep.getConnection() != null) {
         try {
            try {
               if (ep.rollback())
                  ep.getConnection().rollback();
            }
            finally {
               if (ep.closeConnection())
                  ep.getConnection().close();
            }
         } catch (SQLException ex) {
            //just ignore
         }
      }
   }

}

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

package com.ericdaugherty.mail.server.configuration.cbc;

import com.ericdaugherty.mail.server.configuration.BackEndType;
import java.util.List;

/**
 *
 * @author Andreas Kyrmegalos
 */
public enum ConnectorCommand {
    
   //File backend only
   COMMAND_ADD_USER("add user") {

      @Override
      public CBCExecutor getCBCExecutor(List<String> lines) {
         return new AddUser(lines);
      }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.FILE;
      }
   },
   
   //Db backend only
   COMMAND_INSERT_DOMAIN("insertDomain") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new InsertDomain(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_DELETE_DOMAIN("deleteDomain") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new DeleteDomain(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_SET_DEFAULT_DOMAIN("setDefaultDomain") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new SetDefaultDomain(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_INSERT_USER("insertUser") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new InsertUser(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_DELETE_USER("deleteUser") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new DeleteUser(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_SET_USER_PASSWORD("setUserPassword") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new SetUserPassword(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_ADD_FORWARD_ADDRESS("addForwardAddress") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new AddForwardAddress(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_REMOVE_FORWARD_ADDRESS("removeForwardAddress") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new RemoveForwardAddress(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_SET_DEFAULT_MAILBOX("setDefaultMailBox") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new SetDefaultMailbox(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_INSERT_REALM("insertRealm") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new InsertRealm(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_DELETE_REALM("deleteRealm") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new DeleteRealm(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_ADD_USER_TO_REALM("addUserToRealm") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new AddUserToRealm(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_REMOVE_USER_FROM_REALM("removeUserFromRealm") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new RemoveUserFromRealm(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_RETRIEVE_DB_PASSWORD("retrieveDbPassword") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new RetrieveDbPassword(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_RETRIEVE_CONFIG("retrieveConfig") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new RetrieveConfiguration(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   },
   COMMAND_APPLY_CONFIG("applyConfig") {

       @Override
       public CBCExecutor getCBCExecutor(List<String> lines) {
           return new ApplyConfiguration(lines);
       }

      @Override
      public boolean isSupportedBackEnd(BackEndType backEndType) {
         return backEndType == BackEndType.RDBM;
      }
   };
   
   private final String label;
   
   private ConnectorCommand(String label) {
       this.label = label;
   }

   public String getLabel() {
       return label;
   }
   
   public static ConnectorCommand getByLabel(String label) {
       
       for(ConnectorCommand command : values()) {
           if (command.getLabel().equals(label))
               return command;
       }
       return null;
   }
   
   public abstract CBCExecutor getCBCExecutor(List<String> lines);
   
   public abstract boolean isSupportedBackEnd(BackEndType backEndType);
}

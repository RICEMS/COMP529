/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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
 * $Rev: $
 * $Date: $
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

import java.io.*;

/**
 * Copies mail.xml into the test/resources folder.
 * 
 * @author Andreas Kyrmegalos
 */
public class TestResourceHandler {
   
   public static void main(String[] args) {
      
      String baseDir = System.getProperty("basedir");
      File mailXML = new File(baseDir, "docs" + File.separator + "conf" +
            File.separator + "mail.xml");
      
      FileInputStream fis;
      try {
         fis = new FileInputStream(mailXML);
      }
      catch (FileNotFoundException fnfe) {
         return;
      }
      
      File targetMailXML = new File(baseDir, "src" + File.separator + "test"
            + File.separator + "resources" + File.separator + "mail.xml");
      if (targetMailXML.exists()) {
         boolean fileDeleted = targetMailXML.delete();
         if (!fileDeleted||targetMailXML.exists()) {
            System.err.println("Unable to delete "+targetMailXML.getName() + ". Aborting...");
            return;
         }
         try {
            if (!targetMailXML.createNewFile()) {
               System.err.println("Unable to create "+targetMailXML.getName() + ". Aborting...");
               return;
            }
         } catch (IOException ex) {
            System.err.println("Unable to create "+targetMailXML.getName() + ". Aborting...");
               return;
         }
      }
      
      FileOutputStream fos;
      try {
         fos = new FileOutputStream(targetMailXML);
      }
      catch (FileNotFoundException fnfe) {
         return;
      }
      
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
      try {
         
         String line;
         while((line = br.readLine()) != null) {
            if (line.indexOf("<!--Db") != -1) {
               StringBuilder sb = new StringBuilder(line.length());
               int start = line.indexOf("!--Db");
               sb.append(line.substring(0, start));
               int stop = line.indexOf("-->");
               sb.append(line.substring(start+3, stop));
               sb.append(">");
               line = sb.toString();
            }
            pw.println(line);
         }
      }
      catch (IOException ioe) {
         //hmmm
      }
      finally {
         try {
            br.close();
         } catch (IOException ex) {
            //Just ignore
         }
         pw.close();
      }
   }
}

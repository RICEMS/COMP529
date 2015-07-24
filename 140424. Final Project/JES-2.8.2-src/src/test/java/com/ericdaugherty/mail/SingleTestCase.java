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

//Java imports
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

//JavaMail imports
import com.sun.mail.pop3.POP3Message;
import com.sun.mail.smtp.SMTPMessage;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.util.SharedFileInputStream;

//JUnit imports
import static org.junit.Assert.*;

//Local imports
import com.ericdaugherty.mail.server.services.smtp.server.PseudoSMTPProcessor;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class SingleTestCase extends AbstractTestCase {
   
   SingleTestCase(String name, AbstractTestInstance testInstance) {
      super(name, testInstance);
   }
   
   private static class RequestFilenameFilter implements FilenameFilter {
      
      private final Map<String, List<String>> userMessages;
      private final Request request;
      
      private RequestFilenameFilter(Map<String, List<String>> userMessages,
            Request request) {
         this.userMessages = userMessages;
         this.request = request;
      }

      @Override
      public boolean accept(File directory, String filename) {
         if (filename.toLowerCase().endsWith(".loc")) {
            userMessages.get(request.getUsername()).add(filename);
            return true;
         }
         return false;
      }
   };

   @Override
   public boolean execute(final List<Request> requests) throws Exception{

      int size = 2 * userInfo.size();
      final Map<String, List<String>> userMessages = new HashMap(size, 1.0f);
      final Map<String, List<String>> allMessages = new HashMap(size, 1.0f);
      final Map<String, List<String>> remainingMessages = new HashMap(size, 1.0f);
      for (String aUser : userInfo.stringPropertyNames()) {
         userMessages.put(aUser, new ArrayList(requests.size() / userInfo.size()));
         allMessages.put(aUser, new ArrayList(requests.size() / userInfo.size()));
         remainingMessages.put(aUser, new ArrayList(requests.size() / userInfo.size()));
      }
      for (Request request : requests) {
         List<String> userTasks = allMessages.get(request.getUsername());
         userTasks.add(request.getMessage().getName());
         userTasks = remainingMessages.get(request.getUsername());
         userTasks.add(request.getMessage().getName());
      }
      
      final Properties properties = System.getProperties();

      final List<byte[]> hashes = new ArrayList(requests.size());
      final List<byte[]> pop3Hashes = new ArrayList(requests.size());

      Thread thread = new Thread(new Runnable() {

         @Override
         @SuppressWarnings("CallToThreadDumpStack")
         public void run() {

            InputStream is = null;
            try {

               while (requests.size() > 0) {
                  final Request request = requests.remove(0);
                  System.err.println("Checking out " + request.getUsername() + "'s message");
                  String messageFilename = request.getMessage().getName();
                  File userDir = new File(tempJESDir, request.getUsername());
                  remainingMessages.get(request.getUsername()).remove(messageFilename);
                  Session session = Session.getInstance(properties, senderCredentials);
                  String randomSequence = getRandomSequence();
                  try {
                     is = new SharedFileInputStream(request.getMessage());
                     SMTPMessage messageSMTP = new SMTPMessage(session, is);

                     messageSMTP.setFrom(new InternetAddress(senderCredentials.getEmailAddress()));

                     messageSMTP.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getUsername() + '@' + server));

                     Transport.send(messageSMTP);

                     File output = new File(userDir, "generatedSMTP." + randomSequence + "." + messageFilename);
                     OutputStream os = null;
                     try {
                        os = new PrintStream(output, System.getProperty("file.encoding"));
                        messageSMTP.writeTo(os);
                        os.write(new byte[]{0x2E, 0x0D, 0x0A});
                        os.flush();
                     
                        new PseudoSMTPProcessor().process(output.getPath(), server);
                     } finally {
                        IOUtils.close(os);
                     }
                  }
                  finally {
                     IOUtils.close(is);
                  }

                  Tools.createMD5File(new File(userDir, "generatedSMTP." + randomSequence + "." + messageFilename + ".converted").getPath());

                  File userDirJES = new File(tempJESDir, "users" + File.separator + server + File.separator + request.getUsername());
                  if (!userDirJES.exists())
                     System.err.println(userDirJES.getPath() + " does not exist.");
                  if  (!userDirJES.isDirectory()) {
                     System.err.println(userDirJES.getPath() + " is not a directory.");
                  }
                  RequestFilenameFilter rff = new RequestFilenameFilter(userMessages, request);
                  File[] results;
                  int count = 0;
                  while ((results = userDirJES.listFiles(rff)).length == 0) {
                     count++;
                     assertTrue("Waited too long to receive the message. Aborting...", count < 7);
                     System.err.println("mail not yet received, sleeping...");
                     try {
                        Thread.sleep(5 * 1000);
                     } catch (InterruptedException ex) {
                     }
                  }

                  byte[] derivedMD5 = Utils.getDerivedMD5(results[0]);
                  byte[] originalMD5 = Utils.getOriginalMD5(new File(userDir, "generatedSMTP." + randomSequence + "." + messageFilename + ".converted.md5"));
                  
                  if (!Arrays.equals(originalMD5, derivedMD5)) {
                     
                     SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss.SSS");
                     BufferedInputStream bis;
                     
                     Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                     Node root, node;
                     root = doc.createElement("failed");
                     doc.appendChild(root);
                     node = doc.createElement("message");
                     Node attrib = doc.createAttribute("user");
                     attrib.setNodeValue(request.getUsername());
                     node.getAttributes().setNamedItem(attrib);
                     attrib = doc.createAttribute("finename");
                     attrib.setNodeValue(request.getMessage().getName());
                     node.getAttributes().setNamedItem(attrib);

                     Node nodeHex = doc.createElement("messageHex");
                     node.appendChild(nodeHex);

                     Node hexValue = doc.createElement("original");
                     nodeHex.appendChild(hexValue);
                     File file = new File(userDir, "generatedSMTP" + messageFilename + ".converted");
                     bis = new BufferedInputStream((new FileInputStream(file)));
                     try {
                        byte[] buffer = new byte[2048];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                        int read;
                        while ((read = bis.read(buffer)) != -1){
                           baos.write(buffer, 0, read);
                        }
                        hexValue.appendChild(doc.createTextNode(new String(ByteUtils.toHex(baos.toByteArray()))));

                        hexValue = doc.createElement("derived");
                        nodeHex.appendChild(hexValue);
                     } finally {
                        IOUtils.close(bis);
                     }
                     bis = new BufferedInputStream((new FileInputStream(results[0])));
                     try {
                        byte[] buffer = new byte[2048];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                        int read;
                        while ((read = bis.read(buffer)) != -1){
                           baos.write(buffer, 0, read);
                        }
                        hexValue.appendChild(doc.createTextNode(new String(ByteUtils.toHex(baos.toByteArray()))));

                     } finally {
                        IOUtils.close(bis);
                     }

                     root.appendChild(node);
                     node = doc.createElement("all");
                     root.appendChild(node);
                     root = node;
                     for (Map.Entry<String, List<String>> entry:allMessages.entrySet()) {
                        node = doc.createElement(entry.getKey());
                        StringBuilder sb = new StringBuilder();
                        for(String subEntry:entry.getValue()){
                           sb.append(subEntry).append(" ");
                        }
                        sb.deleteCharAt(sb.length()-1);
                        node.appendChild(doc.createTextNode(sb.toString()));
                        root.appendChild(node);
                     }
                     root = doc.getDocumentElement();
                     node = doc.createElement("remaining");
                     root.appendChild(node);
                     root = node;
                     for (Map.Entry<String, List<String>> entry:remainingMessages.entrySet()) {
                        node = doc.createElement(entry.getKey());
                        StringBuilder sb = new StringBuilder();
                        for(String subEntry:entry.getValue()){
                           sb.append(subEntry).append(" ");
                        }
                        if (sb.length()>0) {
                           sb.deleteCharAt(sb.length()-1);
                        }
                        node.appendChild(doc.createTextNode(sb.toString()));
                        root.appendChild(node);
                     }
                     ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                     Tools.streamXMLDocument(doc, baos);
                     byte[] failedXML = baos.toByteArray();
                     
                     FileOutputStream fos = null;
                     JarOutputStream jos = null;
                     java.util.jar.JarEntry je;
                     try {
                        fos = new FileOutputStream(new File(System.getProperty("basedir") + File.separator +
                              "target" + File.separator + "surefire-reports",
                              "FAILED.test."+name+"."+sdf.format(new Date())+"."+request.getUsername()+".jar"));
                        jos = new JarOutputStream(fos);

                        je = new java.util.jar.JarEntry("failed.xml");
                        jos.putNextEntry(je);
                        bis = new BufferedInputStream(new ByteArrayInputStream(failedXML));
                        try {
                           byte[] buffer = new byte[2048];
                           int read;
                           while ((read = bis.read(buffer))!= -1){
                              jos.write(buffer, 0, read);
                           }
                        } finally {
                           IOUtils.close(bis);
                           jos.closeEntry();
                        }

                        je = new java.util.jar.JarEntry(results[0].getName());
                        jos.putNextEntry(je);
                        bis = new BufferedInputStream(new FileInputStream(results[0]));
                        try {
                           byte[] buffer = new byte[2048];
                           int read;
                           while ((read = bis.read(buffer))!= -1){
                              jos.write(buffer, 0, read);
                           }
                        } finally {
                           IOUtils.close(bis);
                           jos.closeEntry();
                        }

                        je = new java.util.jar.JarEntry(request.getMessage().getName());
                        jos.putNextEntry(je);
                        bis = new BufferedInputStream(new FileInputStream(request.getMessage()));
                        try {
                           byte[] buffer = new byte[1024];
                           int read;
                           while ((read = bis.read(buffer))!= -1){
                              jos.write(buffer, 0, read);
                           }
                        } finally {
                           IOUtils.close(bis);
                           jos.closeEntry();
                        }

                        je = new java.util.jar.JarEntry(request.getMessage().getName()+".generatedSMTP");
                        jos.putNextEntry(je);
                        bis = new BufferedInputStream(new FileInputStream(new File(userDir, "generatedSMTP" + messageFilename)));
                        try {
                           byte[] buffer = new byte[1024];
                           int read;
                           while ((read = bis.read(buffer))!= -1){
                              jos.write(buffer, 0, read);
                           }
                        } finally {
                           IOUtils.close(bis);
                           jos.closeEntry();
                        }
                     } finally {
                        IOUtils.close(jos);
                        IOUtils.close(fos);
                     }
                  }
                  System.err.println(new String(ByteUtils.toHex(derivedMD5)));
                  System.err.println(new String(ByteUtils.toHex(originalMD5)));               
                  assertArrayEquals("A persisted received message is not an exact copy of the sent message.", originalMD5, derivedMD5);
                  synchronized (hashes) {
                     hashes.add(derivedMD5);
                  }

                  session = Session.getInstance(properties,
                        new PasswordAuthenticator(request.getUsername() + '@' + server, userInfo.getProperty(request.getUsername())));

                  Store store = null;
                  try {
                     store = session.getStore("pop3");

                     store.connect();

                     //Create a Folder object corresponding to the given name.
                     Folder folder = store.getFolder("inbox");

                     // Open the Folder.
                     folder.open(Folder.READ_WRITE);

                     // Get the messages from the server
                     Message[] messages;

                     count = 0;
                     do {
                        messages = folder.getMessages();
                        if (messages.length == 0) {

                           count++;
                           assertTrue("Waited too long on the message. Aborting...", count < 7);
                           System.err.println("Going to sleep again");
                           try {
                              Thread.sleep(5 * 1000);
                           } catch (InterruptedException ex) {
                           }

                        } else {
                           break;
                        }
                     } while (true);

                     POP3Message messagePOP3 = (POP3Message) messages[0];

                     OutputStream os = null;
                     try {
                        os = new PrintStream(new File(userDir, "generatedPOP3" + messageFilename), System.getProperty("file.encoding"));
                        messagePOP3.writeTo(os);
                     }
                     finally {
                        IOUtils.close(os);
                     }

                     messagePOP3.setFlag(Flags.Flag.DELETED, true);

                     folder.close(true);
                  }
                  finally {
                     if (store != null) {
                        try {
                           store.close();
                        } catch (MessagingException ex) {
                        }
                     }
                  }

                  Tools.createMD5File(new File(userDir, "generatedPOP3" + messageFilename).getPath());

                  originalMD5 = Utils.getOriginalMD5(new File(userDir, "generatedPOP3" + messageFilename + ".md5"));
                  System.err.println(Arrays.equals(originalMD5, derivedMD5));
                  System.err.println(new String(ByteUtils.toHex(derivedMD5)));
                  System.err.println(new String(ByteUtils.toHex(originalMD5)));
                  synchronized (pop3Hashes) {
                     pop3Hashes.add(originalMD5);
                  }
               }

            } catch (Exception e) {
               e.printStackTrace(System.err);
               throw new RuntimeException(e.getMessage());
            }
            finally {
               synchronized (lock) {
                  lock.notifyAll();
               }
            }
         }
      });

      thread.start();

      synchronized (lock) {
         try {
            lock.wait();
         } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
         }
      }

      try {
         Thread.sleep(2 * 1000);
      } catch (InterruptedException ex) {
      }
      
      testInstance.finish(false);
      return verify(pop3Hashes, hashes);
   }
   
   public boolean verify(List<byte[]> pop3Hashes, List<byte[]> hashes) {
      
      Iterator<byte[]> iter = pop3Hashes.iterator();
      byte[] result;
      Iterator<byte[]> iter2;
      while (iter.hasNext()) {
         result = iter.next();
         iter2 = hashes.iterator();
         while (iter2.hasNext()) {
            if (Arrays.equals(result, iter2.next())) {
               iter2.remove();
            }
         }
      }

      return hashes.isEmpty();
   }
}

/**
 * ****************************************************************************
 * This program is a 100% Java Email Server.
 * *****************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com) All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the copyright holder nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************
 * For current versions and more information, please visit:
 * http://javaemailserver.sf.net/
 *
 * or contact the author at: andreaskyrmegalos@hotmail.com
 *
 ******************************************************************************
 * This program is based on the CSRMail project written by Calvin Smith.
 * http://crsemail.sourceforge.net/
 * *****************************************************************************
 *
 * $Rev$ $Date$
 *
 *****************************************************************************
 */
package com.ericdaugherty.mail;

//Java Imports
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

//JUnit Imports
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

//Local imports
import com.ericdaugherty.mail.server.configuration.BackEndType;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.utils.FileUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
@RunWith(Parameterized.class)
public class TestExecutor {

   private static final String baseDir = System.getProperty("basedir");
   private static final File mavenTargetTestClassesDir = new File(baseDir, "target" + File.separator + "test-classes");
   private static final List<File> testMessages = new ArrayList<File>(Arrays.asList(
         new File(mavenTargetTestClassesDir, "mails").listFiles(
         new FileFilter() {
            @Override
            public boolean accept(File file) {
               return file.isFile() && file.getName().toLowerCase().startsWith("testmessage");
            }
         })));
   private static final PasswordAuthenticator defaultSenderCredentials;
   private static final Properties userInfo = new Properties();
   static {

      FileInputStream fis = null;
      try {

         fis = new FileInputStream(new File(mavenTargetTestClassesDir, "users"));
         userInfo.load(fis);
         String sender = null;
         for (String username : userInfo.stringPropertyNames()) {
            sender = username;
            if (sender.toLowerCase(ConfigurationManager.LOCALE).startsWith("sender")) {
               break;
            }
            sender = null;
         }
         if (sender == null) {
            throw new RuntimeException("You have to specify the sender by including an entry in the users file that starts with \"sender.\"");
         }
         defaultSenderCredentials = new PasswordAuthenticator(sender.substring(7), userInfo.getProperty(sender));
         
         userInfo.remove(sender);
      } catch (FileNotFoundException fnfe) {
         throw new ExceptionInInitializerError(fnfe);
      } catch (IOException ioe) {
         throw new ExceptionInInitializerError(ioe);
      } finally {
         IOUtils.close(fis);
      }
   }
   private static final String supportedCiphers = Utils.getSupportedCiphers();

   private static final Set<String> skipTests = new LinkedHashSet<String>();
   
   private static final Map<String, String> resolverParameters = new HashMap<String, String>();

   @Parameters(name = "{index}: test({0})")
   public static Iterable<Object[]> getParameters() throws Exception{
      
      deletePreviousInstances();

      if (System.getProperty("os.name").toLowerCase(ConfigurationManager.LOCALE).contains("win")) {

         File pwdFile = new File(mavenTargetTestClassesDir, "security" + File.separator + "passwordWin");
         if (pwdFile.exists()) {
            FileUtils.copyFile(pwdFile, new File(mavenTargetTestClassesDir, "security" + File.separator + "password"));
            boolean dirGone;
            dirGone = pwdFile.delete();
            if (!dirGone) {
               System.out.println("Unable to delete file " + pwdFile.getName() + ". Ignoring...");
            }
         }
      }

      Properties prop = System.getProperties();
      Set<Map.Entry<Object, Object>> entrySet = prop.entrySet();
      for (Map.Entry<Object, Object> entry : entrySet) {
         Object key = entry.getKey();
         if (key instanceof String) {
            String aKey = (String) key;
            if (aKey.startsWith("dns.")) {
               resolverParameters.put(aKey, (String) entry.getValue());
            }
         }
      }
      
      File forTest = new File(baseDir, "forTest");
      if (!forTest.exists()) {
         boolean dirMade = forTest.mkdir();
         if (!dirMade) {
            throw new RuntimeException("Unable to create folder "+forTest.getName()+" under "+baseDir+". Aborting...");
         }
      }
      String[] surefirePathElements = System.getProperty("surefire.test.class.path").split(File.pathSeparator);
      File aFile;
      for (String surefirePathElement : surefirePathElements) {
         aFile = new File(surefirePathElement);
         if (surefirePathElement.contains("commons-codec") || surefirePathElement.contains("commons-logging") || surefirePathElement.contains("log4j") || surefirePathElement.contains("activation")) {
            FileUtils.copyFile(aFile, new File(forTest, aFile.getName()));
         }
      }

      File file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "tests.xml");

      Document tests = Tools.getXMLDocument(file);

      file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "mail.xml");
      
      Document candidateMail = Tools.getXMLDocument(file);

      Element testsRoot = tests.getDocumentElement();
      if (!testsRoot.getNodeName().equals("tests"))
         throw new IllegalArgumentException("Expected root element 'tests', found \"" + testsRoot.getNodeName() + "\"");
      
      Element skipElement = (Element)Tools.getFirstChildByName(testsRoot, "skip");
      if (skipElement != null) {
         String skips = skipElement.getTextContent();
         Set<String> testsToSkip = new LinkedHashSet<String>(Arrays.asList(skips.trim().split("(\\s+)|([,.]{1})")));
         if (!testsToSkip.isEmpty()) {
            for (String skip : testsToSkip) {
               skip = skip.trim();
               if (skip.isEmpty())
                  continue;
               skipTests.add(skip);
            }
         }
         testsRoot.removeChild(skipElement);
      }
      
      Node settingsNode = null;
      Node realmsNode = null;
      //Get the defaults
      {
         Node aNode = testsRoot.getElementsByTagName("defaults").item(0);
         if (aNode != null) {
            NodeList nodeList = aNode.getChildNodes();
            Node bNode;
            for (int index = 0; index < nodeList.getLength(); index++) {
               bNode = nodeList.item(index);
               if (bNode.getNodeName().equals("settings")) {
                  settingsNode = bNode;
               }
               if (bNode.getNodeName().equals("realms")) {
                  realmsNode = bNode;
               }
               if (settingsNode != null && realmsNode != null) {
                  break;
               }
            }
            testsRoot.removeChild(aNode);
         }
      }

      if (settingsNode != null) {

         Element candidate = candidateMail.getDocumentElement();
         Tools.populateCandidate(Tools.getNodesFromTestsEntry(settingsNode.getFirstChild().getNodeValue(),
               candidate.getOwnerDocument()), candidate, candidateMail);
      }

      String realm = null;
      if (realmsNode != null) {

         realm = realmsNode.getFirstChild().getNodeValue().trim();
         if (realm.indexOf('@') != -1) {
            realm = realm.substring(0, realm.indexOf('@'));
         }
      }

      //Need to persist the changes made to candidateMail
      Tools.persistXMLDocument(candidateMail, file);

      NodeList testNodes = testsRoot.getChildNodes();
      List<TestParameters> parametersList = new ArrayList<TestParameters>(15);
      Node testNode, aNode;
      TestParameters parameters;
      for (int i = 0; i < testNodes.getLength(); i++) {

         testNode = testNodes.item(i);
         if (testNode.getNodeType() != Node.ELEMENT_NODE)
            continue;
         parameters = new TestParameters();
         aNode = testNode.getAttributes().getNamedItem("name");
         if (aNode != null) {
            parameters.setName(aNode.getNodeValue());
         }
         else {
            parameters.setName(testNode.getNodeName());
         }
         parameters.setRealm(realm);

         aNode = Tools.getFirstChildByName(testNode, "messagesPerUser");
         if (aNode != null) {
            parameters.setMessagesPerUser(Integer.parseInt(aNode.getFirstChild().getNodeValue().trim()));
         }
         aNode = Tools.getFirstChildByName(testNode, "runsPerUser");
         if (aNode != null) {
            parameters.setRunsPerUser(Integer.parseInt(aNode.getFirstChild().getNodeValue().trim()));
         }
         if (Tools.getFirstChildByName(testNode, "multithreaded") != null) {
            parameters.setMultithreaded(true);
         }
         aNode = Tools.getFirstChildByName(testNode, "server");
         if (aNode != null) {
            parameters.setServer(aNode.getFirstChild().getNodeValue().trim());
         }

         aNode = Tools.getFirstChildByName(testNode, "javaMail");
         if (aNode != null) {
            String[] entries = aNode.getFirstChild().getNodeValue().trim().split("\\s+");
            String[] keyValue;
            for (String entry : entries) {
               keyValue = entry.split("=");
               parameters.getJavaMailSettings().put(keyValue[0], keyValue[1]);
            }
            parameters.getJavaMailSettings().put("CIPHERS", supportedCiphers);
            String realmUsage = parameters.getJavaMailSettings().getProperty("REALM");
            if (realmUsage == null || realmUsage.equalsIgnoreCase("none")) {
               parameters.getJavaMailSettings().remove("REALM");
            }
            else if (realmUsage.equalsIgnoreCase("default") && realm != null) {
               parameters.getJavaMailSettings().put("REALM",
                     realm.indexOf('@') == -1 ? realm + "@" + parameters.getServer():realm);
            }
         }
         
         aNode = Tools.getFirstChildByName(testNode, "profile");
         if (aNode != null && aNode.getNodeType() == Node.ELEMENT_NODE) {
            parameters.setProfile(Profile.getProfile(aNode.getTextContent()));
         }
         
         aNode = Tools.getFirstChildByName(testNode, "settings");
         parameters.setSettingsNode(aNode);
         
         parametersList.add(parameters);
      }

      Object[][] testParameters = new Object[parametersList.size() - skipTests.size()][2];
      int index = 0;
      for (TestParameters tp : parametersList) {
         if (skipTests.contains(tp.getName())) {
            String message = "Warning - Skipping test: " + tp.getName();
            System.out.println(message);
            continue;
         }
         testParameters[index][0] = tp.getName();
         testParameters[index][1] = tp;
         index++;
      }
      return Arrays.asList(testParameters);
   }
   
   private final AbstractTestInstance testInstance;
   private final AbstractTestCase testCase;
   private final int msgsPerUser;
   private final int runsPerUser;

   /**
    * The class responsible for executing the simulation runs. Dynamic setup and
    * execution.
    *
    * @param testName
    * @param parameters
    * @throws java.lang.Exception
    */
   public TestExecutor(String testName, TestParameters parameters) throws Exception {

      File file;
      
      //Need to create user.conf
      file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "user.conf");
      if (!file.delete() && file.exists())
         throw new IOException("Unable to delete older user.conf file.");
      if (!file.createNewFile())
         throw new IOException("Unable to create file user.conf.");
      
      PrintWriter pw = new PrintWriter(file);
      try {
         pw.println("user."+defaultSenderCredentials.getPasswordAuthentication().getUserName()+"@"+
               parameters.getServer()+"="+defaultSenderCredentials.getPasswordAuthentication().getPassword());
         for (Map.Entry<Object, Object> ui:userInfo.entrySet()) {
            pw.println("user."+ui.getKey()+"@"+parameters.getServer()+"="+ui.getValue());
         }
      } finally {
         IOUtils.close(pw);
      }
      
      //Need to create realms.conf
      file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "realms.conf");
      if (!file.delete() && file.exists())
         throw new IOException("Unable to delete older realms.conf file.");
      if (!file.createNewFile())
         throw new IOException("Unable to create file realms.conf.");
      
      if (parameters.getRealm() != null) {
         StringBuilder sb = new StringBuilder(100);
         sb.append("realm.").append(parameters.getRealm()).append("@").append(parameters.getServer()).append("=");
         for (Map.Entry<Object, Object> ui:userInfo.entrySet()) {

            sb.append(ui.getKey()).append(",");
         }
         sb.append(defaultSenderCredentials.getPasswordAuthentication().getUserName());
         pw = new PrintWriter(file);
         try {
            pw.println(sb.toString());
         } finally {
            IOUtils.close(pw);
         }
      }
      
      file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "mail.xml");
      
      Document candidateMail = Tools.getXMLDocument(file);
      Element candidate = candidateMail.getDocumentElement();
      Node settingsNode = parameters.getSettingsNode();
      if (settingsNode != null) {

         Node[] nodes = Tools.getNodesFromTestsEntry(settingsNode.getFirstChild().getNodeValue(),
               candidate.getOwnerDocument());
         Node aNode;
         String backend = null;
         for (Node node : nodes) {
            if ((aNode = node).getNodeName().equals("backend")) {
               if (Tools.getFirstChildByName(aNode, "File") != null) {
                  backend = "File";
                  break;
               } else if (Tools.getFirstChildByName(aNode, "Db") != null) {
                  backend = "Db";
                  break;
               } else {
                  throw new IllegalArgumentException("The backend needs to be defined. LDAP not supporter currently.");
               }
            }
         }
         Tools.populateCandidate(nodes, candidate, candidateMail);
         
         if ((aNode = Tools.getFirstChildByName(candidate, "backend")) != null) {
            if ("File".equals(backend)) {
               aNode.removeChild(Tools.getFirstChildByName(aNode, "Db"));
               parameters.setBackEndType(BackEndType.FILE);
            } else if ("Db".equals(backend)) {
               aNode.removeChild(Tools.getFirstChildByName(aNode, "File"));
               aNode = Tools.getFirstChildByName(aNode, "Db");
               parameters.setBackEndType(BackEndType.RDBM);
               parameters.setDbServer(aNode.getAttributes().getNamedItem("host").getFirstChild().getNodeValue());
               parameters.setDbPort(Integer.valueOf(aNode.getAttributes().getNamedItem("port").getFirstChild().getNodeValue()));
            }
         }
      }
      
      file = new File(baseDir,
            "src" + File.separator
            + "test" + File.separator
            + "resources" + File.separator
            + "mail-instance.xml");
      if (!file.delete() && file.exists())
         throw new Exception("Unable to delete older mail-instance.xml file.");
      if (!file.createNewFile())
         throw new Exception("Unable to create file mail-instance.xml.");
      
      Tools.persistXMLDocument(candidateMail, file);
      
      Node aNode;
      aNode = Tools.getFirstChildByName(candidate, "cbc");
      if (aNode != null) {
         parameters.setCbcPort(Integer.valueOf(aNode.getAttributes().getNamedItem("port").getFirstChild().getNodeValue()));
      }
      aNode = Tools.getFirstChildByName(candidate, "mail");
      if (aNode != null) {
         aNode = Tools.getFirstChildByName(aNode, "SMTP");
         if (aNode != null) {
            parameters.setSmtpPort(Integer.valueOf(aNode.getAttributes().getNamedItem("port").getFirstChild().getNodeValue()));
         }
      }
      aNode = Tools.getFirstChildByName(candidate, "mail");
      if (aNode != null) {
         aNode = Tools.getFirstChildByName(aNode, "POP3");
         if (aNode != null) {
            parameters.setPop3Port(Integer.valueOf(aNode.getAttributes().getNamedItem("port").getFirstChild().getNodeValue()));
         }
      }
      
      //parameters.getJavaMailSettings().list(System.out);
      
      PasswordAuthenticator senderCredentials;
      String sasl = parameters.getJavaMailSettings().getProperty("SASL");
      if (sasl != null && sasl.equals("DIGEST-MD5")) {
         senderCredentials = new PasswordAuthenticator(defaultSenderCredentials.getPasswordAuthentication().getUserName(),
               defaultSenderCredentials.getPasswordAuthentication().getPassword());
      } else {
         senderCredentials = new PasswordAuthenticator(defaultSenderCredentials.getPasswordAuthentication().getUserName()+"@"+parameters.getServer(),
               defaultSenderCredentials.getPasswordAuthentication().getPassword());
      }
      senderCredentials.setEmailAddress(defaultSenderCredentials.getPasswordAuthentication().getUserName()+"@"+parameters.getServer());
      
      System.out.println("Starting test: " + parameters.getName());
      //Construct the TestInstance
      if (parameters.getBackEndType() == BackEndType.FILE) {
         testInstance = new FileTestInstance(parameters, userInfo, senderCredentials);
         
      } else if (parameters.getBackEndType() == BackEndType.RDBM) {
         testInstance = new DbTestInstance(parameters, userInfo, senderCredentials);
      } else {
         throw new Exception("Unable to construct TestInstance. BackEnd must be either File or Db.");
      }
      
      for (Map.Entry<String, String> entry : resolverParameters.entrySet()) {
         System.setProperty(entry.getKey(), entry.getValue());
      }  
      
      testInstance.setup();
      
      if (parameters.isMultithreaded()) {
         testCase = new MultiTestCase(parameters.getName(), testInstance, Runtime.getRuntime().availableProcessors()*4);
      } else {
         testCase = new SingleTestCase(parameters.getName(), testInstance);
      }
      
      msgsPerUser = parameters.getMessagesPerUser();
      runsPerUser = parameters.getRunsPerUser();
   }

   @Test
   public void runTest() throws Exception {
      
      if (Profile.CBCONLY == testInstance.getProfile()) {
         return;
      }

      System.out.println("running tests for " + testCase.name);
      //Setup the test tasks
      List<Request> requests = new ArrayList<Request>(userInfo.size() * msgsPerUser * runsPerUser);
      Random random = new Random();
      List<String> users = new ArrayList<String>(userInfo.stringPropertyNames());
      int bound = testMessages.size();
      for(int run = 0;run < runsPerUser;run++) {
         for (int msg = 0;msg < msgsPerUser;msg++) {
            for (int usr = 0;usr < users.size();usr++) {
               requests.add(new Request(users.get(usr), testMessages.get(random.nextInt(bound))));
            }
         }
      }
      org.junit.Assert.assertTrue(testCase.execute(requests));
   }

   @After
   public void finishTest() throws IOException {
      testInstance.clearResources();
   }
   
   @AfterClass
   public static void finishSession() throws Exception{
      
      if (!skipTests.isEmpty()) {
         String message = "A number of tests were skipped.";
         System.out.println(message);
      }
      
      File forTest = new File(baseDir, "forTest");
      Utils.deleteFiles(forTest);
      if (!forTest.delete() && forTest.exists()) {
         System.out.println("Unable to delete folder "+forTest.getName()+". Ignoring...");
      }
      File[] jesDirs = new File(System.getProperty("java.io.tmpdir")).listFiles(
         new FileFilter() {
            
            @Override
            public boolean accept(File file) {
               if (file.isDirectory() && file.getName().toLowerCase().startsWith("jes")) {
                  try {
                     Integer.valueOf(file.getName().substring(3));
                     return true;
                  }
                  catch (NumberFormatException nfe){
                     System.out.println(file.getName());
                  }
               }
               return false;
            }
         });
      for (File file:jesDirs) {
         Utils.deleteFilesOnExit(file);
         file.deleteOnExit();
      }
   }
   
   static final void deletePreviousInstances() throws IOException {
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      FileFilter filter = new FileFilter() {

         @Override
         public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().toLowerCase().startsWith("jes");
         }
         
      };
      File[] files = tempDir.listFiles(filter);
      if (files == null)
         return;
      for (File file : files) {
         Utils.deleteFiles(file, true);
         file.delete();
      }
   }
}

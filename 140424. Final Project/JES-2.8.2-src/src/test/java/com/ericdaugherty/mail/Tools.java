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
 * $Rev$
 * $Date$
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

//Java Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.PasswordFactory;
import com.ericdaugherty.mail.server.utils.IOUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Tools {
   
   static void persistXMLDocument(@NonNull Document document, @NonNull File file) throws TransformerException{
      
      Source source = new DOMSource(document);
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();
      StreamResult result = new StreamResult(file);
      transformer.transform(source, result);
   }
   
   static void streamXMLDocument(@NonNull Document document, @NonNull OutputStream stream) throws TransformerException{
      
      Source source = new DOMSource(document);
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();
      StreamResult result = new StreamResult(stream);
      transformer.transform(source, result);
   }
   
   static Node[] getNodesFromTestsEntry(@NonNull String testsEntry, @NonNull Document document) throws Exception{
      
      Element element = document.createElement("config");
      String[] entries = testsEntry.trim().split("\\s+");
      Node[] nodes = new Node[entries.length];
      int nodeIndex = 0;
      for (String entry:entries) {
         String[] path = entry.split("/");
         if (path.length==0) continue;
         String leaf = path[path.length-1];
         //has attributes
         String content = null;
         String[] attributes = null;
         if (leaf.indexOf(';')!=-1) {
            if (leaf.indexOf(':')!=-1) {
               attributes = leaf.substring(leaf.indexOf(';')+1, leaf.indexOf(':')).split(";");
               content = leaf.substring(leaf.indexOf(':')+1);
            }
            else {
               attributes = leaf.substring(leaf.indexOf(';')+1).split(";");
            }
            leaf = leaf.substring(0, leaf.indexOf(';'));
         }
         else if (leaf.indexOf(':')!=-1) {
            content = leaf.substring(leaf.indexOf(':')+1);
            leaf = leaf.substring(0, leaf.indexOf(':'));
         }
         //construct path
         Element finalNode = document.createElement(leaf);
         if (content!=null) {
            finalNode.setTextContent(content);
         }
         if (attributes!=null) {
            String[] keyValue;
            for (String attribute:attributes) {
               keyValue = attribute.split("=");
               finalNode.setAttribute(keyValue[0], keyValue[1]);
            }
         }
         Node firstNode;
         if (path.length>1) {
            firstNode = element.appendChild(document.createElement(path[0]));
            Node aNode = firstNode;
            for (int index = 1;index < path.length-1;index++) {
               aNode = aNode.appendChild(document.createElement(path[index]));
            }
            aNode.appendChild(finalNode);
         }
         else {
            firstNode = finalNode;
         }
         nodes[nodeIndex++] = firstNode;
      }
      return nodes;
   }

   static Document getXMLDocument(@NonNull File xmlFile) throws Exception{
      
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      
      Document doc;
      FileInputStream fis = null;
      try {
         doc = db.parse(fis = new FileInputStream(xmlFile));
         doc.getDocumentElement().normalize();
         return doc;
      } finally {
         IOUtils.close(fis);
      }
   }
   
   static String getTextNodeValueFromFirstChildByName(@NonNull Node node, @NonNull String name) {
      
      NodeList list = node.getChildNodes();
      Node aNode;
      for(int i=0;i<list.getLength();i++) {
         aNode = list.item(i);
         if(aNode.getNodeName().equals(name)) {
            aNode = aNode.getFirstChild();
            if (aNode != null && aNode.getNodeType() == Node.TEXT_NODE)
               return aNode.getNodeValue();
            break;
         }
      }
      return null;
   }
   
   static String getFirstTextNodeValue(@NonNull Node node) {
      
      if (!node.hasChildNodes())
         return null;
      NodeList list = node.getChildNodes();
      for(int i=0;i<list.getLength();i++) {
         if (list.item(i).getNodeType() == Node.TEXT_NODE)
            return list.item(i).getNodeValue();
      }
      return null;
   }
   
   static Node getFirstChildByName(@NonNull Node node, @NonNull String name) {
      
      if (!node.hasChildNodes())
         return null;
      NodeList list = node.getChildNodes();
      for (int i=0;i<list.getLength();i++) {
         if(list.item(i).getNodeName().equals(name)) {
            return list.item(i);
         }
      }
      return null;
   }

   static Node[] populateCandidate(@NonNull Node[] settings,
         @NonNull Element candidate, @NonNull Document candidateMail) throws Exception {

      Node nextNode;
      for (Node aNode : settings) {

         do {
            nextNode = (Element) Tools.getFirstChildByName(candidate, aNode.getNodeName());
            if (nextNode == null)
               throw new Exception("No node named " + aNode.getNodeName()
                     + " found attached to " + candidate.getNodeName()
                     + ". Aborting...");
            candidate = (Element) nextNode;
            nextNode = aNode;

         } while ((aNode = aNode.getFirstChild()) != null && aNode.getNodeType() == Node.ELEMENT_NODE);

         if (nextNode.hasChildNodes() && nextNode.getFirstChild().getNodeValue() != null && !nextNode.getFirstChild().getNodeValue().isEmpty()) {
            candidate.setTextContent(nextNode.getFirstChild().getNodeValue());
         }
         NamedNodeMap nnm = nextNode.getAttributes();
         if (nnm != null && nnm.getLength() != 0) {
            for (int index = 0; index < nnm.getLength(); index++) {
               candidate.setAttribute(nnm.item(index).getNodeName(), nnm.item(index).getFirstChild().getNodeValue());
            }
         }
         candidate = candidateMail.getDocumentElement();
      }
      return settings;
   }
   
   static void createMD5File(@NonNull String input) throws IOException, GeneralSecurityException{

      File output = new File(input + ".md5");
      if (!output.exists() && !output.createNewFile())
         throw new IOException("Unable to create file " + output.getName());
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(output);
         fos.write(Utils.getDerivedMD5(new File(input)));
         fos.write(System.getProperty("line.separator").getBytes(ConfigurationManager.getUtf8Charset()));
         fos.flush();
      }
      finally {
         IOUtils.close(fos);
      }
   }

   private static void createTruststoreWithCACertificate(@NonNull String certificatePathname,
         @NonNull String alias, @NonNull String truststorePathname) {
      CertificateFactory cf;
      BufferedInputStream bis = null;
      FileOutputStream fos = null;
      Certificate serverCert;
      try {
         cf = CertificateFactory.getInstance("X.509");
         bis = new BufferedInputStream(new FileInputStream(new File(certificatePathname)));
         fos = new FileOutputStream(new File(truststorePathname));
         serverCert = (X509Certificate)cf.generateCertificate(bis);
         KeyStore ks = KeyStore.getInstance("jks");
         ks.load(null, null);
         ks.setCertificateEntry(alias, serverCert);
         ks.store(fos, "password".toCharArray());

      }
      catch (Exception e) {
         e.printStackTrace(System.err);
      } finally {
         IOUtils.close(bis);
         IOUtils.close(fos);
      }
   }

   private static void createKeystoreWithPrivateKey(@NonNull String pkcs12Alias,
         @NonNull String pkcs12Password, @NonNull String pkcs12Pathname,
         @NonNull String alias, @NonNull String password, @NonNull String keystorePathname ) {

      char[] pass = password.toCharArray();
      char[] pkcs12Pass = pkcs12Password.toCharArray();
      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
         KeyStore tempks = KeyStore.getInstance("pkcs12", "SunJSSE");
         File pkcs12 = new File(pkcs12Pathname);
         tempks.load(fis = new FileInputStream(pkcs12), pkcs12Pass);
         Key key = tempks.getKey(pkcs12Alias, pkcs12Pass);
         if (key == null) {
            throw new RuntimeException("Got null key from keystore!");
         }
         RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) key;
         Certificate[] clientCerts = tempks.getCertificateChain(pkcs12Alias);
         if (clientCerts == null) {
            throw new RuntimeException("Got null cert chain from keystore!");
         }
         KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry(privKey, clientCerts);
         KeyStore.ProtectionParameter kspp = new KeyStore.PasswordProtection(pass);
         fos = new FileOutputStream(keystorePathname);
         KeyStore ks = KeyStore.getInstance("jceks");
         ks.load(null, pass);
         ks.setEntry(alias, pke, kspp);
         ks.store(fos, pass);

      }
      catch (Exception e) {
         e.printStackTrace(System.err);
      } finally {
         PasswordFactory.clear(pass);
         IOUtils.close(fis);
         IOUtils.close(fos);
      }
   }

   public static void main(String[] input) throws Exception{

      if (input[0].toLowerCase().equals("md5")) createMD5File(input[1]);
      else if (input[0].toLowerCase().equals("tru")) createTruststoreWithCACertificate(input[1], input[2], input[3]);
      else if (input[0].toLowerCase().equals("key")) createKeystoreWithPrivateKey(input[1], input[2], input[3], input[4], input[5], input[6]);
      else if (input[0].toLowerCase().equals("xml")) {
         String test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"+
                       "<config>\r\n"+
                       "\t<test attrib=\"attribValue\">\r\n"+
                       "\t\tnodeValue\n"+
                       "\t</test>\r\n"+
                       "</config>";
         Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(test.getBytes(ConfigurationManager.getUtf8Charset())));
         System.out.println("returned value is "+getTextNodeValueFromFirstChildByName(doc.getDocumentElement(), "test"));
      }
   }

}

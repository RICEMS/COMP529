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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class add to a pre 1.6 Java version the ability to handle a properties
 * class without the limitation of using unicode escape sequences to persist
 * non iso-8859-1 characters to a file. All the terms regarding the processing
 * of properties are inherited from the properties class with the obvious
 * exception referred to before. Any character set encoding can be used as long
 * as the java virtual machine instantiating this class supports it.
 * The file encoding is not allowed to be altered and is only declared in the
 * class constructor. Thus the properties will be saved in the same format as
 * the one defined during the creation of an instance of this class. For this
 * to be achieved, one should not attempt to save the properties via a call to
 * a store method of the properties instanced class but rather to one of the
 * store methods in this class. Static methods are provided to save properties
 * to a file when JESProperties was not used to load the properties from disk.
 * 
 * <b>ATTENTION!</b> This class, when instantiated with the charMode flag set to
 * <b>true</b>, breaks the {@link Properties} contract in that it stores the values to
 * the internal properties file as an array of primitive characters and <b>NOT</b>
 * as a String. Therefore it is not a 100% replacement for the Properties class.
 *
 * @author Andreas Kyrmegalos
 */
public class JESProperties {

   private final DelimitedInputStream dis;
   private final String fileEncoding;
   private Properties properties;
   private final ValueLoader vl;
   
   private class ValueLoader {
      
      protected Object finishLoadingValue(char[] source, int start, int length) {
         return finishLoadingString(source,start,length);
      }
   }
   
   private class CharValueLoader extends ValueLoader{
      
      @Override
      protected Object finishLoadingValue(char[] source, int start, int length) {
         return finishLoadingChar(source,start,length);
      }
   }
   
   private JESProperties(Properties properties) {
      this(properties,System.getProperty("file.encoding"), false);
   }

   private JESProperties(Properties properties, String fileEncoding) {
      this(properties, fileEncoding, false);
   }

   private JESProperties(Properties properties, String fileEncoding, boolean charMode) {
      this.dis = null;
      this.fileEncoding = fileEncoding;
      this.properties = properties;
      if (!charMode) {
         vl = new ValueLoader();
      }
      else {
         vl = new CharValueLoader();
      }
   }

   public JESProperties(DelimitedInputStream dis) throws IOException {
      this(dis, System.getProperty("file.encoding"), false);
   }

   public JESProperties(DelimitedInputStream dis, boolean charMode) throws IOException {
      this(dis, System.getProperty("file.encoding"), charMode);
   }

   public JESProperties(DelimitedInputStream dis, String fileEncoding) throws IOException{
      this(dis, fileEncoding, false);
   }
   
   public JESProperties(DelimitedInputStream dis, String fileEncoding, boolean charMode) throws IOException{
      this.dis = dis;
      if (!Charset.forName(fileEncoding).equals(Charset.forName(dis.getEncoding()))) {
         this.fileEncoding = dis.getEncoding();
      }
      else {
         this.fileEncoding = fileEncoding;
      }
      if (!charMode) {
         vl = new ValueLoader();
      }
      else {
         vl = new CharValueLoader();
      }
   }

   /**
    * Populates the properties with keys and values from the supplied stream.
    * The load, get and store operations are not Thread-safe. Should be synchronized
    * externally.
    * 
    * @throws IOException 
    */
   public void load() throws IOException{

      properties = new Properties();

      byte[] logicalLine = new byte[10];
      byte[] naturalLine;
      int currentLLCount=0, currentNLCount=0, nLLength, aByte;
      boolean ignoreWS = true, continuedLogicalLine = false, gotABackslash = false;

      try {
         while((naturalLine=dis.readLine())!=null) {
            nLLength = naturalLine.length;
            if (nLLength==0) continue;
            aByte = naturalLine[0];
            if (!continuedLogicalLine&&(aByte==0x23||aByte==0x21)) {
               continue;
            }
            for(;currentNLCount<nLLength;) {
               aByte = naturalLine[currentNLCount];
               if (ignoreWS&&(aByte==0x20||aByte==0x09||aByte==0x0c)) {
                  currentNLCount++;
                  continue;
               }
               ignoreWS = false;
               if (aByte==0x5c) {
                  gotABackslash = !gotABackslash;
               }
               else {
                  gotABackslash = false;
               }
               logicalLine[currentLLCount++] = naturalLine[currentNLCount++];
               if (currentLLCount==logicalLine.length) {
                  byte[] temp = new byte[currentLLCount*2];
                  System.arraycopy(logicalLine, 0, temp, 0, currentLLCount);
                  logicalLine = temp;

               }
            }
            if (gotABackslash) {
               continuedLogicalLine = true;
            }
            else if (currentLLCount>0){
               createKeyValuePair(logicalLine,currentLLCount);
               logicalLine = new byte[10];
               currentLLCount = 0;
            }
            ignoreWS = true;
            currentNLCount = 0;

         }
      }
      finally {
         IOUtils.close(dis);
      }
   }

   private void createKeyValuePair(byte[] logicalLine, int length) throws UnsupportedEncodingException {

      char[] line = new String(logicalLine,0,length, fileEncoding).toCharArray();
      int lineLength = line.length, currentPosition = 0, keyFinish = 0, valueStart=lineLength;
      boolean gotABackslash = false, separatorFound = false;
      char aChar;
      String key;
      Object value;
      for (;currentPosition<lineLength;) {
         aChar = line[currentPosition];
         if (!gotABackslash) {
            if ((aChar==':'||aChar=='=')) {
               separatorFound = true;
               valueStart = currentPosition+1;
               break;
            }
            else if ((aChar == ' ' || aChar == '\t' ||  aChar == '\f')){
               valueStart = currentPosition+1;
               break;
            }
         }
         if (aChar=='\\') {
            gotABackslash = !gotABackslash;
         }
         else {
            gotABackslash = false;
         }
         keyFinish = ++currentPosition;
      }
      key = finishLoadingString(line,0,keyFinish);
      for (;currentPosition<lineLength;) {
         aChar = line[currentPosition];
         if (aChar != ' ' && aChar != '\t' &&  aChar != '\f') {
            if (!separatorFound && (aChar == '=' ||  aChar == ':')) {
               separatorFound = true;
            } else {
               break;
            }
         }
         valueStart = ++currentPosition;
      }
      value = vl.finishLoadingValue(line,valueStart,lineLength-valueStart);
      properties.put(key, value);
   }

   private String finishLoadingString(char[] source, int start, int length) {

      StringBuilder sb = new StringBuilder(length);
      int currentSourceCount = start;
      char aChar;
      for (start=0;start<length;start++) {
         aChar = source[currentSourceCount++];
         if (aChar=='\\') {
            aChar = source[currentSourceCount++];
            start++;
            if (aChar == 't') aChar = '\t';
            else if (aChar == 'r') aChar = '\r';
            else if (aChar == 'n') aChar = '\n';
            else if (aChar == 'f') aChar = '\f';
         }
         sb.append(aChar);
      }
      return sb.toString();
   }

   private char[] finishLoadingChar(char[] source, int start, int length) {

      StringBuilder sb = new StringBuilder(length);
      int currentSourceCount = start;
      char aChar;
      for (start=0;start<length;start++) {
         aChar = source[currentSourceCount++];
         if (aChar=='\\') {
            aChar = source[currentSourceCount++];
            start++;
            if (aChar == 't') aChar = '\t';
            else if (aChar == 'r') aChar = '\r';
            else if (aChar == 'n') aChar = '\n';
            else if (aChar == 'f') aChar = '\f';
         }
         sb.append(aChar);
      }
      char[] value = new char[sb.length()];
      sb.getChars(0, sb.length(), value, 0);
      return value;
   }

   /**
    * @param target
    * @param comments
    * @throws java.io.IOException
    * @see #store(File, String)
    */
   public void store(String target, String comments) throws IOException {
      store(new File(target), comments);
   }

   /**
    * Stores the keys, values in the properties file using the supplied
    * stream. The load, get and store operations are not Thread-safe. Should be
    * synchronized externally.
    * 
    * @param target
    * @param comments
    * @throws IOException 
    */
   public void store(File target, String comments) throws IOException {
      BufferedWriter bw = null;
      try {
         bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target),fileEncoding));
         if (comments!=null) {
            writeComments(bw, comments);
         }
         bw.write("#" + new Date().toString());
         bw.newLine();
         
         Iterator iter = properties.entrySet().iterator();
         String key;
         Object value;
         Entry entry;
         StringBuilder sb;
         char[] toWrite;
         while(iter.hasNext()) {
            entry = (Entry)iter.next();
            key = (String)entry.getKey();
            value = entry.getValue();
            if (value instanceof String) {
               bw.write(finishSavingString(key,true)+"="+finishSavingString((String)value,false));
            }
            else {
               sb = new StringBuilder(key.length()+1+((char[])value).length);
               sb.append(finishSavingString(key,true));
               sb.append('=');
               sb.append(finishSavingChar((char[])value,false));
               toWrite = new char[sb.length()];
               sb.getChars(0, sb.length(), toWrite, 0);
               bw.write(toWrite);
            }
            bw.newLine();
         }

      }
      finally {
         IOUtils.close(bw);
      }
   }

   private String finishSavingString(String source, boolean escapeSpaces) {

      int length = source.length();
      StringBuilder sb = new StringBuilder(length+10);
      for (int current=0;current<length;current++) {
         appendToStringBuilder(source.charAt(current), current, sb, escapeSpaces);
      }
      return sb.toString();
   }

   private char[] finishSavingChar(char[] source, boolean escapeSpaces) {

      int length = source.length;
      StringBuilder sb = new StringBuilder(length+10);
      for (int current=0;current<length;current++) {
         appendToStringBuilder(source[current], current, sb, escapeSpaces);
      }
      char[] output = new char[sb.length()];
      sb.getChars(0, sb.length(), output, 0);
      return output;
   }
   
   private void appendToStringBuilder(char aChar, int current, StringBuilder sb, boolean escapeSpaces) {
         if (aChar=='\\'|| (aChar=='#'&&current==0) || (aChar=='!'&&current==0) ||aChar=='=' || aChar==':') {
            sb.append('\\');
         }
         else if(aChar=='\r') {
            sb.append('\\');
            sb.append('r');
            return;

         }
         else if(aChar=='\n') {
            sb.append('\\');
            sb.append('n');
            return;

         }
         else if(aChar=='\t') {
            sb.append('\\');
            sb.append('t');
            return;

         }
         else if(aChar=='\f') {
            sb.append('\\');
            sb.append('f');
            return;

         }
         else if(aChar==' ') {
            if (escapeSpaces||current==0) {
               sb.append('\\');
            }
         }
         sb.append(aChar);
   }

   private void writeComments(BufferedWriter bw, String comments) throws IOException{

      int start = 0;
      for (int i=0;i<comments.length();i++) {
         if (comments.charAt(i)=='\r'||comments.charAt(i)=='\n') {
            if (comments.charAt(start)!='#') bw.write("#");
            bw.write(comments,start,i-start);
            bw.newLine();
            if (i<comments.length()-1&&comments.charAt(i+1)=='\n') {
               i++;
            }
            start = i+1;
         }
         if (i==comments.length()-1) {
            if (comments.charAt(start)!='#') bw.write("#");
            bw.write(comments,start,i-start);
            bw.newLine();
         }
      }

   }

   public static void store(Properties properties, String fileEncoding, String target, String comments) throws IOException {
      store(properties,fileEncoding,new File(target),comments);
   }

   public static void store(Properties properties, String target, String comments) throws IOException {
      store(properties,new File(target),comments);
   }

   public static void store(Properties properties, String fileEncoding, File target, String comments) throws IOException {
      new JESProperties(properties, fileEncoding).store(target, comments);
   }

   public static void store(Properties properties, File target, String comments) throws IOException {
      new JESProperties(properties).store(target, comments);
   }

   /**
    * Retrieves the Properties files. The load, get and store operations
    * are not Thread-safe. Should be synchronized externally.
    * 
    * @return Properties as constructed using the supplied stream
    */
   @NonNull
   public Properties getProperties() {
      return properties != null ? properties : new Properties();
   }

}

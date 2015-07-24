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

package com.ericdaugherty.mail.server.services.smtp.server;

//Java Imports
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Encoding Imports
import org.apache.commons.codec.binary.Base64;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.services.smtp.MIMEConstants;
import com.ericdaugherty.mail.server.services.smtp.server.support.AddDataLine;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 * Each line of an incoming message is parsed based on rfc MIME rules.
 *
 * @author Andreas Kyrmegalos
 */
public class MIMEProcessor implements MIMEConstants {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog( MIMEProcessor.class );
   
   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   MIMEBody mimeBody;

   boolean message8bitMIME;

   AddDataLine addDataLine;

   private Domain domain;
   
   private List<byte[]> rawTextForEncoding;

   MIMEProcessor() {}

   static final class MIMEBody {

      private final MIMEBody parent;
      public int mime = MIME_UNDEFINED, submime = MIME_UNDEFINED;
      public boolean initialHeaders = true, readingHeaders, base64EncodeText, hasMIMEVersion;
      private String boundary = MIMEBOUNDARY;
      private boolean switchToChildMIMEBody;

      public MIMEBody(MIMEBody parent) {
         this.parent = parent;
      }

   }

   protected void setDomain(Domain domain) {
      this.domain = domain;
   }

   protected final void processDATA(byte[] output) throws IOException {

      if (!(mimeBody.initialHeaders||mimeBody.readingHeaders||(output.length!=0&&(output[0]==0x02D&&output[1]==0x02D)))) {
         if (!mimeBody.base64EncodeText) {
            addDataLine.addDataLine(output);
         }
         else {
            rawTextForEncoding.add(output);
         }
      }
      else {
         String input = process(output);
        if (mimeBody.initialHeaders) {
           String capitalized = input.toUpperCase(ConfigurationManager.LOCALE);
           if (!mimeBody.hasMIMEVersion&&capitalized.startsWith(MIMEVERSION)) {
              int parenthesesStart = capitalized.indexOf('(');
              int parenthesesFinish = capitalized.indexOf(')');
              if (parenthesesStart==-1&&parenthesesFinish==-1) {
                  mimeBody.hasMIMEVersion = true;
              }
              else if (!(parenthesesStart>=parenthesesFinish||(parenthesesStart>-1&&parenthesesFinish==-1)||(parenthesesStart==-1&&parenthesesFinish>=-1))) {
                 String deComment = capitalized.substring(0,parenthesesStart)+capitalized.substring(parenthesesFinish+1);
                 if (deComment.indexOf("1.0")!=-1) {
                    mimeBody.hasMIMEVersion = true;
                 }
              }
           }
           if(capitalized.startsWith(MIMECONTENT_TYPE) && mimeBody.mime == MIME_UNDEFINED) {
              if (capitalized.indexOf(MIMEMULTIPART)!=-1) {
                 mimeBody.mime = MIME_MULTIPART;
                 if (capitalized.indexOf(MIMEBOUNDARY)!=-1) {
                    if (input.lastIndexOf("\"")!=-1) {
                       mimeBody.boundary = input.substring(input.indexOf("\"")+1,input.lastIndexOf("\""));
                    }
                    else {
                       mimeBody.boundary = input.substring(input.lastIndexOf("=")+1);
                    }
                 }
              }
              else {
                 if (capitalized.indexOf(MIMETEXT)!=-1) {
                    mimeBody.mime = MIME_TEXT;
                 }
                 else {
                    mimeBody.mime = MIME_OTHER;
                 }
                 mimeBody.readingHeaders = true;
                 mimeBody.initialHeaders = false;

              }

           }
           else if (mimeBody.mime == MIME_MULTIPART) {
              if (mimeBody.boundary.equals(MIMEBOUNDARY) && capitalized.indexOf(MIMEBOUNDARY)!=-1) {
                 if (input.lastIndexOf("\"")!=-1) {
                    mimeBody.boundary = input.substring(input.indexOf("\"")+1,input.lastIndexOf("\""));
                 }
                 else {
                    mimeBody.boundary = input.substring(input.lastIndexOf('=')+1);
                 }
              }
              if (!mimeBody.boundary.equals(MIMEBOUNDARY) && input.indexOf(mimeBody.boundary)!=-1) {
                 mimeBody.readingHeaders = true;
                 mimeBody.initialHeaders = false;
              }

           }
           //If the first blank line is reached perform some checks
           if (input.length()==0) {

              if (mimeBody.mime!=MIME_UNDEFINED) {
                 //Make sure a MIME-Version header field exists in the presence of
                 //a content-type header-field. It is reasonable to assume that the
                 //MIME-Version header field should exist in such a case.
                 if (!mimeBody.hasMIMEVersion) {
                    mimeBody.hasMIMEVersion = true;
                    addDataLine.addDataLine(("MIME-Version: 1.0").getBytes(usAsciiCharset));
                 }
              }
              else {
                 //If no content-type is found, insert the default defined in RFC2045/5.2
                 //A MIME-Version header-field is expected to be present
                 if (mimeBody.hasMIMEVersion) {
                    addDataLine.addDataLine(("Content-type: text/plain; charset=us-ascii").getBytes(usAsciiCharset));
                 }
              }
              //No headers need be read at this point
              mimeBody.initialHeaders = false;
              mimeBody.readingHeaders = false;
           }
           addDataLine.addDataLine(output);

        }
        else if (mimeBody.readingHeaders) {
           String capitalized = input.toUpperCase(ConfigurationManager.LOCALE);
           if (mimeBody.mime == MIME_MULTIPART && capitalized.startsWith(MIMECONTENT_TYPE)) {
              if (capitalized.indexOf(MIMETEXT)!=-1) {
                 mimeBody.submime = MIME_TEXT;
              }
              else {
                 if (capitalized.indexOf(MIMERFC822)!=-1) {
                    mimeBody.switchToChildMIMEBody = true;
                 }
                 mimeBody.submime = MIME_OTHER;
              }
              addDataLine.addDataLine(output);
           }
           else if (mimeBody.mime == MIME_MULTIPART || mimeBody.mime == MIME_TEXT) {
              if (capitalized.startsWith(MIMECONTENT_TRANSFER_ENCODING)) {
                 if (capitalized.indexOf(MIME8BIT)!=-1 && (mimeBody.submime == MIME_TEXT || mimeBody.mime == MIME_TEXT) && !message8bitMIME) {
                    if (input.indexOf(';')!=-1) {
                       addDataLine.addDataLine((MIMEBASE64ENCODING+input.substring(input.indexOf(";"))).getBytes(usAsciiCharset));
                    }
                    else {
                       addDataLine.addDataLine((MIMEBASE64ENCODING).getBytes(usAsciiCharset));
                    }
                    mimeBody.base64EncodeText = true;
                    rawTextForEncoding = new ArrayList<byte[]>();
                 }
                 else {
                    addDataLine.addDataLine(output);
                 }
              }
              else if (input.equals("")) {
                 if (mimeBody.base64EncodeText) {
                    addDataLine.addDataLine((MIMEAAUTOCONVERT+domain.getDomainName()).getBytes(usAsciiCharset));
                 }
                 addDataLine.addDataLine(new byte[]{});
                 mimeBody.readingHeaders = false;
                 if (mimeBody.switchToChildMIMEBody) {
                    mimeBody = new MIMEBody(mimeBody);
                 }
              }
              else {
                 addDataLine.addDataLine(output);
              }
           }
           else {
              if (input.equals("")) {
                 mimeBody.readingHeaders = false;
                 addDataLine.addDataLine(new byte[]{});
                 if (mimeBody.switchToChildMIMEBody) {
                    mimeBody = new MIMEBody(mimeBody);
                 }
              }
              else {
                 addDataLine.addDataLine(output);
              }
           }
        }
        else if (input.indexOf(mimeBody.boundary)!=-1) {
           if (mimeBody.base64EncodeText) {
              mimeBody.base64EncodeText = false;
              encodeBase64();
           }
           mimeBody.readingHeaders = true;
           if (input.indexOf(mimeBody.boundary+"--")!=-1) {
              //Parent can be null (mimeBody is the top level message body in such a case)
              //If that is indeed the case this is practically the end of the entire message
              //when a top-level boundary has been defined
              if (mimeBody.parent!=null) {
                 mimeBody = mimeBody.parent;
                 mimeBody.switchToChildMIMEBody = false;
              }
           }
           addDataLine.addDataLine(output);
        }
        else {
            addDataLine.addDataLine(output);
        }
      }
   }

   protected boolean checkEndOfDATA(byte[] output) throws IOException{
      if (output.length==1 && output[0]==0x2E) {
         if (mimeBody.mime==MIME_TEXT && mimeBody.base64EncodeText) {
            mimeBody.base64EncodeText = false;
            encodeBase64();
         }
         return true;
      }
      return false;
   }

    protected void encodeBase64() throws IOException {
       int emptyLines = 0;
       while (new String(rawTextForEncoding.get(rawTextForEncoding.size()-1)).trim().equals("")) {
          rawTextForEncoding.remove(rawTextForEncoding.size()-1);
          emptyLines++;
       }
       int totalByteArraySize = 0, rawTextCount = rawTextForEncoding.size();
       for (int i=0;i<rawTextCount;i++) {
          totalByteArraySize+=(rawTextForEncoding.get(i)).length;
       }
       byte[] toEncode = new byte[totalByteArraySize+rawTextCount*2];
       int position = 0, tempToEncodeLength;
       byte[] tempToEncode;
       for (int i=0;;i++) {
          tempToEncode = rawTextForEncoding.get(i);
          tempToEncodeLength = tempToEncode.length;
          System.arraycopy(tempToEncode, 0, toEncode, position, tempToEncodeLength);
          toEncode[position+tempToEncodeLength]   = 0x0d;
          toEncode[position+tempToEncodeLength+1] = 0x0a;
          if (i==rawTextCount-1) break;
          position += tempToEncodeLength+2;
       }
       encodeBase64(toEncode);
       for (int i=0;i<emptyLines;i++) {
          addDataLine.addDataLine( new byte[]{} );
       }

       for (int i=totalByteArraySize-1;i>=0;i--) {
          toEncode[i] = 0;
       }

       rawTextForEncoding.clear();
       rawTextForEncoding = null;
    }

    private void encodeBase64(byte[] input) throws IOException{
       DelimitedInputStream dis = null;
       try {
          dis = new DelimitedInputStream( new ByteArrayInputStream(Base64.encodeBase64Chunked(input)),new byte[]{0x0d,0x0a} );
          for(;;) {
            byte[] inLine = dis.readLine();
            if (inLine == null) break;
            else {
               addDataLine.addDataLine( inLine );
            }
          }
       }
       finally {
          IOUtils.close(dis);
       }
    }

    /**
     * This method converts a sequence of bytes to a String
     *
     * @param input a sequence of bytes from the incoming stream
     * @return a string corresponding to a line of input from the stream encoded
     * in US-ASCII
     */
    private String process(byte[] input) {
      int length = input.length;
      if (length==0) return "";
      return new String(input, 0, length, usAsciiCharset);
    }
}

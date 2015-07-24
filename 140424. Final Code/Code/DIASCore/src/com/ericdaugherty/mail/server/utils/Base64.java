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

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Base64 {
   
   private static final byte[] BASE64_AB_MIME = new byte[]{
   
           0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0x4a,0x4b,0x4c,0x4d,0x4e,0x4f,//A-Z
      0x50,0x51,0x52,0x53,0x54,0x55,0x56,0x57,0x58,0x59,0x5a,
           0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0x6a,0x6b,0x6c,0x6d,0x6e,0x6f,//a-z
      0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7a,
      0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,//0-9
      0x2b,0x2f//+,/
   };
   
   private static final byte[] LINEBRAKE_MIME = new byte[]{0x0d,0x0a};//CRLF
   
   private static final byte PADDING_MIME = 0x3d;//=
   
   private static final int LINE_LENGTH_MIME = 76;
   
   private static final int DEFAULT_ENCODING_SIZE = 12;
   
   private byte[] lineBrake = LINEBRAKE_MIME;
   
   private byte padding = PADDING_MIME;
   
   private int lineLength = LINE_LENGTH_MIME;
   
   private int lineCount;
   
   private int encodingPosition;
   
   private byte[] encodingOutput;
   
   private byte remain;
   
   private int remainder;
   
   private boolean completed;
   
   public Base64() {}
   
   public Base64(byte[] lineBrake, byte padding, int lineLength) {
      
      this.lineBrake = lineBrake.clone();
      this.padding = padding;
      this.lineLength = lineLength;
   }
   
   private void resizeEncodingOutput(int hint) {
      if (encodingOutput==null) {
         encodingOutput = new byte[Math.max(DEFAULT_ENCODING_SIZE,hint)];
         encodingPosition = 0;
      }
      else {
         byte[] temp = new byte[encodingOutput.length+Math.max(DEFAULT_ENCODING_SIZE,hint)];
         System.arraycopy(encodingOutput, 0, temp, 0, encodingOutput.length);
         encodingOutput = temp;
      }
   }
   
   private void checkLineLength() {
      if (encodingPosition-(lineCount*(lineLength+lineBrake.length))==lineLength) {
         lineCount++;
         if ((encodingPosition+lineBrake.length)>=encodingOutput.length) {
            resizeEncodingOutput(0);
         }
         System.arraycopy(lineBrake,0,encodingOutput,encodingPosition,lineBrake.length);
         encodingPosition += lineBrake.length;
      }
   }
   
   void encodeMIME(byte[] input, int offset, int length) {
      
      //System.out.println("offset "+offset+" length "+length);
      if (length==0||completed) return;
      int encodingHint = (int)Math.ceil(length/57.);
      encodingHint *= 76+2;
      if (encodingOutput==null||encodingHint>=(encodingOutput.length-encodingPosition)) {
         resizeEncodingOutput(encodingHint);
      }
      //System.out.println(encodingHint+" "+(encodingOutput.length-encodingPosition));
      int groupIter = 0;
      byte a,b,c;
      if (remainder==2) {
         if (length>0) {
            groupIter += 1;
            b = remain;
            c = input[offset];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[c & 0x3F];
            checkLineLength();
         }
      }
      else if (remainder==1) {
         if (length>1) {
            groupIter += 2;
            a = remain;
            b = input[offset];
            c = input[offset+1];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[((a << 4) & 0x30) + ((b >>> 4) & 0xf)];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[c & 0x3F];
            checkLineLength();
         }
         else {
            groupIter += 1;
            a = remain;
            b = input[offset];
            encodingOutput[encodingPosition++] = BASE64_AB_MIME[((a << 4) & 0x30) + ((b >>> 4) & 0xf)];
            remain = b;
            remainder = 2;
            return;
         }
      }
      int alignedLength = length - groupIter;
      remainder = alignedLength%3;
      alignedLength = alignedLength - (alignedLength%3);
      //System.out.println("groupIter "+groupIter+" alignedLength "+alignedLength+" length "+length);
      //System.out.println("remainder "+remainder);
      for (;groupIter<alignedLength;groupIter+=3) {
         a = input[offset+groupIter];
         b = input[offset+groupIter+1];
         c = input[offset+groupIter+2];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[(a >>> 2) & 0x3F];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[((a << 4) & 0x30) + ((b >>> 4) & 0xf)];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[c & 0x3F];
         checkLineLength();
      }
      if (remainder==2) {
         a = input[offset+groupIter];
         b = input[offset+groupIter+1];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[(a >>> 2) & 0x3F];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[((a << 4) & 0x30) + ((b >>> 4) & 0xf)];
         remain = b;
      }
      else if (remainder==1) {
         a = input[offset+groupIter];
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[(a >>> 2) & 0x3F];
         remain = a;
      }
   }
   
   byte[] doFinalEncodeMIME() {
      
      if (completed) return encodingOutput;
      completed = true;
      byte a,b;
      if (remainder==2) {
         b = remain;
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[((b << 2) & 0x3c)];
         encodingOutput[encodingPosition++] = padding;
      }
      else if (remainder==1) {
         a = remain;
         encodingOutput[encodingPosition++] = BASE64_AB_MIME[((a << 4) & 0x30)];
         encodingOutput[encodingPosition++] = padding;
         encodingOutput[encodingPosition++] = padding;
      }
      if (encodingPosition<encodingOutput.length) {
         byte[] temp = new byte[encodingPosition];
         System.arraycopy(encodingOutput,0,temp,0,encodingPosition);
         encodingOutput = temp;
      }
      return encodingOutput;
   }
   
   @NonNull
   byte[] drainEncodedOutputMIME() {
      
      if (completed || lineCount == 0) return new byte[0];
      
      int drainLength = lineCount*(lineLength+lineBrake.length);
      lineCount = 0;
      byte[] drainOutput = new byte[drainLength];
      System.arraycopy(encodingOutput, 0, drainOutput, 0, drainLength);
      encodingPosition -= drainLength;

      byte[] temp = new byte[encodingPosition];
      System.arraycopy(encodingOutput, drainLength, temp, 0, encodingPosition);
      encodingOutput = temp;
      return drainOutput;
   }
   
   public void dispose() {
      
      lineBrake = null;
      encodingOutput = null;
   }
}

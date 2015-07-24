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

//Java imports
import java.security.GeneralSecurityException;

/**
 * Various SMTP/POP3 related settings.
 *
 * @author Andreas Kyrmegalos
 */
public class ByteUtils {
   
   public static char[] toHex(byte[] bArray) {
      
      char[] chars = new char[2*bArray.length];
      int i,b;
      for (i = 0; i < bArray.length; i++) {
         b = ((bArray[i]&0xff) & 0xf0)>>>4;
         if (b>=0x0a) {
            chars[i * 2] = (char)(b + 0x57);
         }
         else {
            chars[i * 2] = (char)(b + 0x30);
         }
         b = (bArray[i]&0xff) & 0x0f;
         if (b>=0x0a) {
            chars[i * 2 + 1] = (char)(b + 0x57);
         }
         else {
            chars[i * 2 + 1] = (char)(b + 0x30);
         }
      }
      return chars;
   }
      
   public static byte[] toByteArray(char[] array) {
      int length = array.length;
      byte[] number = new byte[length/2];
      int i, radix = 16;
      for (i=0; i<length; i+=2) {
         number[i/2] = (byte)((Character.digit(array[i], radix)*radix+Character.digit(array[i+1], radix))&0xff);
      }
      return number;
   }
      
   public static void computeAndSetParityBit(byte[] input) {

      int aByte;
      for (int i=input.length-1;i>=0;i--) {
         aByte = (input[i]&0xff);
         aByte ^= aByte>>4;
         aByte &= 0x0f;
         if (((0x6996 >> aByte) & 0x01) == 1) {
            input[i] &= 0xfe;
         }
         else {
            input[i] |= 0x01;
         }
      }
   }

   public static byte[] convert8bitTo7bit(byte[] input, int offset, boolean resetParityBit) throws GeneralSecurityException{

      if (offset+7>input.length) throw new GeneralSecurityException("Not enough bytes");
      byte[] result = new byte[8];
      result[0] = input[offset];
      for (int j=1;j<7;j++) {
         result[j] = (byte)(((input[j+offset]&0xff)>>>j)|((input[j-1+offset]&0xff)<<8-j));
      }
      result[7] = (byte)((input[6+offset]&0xff)<<1);
      if (resetParityBit) {
         for (int i=0;i<result.length;i++) {
            result[i] = (byte)(result[i]&0xfe);
         }
      }
      return result;
   }

   public static byte[] convert8bitTo7bit(byte[] input, boolean resetParityBit){

      //if (input.length%7!=0) throw new RuntimeException("input array length not a multiple of 7");
      int chunksOf7 = input.length/7;
      byte[] result = new byte[chunksOf7*8];
      if (chunksOf7==1) {
         result[0] = input[0];
         for (int j=1;j<7;j++) {
            result[j] = (byte)(((input[j]&0xff)>>>j)|((input[j-1]&0xff)<<8-j));
         }
         result[7] = (byte)(input[6]<<1);
      }
      else {
         for (int i=0;i<chunksOf7;i++) {
            result[i*8] = input[i*8-i];
            for (int j=1;j<7;j++) {
               result[i*8+j] = (byte)(((input[i*8+j-i]&0x0ff)>>>j)|((input[i*8+j-1-i]&0xff)<<8-j));
            }
            result[i*8+7] = (byte)((input[i*8+6-i]&0xff)<<1);
         }
      }
      if (resetParityBit) {
         for (int i=0;i<result.length;i++) {
            result[i] = (byte)(result[i]&0xfe);
         }
      }
      return result;
   }

   /**
    * Encodes an integer into network byte order.
    * @param num
    * @param buf
    * @param offset
    * @param length
    */
   public static void getNetworkByteOrderFromInt(int num, byte[] buf, int offset, int length) {
      
      switch (length) {
         case 4:
            buf[offset] = (byte)((num & 0xff000000)>>>24);
            buf[offset+1] = (byte)((num & 0x00ff0000)>>>16);
            buf[offset+2] = (byte)((num & 0x0000ff00)>>>8);
            buf[offset+3] = (byte) (num & 0x000000ff);
            break;
         case 3:
            buf[offset] = (byte)((num & 0x00ff0000)>>>16);
            buf[offset+1] = (byte)((num & 0x0000ff00)>>>8);
            buf[offset+2] = (byte) (num & 0x000000ff);
            break;
         case 2:
            buf[0] = (byte)((num & 0x0000ff00)>>>8);
            buf[offset+1] = (byte) (num & 0x000000ff);
            break;
         case 1:
            buf[0] = (byte) (num & 0x000000ff);
            break;
         default: throw new IllegalArgumentException("A length of " + length + " is not one of 1, 2, 3, 4.");
      }
   }

   /**
    * Constructs an integer based on network byte order.
    * @param buf
    * @param offset
    * @param length
    * @return 
    */
   public static int getIntegerFromNetworkByteOrder(byte[] buf, int offset, int length) {

      switch (length) {
         case 4:
            return ((buf[offset]&0xff)<<24)| ((buf[offset+1]&0xff)<<16)| ((buf[offset+2]&0xff)<<8)| (buf[offset+3]&0xff);
         case 3:
            return ((buf[offset]&0xff)<<16)| ((buf[offset+1]&0xff)<<8)| (buf[offset+2]&0xff);
         case 2:
            return ((buf[offset]&0xff)<<8)| (buf[offset+1]&0xff);
         case 1:
            return (buf[offset]&0xff);
         default:
            return Integer.MIN_VALUE;
      }
   }

}

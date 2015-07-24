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

package com.ericdaugherty.mail.server.crypto.scrypt;

//Java imports
import java.io.*;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//Logging imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.configuration.JESVault;
import com.xlat4cast.jes.crypto.PBKDF2;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;
import com.xlat4cast.jes.crypto.mac.HMACParameterSpec;
import com.xlat4cast.jes.crypto.mac.JESMac;
import com.ericdaugherty.mail.server.errors.VaultException;
import com.ericdaugherty.mail.server.utils.IOUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Scrypt {

   /** Logger */
   private static Log log = LogFactory.getLog(Scrypt.class);
   
   private static final class Parameters {

      private byte logN;
      private int N;
      private int r;
      private int p;

      public Parameters() {
      }

      public Parameters(int N, int r, int p) {
         this.N = N;
         this.r = r;
         this.p = p;
      }
   }

   private double scryptEncCpuPerf(JESMac mac) throws GeneralSecurityException, VaultException {

      long i = 0;
      long second = 1000000000L;
      long start = System.nanoTime();
      long interval;
      byte[] salt = new byte[0];
      Parameters parameters = new Parameters(128, 1, 1);
      byte[] dk = new byte[0];
      do {
         cryptoScrypt(mac, salt, parameters, dk);
         interval = System.nanoTime() - start;
         if (interval > second) {
            break;
         }
         i += 512;
      } while (true);
      return i / (double) interval;
   }

   private void cryptoScrypt(JESMac mac, byte[] salt, Parameters parameters, byte[] dk) throws GeneralSecurityException, VaultException {

      byte[] B = new byte[128 * parameters.r * parameters.p];
      byte[] V = new byte[128 * parameters.r * parameters.N];
      byte[] XY = new byte[256 * parameters.r];

      int i;

      if (parameters.r * parameters.p >= 1 << 30) {
         throw new VaultException();
      }
      if ((parameters.N == 0) || ((parameters.N & (parameters.N - 1)) != 0)) {
         throw new VaultException();
      }
      if ((parameters.r > Integer.MAX_VALUE / 128 / parameters.p)
            || (parameters.N > Integer.MAX_VALUE / 128 / parameters.r)) {
         throw new VaultException();
      }

      PBKDF2 pbkdf2 = new PBKDF2();
      
      /* 1: (B_0 ... B_{p-1}) <-- PBKDF2(P, S, 1, p * MFLen) */
      pbkdf2.deriveKey(mac, salt, 1, B);

      for (i = 0; i < parameters.p; i++) {
         /* 3: B_i <-- MF(B_i, N) */
         smix(B, i * 128 * parameters.r, parameters.r, parameters.N, V, XY);
      }

      /* 5: DK <-- PBKDF2(P, B, 1, dkLen) */
      pbkdf2.deriveKey(mac, B, 1, dk);
      
   }
   
   private void smix(byte[] B, int bOffset, int r, int N, byte[] V, byte[] XY) {

      int i, j;

      /* 1: X <-- B */
      System.arraycopy(B, bOffset, XY, 0, 128 * r);

      /* 2: for i = 0 to N - 1 do */
      for (i = 0; i < N; i++) {
         /* 3: V_i <-- X */
         System.arraycopy(XY, 0, V, i * 128 * r, 128 * r);

         /* 4: X <-- H(X) */
         blockmixSalsa8(XY, XY, 128 * r, r);
      }
      /* 6: for i = 0 to N - 1 do */
      for (i = 0; i < N; i++) {
         /* 7: j <-- Integerify(X) mod N */
         j = integerify(XY, r) & (N - 1);

         /* 8: X <-- H(X \xor V_j) */
         blkxor(XY, 0, V, j * (128 * r), 128 * r);
         blockmixSalsa8(XY, XY, 128 * r, r);
      }

      /* 10: B' <-- X */
      System.arraycopy(XY, 0, B, bOffset, 128 * r);
   }

   public int integerify(byte[] B, int r) {
      return le32dec(B, (2 * r - 1) * 64);
   }

   private void blockmixSalsa8(byte[] B, byte[] Y, int yOffset, int r) {

      byte[] X = new byte[64];

      /* 1: X <-- B_{2r - 1} */
      System.arraycopy(B, (2 * r - 1) * 64, X, 0, 64);

      /* 2: for i = 0 to 2r - 1 do */
      for (int i = 0; i < 2 * r; i++) {

         /* 3: X <-- H(X \xor B_i) */
         blkxor(X, 0, B, i * 64, 64);
         salsa20_8(X);

         /* 4: Y_i <-- X */
         System.arraycopy(X, 0, Y, yOffset + i * 64, 64);
      }

      /* 6: B' <-- (Y_0, Y_2 ... Y_{2r-2}, Y_1, Y_3 ... Y_{2r-1}) */
      for (int i = 0; i < r; i++) {
         System.arraycopy(Y, yOffset + (i * 2) * 64, B, i * 64, 64);
      }
      for (int i = 0; i < r; i++) {
         System.arraycopy(Y, yOffset + (i * 2 + 1) * 64, B, (i + r) * 64, 64);
      }
   }

   private void blkxor(byte[] dest, int destOffset, byte[] src, int srcOffset, int len) {

      for (int i = 0; i < len; i++) {
         dest[destOffset + i] ^= src[srcOffset + i];
      }
   }

   public static int R(int a, int b) {
      return (a << b) | (a >>> (32 - b));
   }

   public int be32dec(byte[] B, int offset) {
      return (B[offset + 3] & 0xff) | (B[offset + 2] & 0xff) << 8 | (B[offset + 1] & 0xff) << 16 | (B[offset] & 0xff) << 24;
   }

   private void be32enc(byte[] B, int bOffset, int l) {

      B[bOffset + 0] = (byte) (l >> 24 & 0xff);
      B[bOffset + 1] = (byte) (l >> 16 & 0xff);
      B[bOffset + 2] = (byte) (l >> 8 & 0xff);
      B[bOffset + 3] = (byte) (l & 0xff);
   }

   public int le32dec(byte[] B, int offset) {
      return (B[offset] & 0xff) | (B[offset + 1] & 0xff) << 8 | (B[offset + 2] & 0xff) << 16 | (B[offset + 3] & 0xff) << 24;
   }

   public void le32enc(byte[] B, int bOffset, int[] x, int xPos) {
      B[bOffset + 0] = (byte) (x[xPos] & 0xff);
      B[bOffset + 1] = (byte) (x[xPos] >> 8 & 0xff);
      B[bOffset + 2] = (byte) (x[xPos] >> 16 & 0xff);
      B[bOffset + 3] = (byte) (x[xPos] >> 24 & 0xff);
   }

   private void salsa20_8(byte[] B) {

      int[] B32 = new int[16];
      int[] x = new int[16];
      int i;

      /* Convert little-endian values in. */
      for (i = 0; i < 16; i++) {
         B32[i] = le32dec(B, i * 4);
      }

      /* Compute x = doubleround^4(B32). */
      System.arraycopy(B32, 0, x, 0, 16);

      for (i = 0; i < 8; i += 2) {
         /* Operate on columns. */
         x[ 4] ^= R(x[ 0] + x[12], 7);
         x[ 8] ^= R(x[ 4] + x[ 0], 9);
         x[12] ^= R(x[ 8] + x[ 4], 13);
         x[ 0] ^= R(x[12] + x[ 8], 18);

         x[ 9] ^= R(x[ 5] + x[ 1], 7);
         x[13] ^= R(x[ 9] + x[ 5], 9);
         x[ 1] ^= R(x[13] + x[ 9], 13);
         x[ 5] ^= R(x[ 1] + x[13], 18);

         x[14] ^= R(x[10] + x[ 6], 7);
         x[ 2] ^= R(x[14] + x[10], 9);
         x[ 6] ^= R(x[ 2] + x[14], 13);
         x[10] ^= R(x[ 6] + x[ 2], 18);

         x[ 3] ^= R(x[15] + x[11], 7);
         x[ 7] ^= R(x[ 3] + x[15], 9);
         x[11] ^= R(x[ 7] + x[ 3], 13);
         x[15] ^= R(x[11] + x[ 7], 18);

         /* Operate on rows. */
         x[ 1] ^= R(x[ 0] + x[ 3], 7);
         x[ 2] ^= R(x[ 1] + x[ 0], 9);
         x[ 3] ^= R(x[ 2] + x[ 1], 13);
         x[ 0] ^= R(x[ 3] + x[ 2], 18);

         x[ 6] ^= R(x[ 5] + x[ 4], 7);
         x[ 7] ^= R(x[ 6] + x[ 5], 9);
         x[ 4] ^= R(x[ 7] + x[ 6], 13);
         x[ 5] ^= R(x[ 4] + x[ 7], 18);

         x[11] ^= R(x[10] + x[ 9], 7);
         x[ 8] ^= R(x[11] + x[10], 9);
         x[ 9] ^= R(x[ 8] + x[11], 13);
         x[10] ^= R(x[ 9] + x[ 8], 18);

         x[12] ^= R(x[15] + x[14], 7);
         x[13] ^= R(x[12] + x[15], 9);
         x[14] ^= R(x[13] + x[12], 13);
         x[15] ^= R(x[14] + x[13], 18);

      }

      /* Compute B32 = B32 + x. */
      for (i = 0; i < 16; ++i) {
         B32[i] += x[i];
      }

      /* Convert little-endian values out. */
      for (i = 0; i < 16; i++) {
         le32enc(B, 4 * i, B32, i);
      }
   }

   private long memToUse(int maxmem, double maxmemfrac) {

      long memavail = Runtime.getRuntime().freeMemory();

      if ((maxmemfrac > 0.5) || (maxmemfrac == 0.0)) {
         maxmemfrac = 0.5;
      }
      memavail = (long) (maxmemfrac * memavail);

      /* Don't use more than the specified maximum. */
      if ((maxmem > 0) && (memavail > maxmem)) {
         memavail = maxmem;
      }

      /* But always allow at least 1 MiB. */
      if (memavail < 1024 * 1204) {
         memavail = 1024 * 1024;
      }

      /* Return limit via the provided pointer. */
      return memavail;

   }

   private Parameters pickParams(String algorithm, int maxmem, double maxmemfrac, double maxtime) throws Exception {

      Parameters parameters = new Parameters();
      long memlimit = memToUse(maxmem, maxmemfrac);

      double opps;
      double opslimit;
      double maxN, maxrp;

      /* Figure out how fast the CPU is. */
      JESMac jesMac = JESMac.getInstance(algorithm);
      jesMac.init(new HMACParameterSpec());

      opps = scryptEncCpuPerf(jesMac);
      opslimit = opps * maxtime;

      /* Allow a minimum of 2^15 salsa20/8 cores. */
      if (opslimit < 32768) {
         opslimit = 32768;
      }

      /* Fix r = 8 for now. */
      parameters.r = 8;

      if (opslimit < memlimit / 32.0) {
         /* Set p = 1 and choose N based on the CPU limit. */
         parameters.p = 1;
         maxN = opslimit / (parameters.r * 4);
         for (parameters.logN = 1; parameters.logN < 63; parameters.logN++) {
            if (1 << parameters.logN > maxN / 2) {
               break;
            }
         }
      } else {
         /* Set N based on the memory limit. */
         maxN = memlimit / (parameters.r * 128.0);
         for (parameters.logN = 1; parameters.logN < 63; parameters.logN++) {
            if (1 << parameters.logN > maxN / 2) {
               break;
            }
         }

         /* Choose p based on the CPU limit. */
         maxrp = (opslimit / 4) / (1 << parameters.logN);
         if (maxrp > 0x3fffffff) {
            maxrp = 0x3fffffff;
         }
         parameters.p = (int) (maxrp) / parameters.r;
      }
      parameters.N = 1 << parameters.logN;

      return parameters;
   }

   private void checkParams(String algorithm, int maxmem, double maxmemfrac, double maxtime, Parameters parameters) throws GeneralSecurityException, VaultException {

      long memlimit = memToUse(maxmem, maxmemfrac);

      double opps;
      double opslimit;

      /* Figure out how fast the CPU is. */
      JESMac jesMac = JESMac.getInstance(algorithm);
      jesMac.init(new HMACParameterSpec());

      opps = scryptEncCpuPerf(jesMac);
      opslimit = opps * maxtime;

      if (opslimit < 32768) {
         opslimit = 32768;
      }

      if (parameters.logN < 1 || parameters.logN > 63) {
         throw new VaultException();
      }
      if (parameters.r * parameters.p > 0x40000000) {
         throw new VaultException();
      }
      if ((memlimit / parameters.N) / parameters.r < 128) {
         throw new VaultException();
      }
      if ((opslimit / parameters.N) / (parameters.r * parameters.p) < 4) {
         throw new VaultException();
      }
   }

   private void encodeSetup(JESMac jesMac, Algorithm algorithm, boolean limitedCryptography, byte[] header, byte[] dk, int maxmem, double maxmemfrac, double maxtime) throws GeneralSecurityException, Exception {

      byte[] salt = new byte[32];

      Parameters parameters = pickParams(jesMac.getAlgorithm(), maxmem, maxmemfrac, maxtime);

      SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
      sr.nextBytes(salt);

      cryptoScrypt(jesMac, salt, parameters, dk);

      System.arraycopy("scrypt".getBytes(), 0, header, 0, 6);
      header[6] = (byte) (algorithm.getHMACAlgorithmCode() | (limitedCryptography ? 0x40 : 0x00));
      header[7] = parameters.logN;
      be32enc(header, 8, parameters.r);
      be32enc(header, 12, parameters.p);
      System.arraycopy(salt, 0, header, 16, 32);

      MessageDigest jesMessageDigest = JESMessageDigest.getInstance(jesMac.getAlgorithm().substring(4));
      jesMessageDigest.update(header, 0, 48);
      byte[] hbuf = jesMessageDigest.digest();
      System.arraycopy(hbuf, 0, header, 48, 16);

      int macLength = jesMac.getMacLength();
      byte[] keyHMAC = new byte[macLength];
      System.arraycopy(dk, limitedCryptography ? 16 : 32, keyHMAC, 0, macLength);
      jesMac.init(new SecretKeySpec(keyHMAC, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      jesMac.update(header, 0, 64);
      hbuf = jesMac.doFinal();
      System.arraycopy(hbuf, 0, header, 64, macLength);
   }

   private Parameters decodeSetup(JESMac jesMac, Algorithm algorithm, boolean limitedCryptography, byte[] header, byte[] dk, int maxmem, double maxmemfrac, double maxtime) throws GeneralSecurityException, VaultException {

      byte[] salt = new byte[32];

      Parameters parameters = new Parameters();

      parameters.logN = header[7];
      parameters.r = be32dec(header, 8);
      parameters.p = be32dec(header, 12);
      parameters.N = 1 << parameters.logN;
      System.arraycopy(header, 16, salt, 0, 32);

      MessageDigest jesMessageDigest = JESMessageDigest.getInstance(jesMac.getAlgorithm().substring(4));
      jesMessageDigest.update(header, 0, 48);
      byte[] hbuf = jesMessageDigest.digest();
      byte[] computedDigest = new byte[16];
      System.arraycopy(hbuf, 0, computedDigest, 0, 16);
      byte[] storedDigest = new byte[16];
      System.arraycopy(header, 48, storedDigest, 0, 16);
      if (!Arrays.equals(storedDigest, computedDigest)) {
         throw new VaultException("Input is not a valid scrypt-encrypted block");
      }
      checkParams(jesMac.getAlgorithm(), maxmem, maxmemfrac, maxtime, parameters);

      cryptoScrypt(jesMac, salt, parameters, dk);

      int macLength = jesMac.getMacLength();
      byte[] keyHMAC = new byte[macLength];
      System.arraycopy(dk, limitedCryptography ? 16 : 32, keyHMAC, 0, macLength);
      jesMac.init(new SecretKeySpec(keyHMAC, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      jesMac.update(header, 0, 64);
      byte[] computedHMAC = jesMac.doFinal();
      byte[] storedHMAC = new byte[macLength];
      System.arraycopy(header, 64, storedHMAC, 0, macLength);
      if (!Arrays.equals(storedHMAC, computedHMAC)) {
         throw new VaultException("Passphrase is incorrect");
      }

      return parameters;
   }

   public byte[] encodeVault(JESVault jesVault, byte[] passwd, Algorithm algorithm, boolean limitedCryptography, int maxmem, double maxmemfrac, double maxtime) throws GeneralSecurityException, Exception {

      JESMac jesMac = JESMac.getInstance(algorithm.getHMACAlgorithm());

      int macLength = jesMac.getMacLength();
      byte[] header = new byte[64 + macLength];
      byte[] dk = new byte[(limitedCryptography ? 16 : 32) + macLength];

      jesMac.init(new SecretKeySpec(passwd, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      encodeSetup(jesMac, algorithm, limitedCryptography, header, dk, maxmem, maxmemfrac, maxtime);

      byte[] keyAES = new byte[(limitedCryptography ? 16 : 32)];
      byte[] keyHMAC = new byte[macLength];
      System.arraycopy(dk, 0, keyAES, 0, (limitedCryptography ? 16 : 32));
      System.arraycopy(dk, (limitedCryptography ? 16 : 32), keyHMAC, 0, macLength);

      jesMac.init(new SecretKeySpec(keyHMAC, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      jesMac.update(header, 0, 64 + macLength);

      SecretKeySpec key = new SecretKeySpec(keyAES, "AES");
      byte[] ivBytes = new byte[16];
      System.arraycopy(header, 64, ivBytes, 0, 16);
      IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

      Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(jesVault);

         byte[] buffer = baos.toByteArray();
         
         int offset = 64 + macLength;
         int length = buffer.length;
         
         byte[] output = new byte[offset + length + macLength];
         System.arraycopy(header, 0, output, 0, offset);

         ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
         CipherInputStream cis = new CipherInputStream(bais, cipher);
         
         try {
            int bufferBlockLength = 512;
            int readCount;
            int totalRead = 0;
            do {
               readCount = cis.read(output, offset + totalRead, bufferBlockLength);
               if (readCount==-1) break;
               jesMac.update(output, offset + totalRead, readCount);
               totalRead += readCount;
            }while(true);
         } finally {
            IOUtils.close(cis);
         }
         byte[] mac = jesMac.doFinal();
         
         System.arraycopy(mac, 0, output, offset + length, macLength);
         return output;
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw e;
      }
   }

   public JESVault decodeVault(byte[] input, byte[] passwd, boolean limitedCryptography, int maxmem, double maxmemfrac, double maxtime) throws GeneralSecurityException, VaultException {

      if (!new String(input, 0, 6).equals("scrypt")) {
         throw new VaultException("Not a scrypt file");
      }
      Algorithm algorithm = Algorithm.getAlgorithm(input[6] & 0x3F);
      boolean limitedCryptographyPersisted = (input[6] & 0x40) == 0x40;

      JESMac jesMac = JESMac.getInstance(algorithm.getHMACAlgorithm());
      
      if ((limitedCryptography!=limitedCryptographyPersisted)) {
         if (!limitedCryptographyPersisted) {
            if (Cipher.getMaxAllowedKeyLength("AES")==Integer.MAX_VALUE) {
               //Override option, switch to unlimited cryptography
               limitedCryptography = false;
            }
            else {
               throw new VaultException("Unable to decode vault, unlimited cryptography required.");
            }
         }
         else {
            limitedCryptography = limitedCryptographyPersisted;
            if (log.isDebugEnabled()) {
               log.debug("Vault created with limited cryptography.");
            }
         }
      }
      
      int macLength = jesMac.getMacLength();
      byte[] header = new byte[64 + macLength];
      System.arraycopy(input, 0, header, 0, 64 + macLength);
      byte[] dk = new byte[(limitedCryptography ? 16 : 32) + macLength];

      jesMac.init(new SecretKeySpec(passwd, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      decodeSetup(jesMac, algorithm, limitedCryptography, header, dk, maxmem, maxmemfrac, maxtime);

      byte[] keyAES = new byte[(limitedCryptography ? 16 : 32)];
      byte[] keyHMAC = new byte[macLength];
      System.arraycopy(dk, 0, keyAES, 0, (limitedCryptography ? 16 : 32));
      System.arraycopy(dk, (limitedCryptography ? 16 : 32), keyHMAC, 0, macLength);

      jesMac.init(new SecretKeySpec(keyHMAC, algorithm.getHMACAlgorithm()), new HMACParameterSpec());
      jesMac.update(header, 0, 64 + macLength);

      SecretKeySpec key = new SecretKeySpec(keyAES, "AES");
      byte[] ivBytes = new byte[16];
      System.arraycopy(header, 64, ivBytes, 0, 16);
      IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

      Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

      CipherOutputStream cos = null;
      ObjectInputStream ois = null;
      try {
         int offset = 64 + macLength;
         int length = input.length - 64 - macLength - 32;
         
         ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
         cos = new CipherOutputStream(baos, cipher);

         int bufferBlockLength = 8192;
         int fullBlocks = length / bufferBlockLength;
         boolean hasPartial = length % bufferBlockLength != 0;
         for (int i = 0; i < fullBlocks; i++) {

            jesMac.update(input, offset + i * bufferBlockLength, bufferBlockLength);
            cos.write(input, offset + i * bufferBlockLength, bufferBlockLength);
         }
         if (hasPartial) {

            bufferBlockLength = length % bufferBlockLength;
            jesMac.update(input, offset + fullBlocks * 8192, bufferBlockLength);
            cos.write(input, offset + fullBlocks * 8192, bufferBlockLength);
         }
         
         byte[] computedHMAC = jesMac.doFinal();
         byte[] storedHMAC = new byte[macLength];
         System.arraycopy(input, input.length - 32, storedHMAC, 0, macLength);
         if (!Arrays.equals(storedHMAC, computedHMAC)) {
            throw new VaultException("Input is not a valid scrypt-encrypted block");
         }
         
         ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
         ois = new ObjectInputStream(bais);
         JESVault jesVault = (JESVault) ois.readObject();

         return jesVault;
      }
      catch (ClassNotFoundException cnfe) {
         log.error(cnfe.getLocalizedMessage());
         throw new RuntimeException(cnfe.getLocalizedMessage());
      }
      catch (IOException ioe) {
         log.error(ioe.getLocalizedMessage());
         throw new RuntimeException(ioe.getLocalizedMessage());
      }
      finally {
         IOUtils.close(cos);
         IOUtils.close(ois);
      }
   }

   public enum Algorithm {

      SHA256, SHA384, SHA512;

      public String getHMACAlgorithm() {
         switch (this) {
            case SHA384:
               return "HmacSHA-384";
            case SHA512:
               return "HmacSHA-512";
            default:
               return "HmacSHA-256";
         }
      }

      byte getHMACAlgorithmCode() {
         switch (this) {
            case SHA384:
               return 1;
            case SHA512:
               return 2;
            default:
               return 0;
         }
      }

      static Algorithm getAlgorithm(int code) throws VaultException {
         switch (code) {
            case 0:
               return SHA256;
            case 1:
               return SHA384;
            case 2:
               return SHA512;
            default:
               throw new VaultException("Unsupported HMAC function");
         }
      }
   }
}
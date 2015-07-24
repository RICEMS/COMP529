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

package com.ericdaugherty.mail.server.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Saslprep {
   
   public enum StringType {
      STORED_STRING, QUERY
   };
   
   public static char[] prepareString(char[] input, boolean checkBidi, StringType stringType, boolean includeUnassigned) throws SaslprepException{
      
      if (input == null)
         return null;
      if (input.length == 0)
         return input;
      
      if (stringType==StringType.STORED_STRING&&includeUnassigned) {
         throw new SaslprepException("Stored strings using the profile MUST NOT "
               + "contain any unassigned code points.");
      }
      
      int length;
      StringBuilder sb = new StringBuilder(input.length);
      for (char c : input) {
         
         if (c12Entries.contains(c)) {
            sb.append(' ');
         }
         else if (!contains(b1Entries,c)) {
            sb.append(c);
         }
      }
      
      char[] ni = java.text.Normalizer.normalize(sb, java.text.Normalizer.Form.NFKC).toCharArray();
      
      length = ni.length;
      for (int i=0;i<length;i++) {
         if (c12Entries.contains(ni[i])) throw new SaslprepException("Prohibited output: Non-ASCII space characters");
         if (c21Entries.contains(ni[i])) throw new SaslprepException("Prohibited output: ASCII control characters");
         if (c22Entries.contains(ni[i])) throw new SaslprepException("Prohibited output: Non-ASCII control characters");
         if (contains( c3Entries,ni[i])) throw new SaslprepException("Prohibited output: Private Use characters");
         if (contains( c4Entries,ni[i])) throw new SaslprepException("Prohibited output: Non-character code points");
         if (contains( c6Entries,ni[i])) throw new SaslprepException("Prohibited output: Inappropriate for plain text characters");
         if (contains( c7Entries,ni[i])) throw new SaslprepException("Prohibited output: Inappropriate for canonical representation characters");
         if (contains( c8Entries,ni[i])) throw new SaslprepException("Prohibited output: Change display properties or deprecated characters");
         if (contains( c9Entries,ni[i])) throw new SaslprepException("Prohibited output: Tagging characters");
         if (isHighSurrogate((int)ni[i])) {
            if (i+1==length) throw new SaslprepException("Malformed supplementary code");
            if (!isLowSurrogate((int)ni[i+1])) throw new SaslprepException("Malformed supplementary code");
            if (contains(c22EntriesS,Character.codePointAt(ni, i))) throw new SaslprepException("Prohibited output: Non-ASCII control characters");
            if (contains( c3EntriesS,Character.codePointAt(ni, i))) throw new SaslprepException("Prohibited output: Private Use characters");
            if (contains( c4EntriesS,Character.codePointAt(ni, i))) throw new SaslprepException("Prohibited output: Non-character code points");
            if (contains( c9EntriesS,Character.codePointAt(ni, i))) throw new SaslprepException("Prohibited output: Tagging characters");
            i++;
         }
      }
      
      char[] npo = ni;
      length = npo.length;
      
      if (checkBidi) {
         
         if (!(length==1||(length==2&&Character.codePointAt(npo, 0)>65535))) {

            boolean anyRandALCat, anyLCat, firstCatIsRandL, lastCatIsRandL = false, lastCharALowSurrogate;
            int firstChar, lastChar;
            int codePoint;
            firstChar = Character.codePointAt(npo, 0);
            anyRandALCat = contains(d1,firstChar)||contains(d1S,firstChar);
            firstCatIsRandL = anyRandALCat;
            anyLCat = contains(d2,firstChar)||contains(d2S,firstChar);

            lastCharALowSurrogate = isLowSurrogate(Character.codePointAt(npo, length-1));
            lastChar = Character.codePointAt(npo, length-(lastCharALowSurrogate?2:1));
            if (contains(d1,lastChar)||contains(d1S,lastChar)) {
               anyRandALCat = true;
               lastCatIsRandL = anyRandALCat;
            }
            if (contains(d2,lastChar)||contains(d2S,lastChar)) {
               anyLCat = true;
            }
            if (anyRandALCat&&anyLCat) throw new SaslprepException("If a string "
                  + "contains any RandALCat character, the string MUST NOT contain any LCat character");
            if ((firstCatIsRandL&&!lastCatIsRandL)||(!firstCatIsRandL&&lastCatIsRandL)) {
               throw new SaslprepException("If a string contains any RandALCat "
                     + "character, a RandALCat character MUST be the first character "
                     + "of the string, and a RandALCat character MUST be the last "
                     + "character of the string");
            }

            for (int i=(Character.codePointAt(npo, 0)>65535?2:1);i<length-(lastCharALowSurrogate?2:1);i++) {

               codePoint = Character.codePointAt(npo, i);
               if (contains(d1,codePoint)||contains(d1S,codePoint)) {
                  anyRandALCat = true;
               }
               if (contains(d2,codePoint)||contains(d2S,codePoint)) {
                  anyLCat = true;
               }
               if (anyRandALCat){
                  if (anyLCat) throw new SaslprepException("If a string contains "
                        + "any RandALCat character, the string MUST NOT contain any LCat character");
                  if (!(firstCatIsRandL&&lastCatIsRandL)) {
                     throw new SaslprepException("If a string contains any RandALCat "
                           + "character, a RandALCat character MUST be the first character "
                           + "of the string, and a RandALCat character MUST be the last "
                           + "character of the string");
                  }
               }

               if (isHighSurrogate((int)npo[i])) i++;
            }
         }
         
      }
      
      if (stringType==StringType.QUERY&&includeUnassigned) return npo;
      
      sb = new StringBuilder(npo.length);
      length = length - (isLowSurrogate(Character.codePointAt(npo, length-1))?1:0);
      for (int i=0;i<length;i++) {
         
         int codePoint = Character.codePointAt(npo, i);
         if (!(contains(a1,codePoint)||contains(a1S,codePoint))) {
            sb.append(Character.toChars(Character.codePointAt(npo, i)));
         }
         if (isHighSurrogate((int)npo[i])) i++;
      }
      
      char[] output = new char[sb.length()];
      sb.getChars(0, sb.length(), output, 0);
      return output;
   }
   
   private static boolean contains(int[] map, int entry) {
      
      for (int c:map) {
         if (c==entry) return true;
      }
      return false;
   }
   
   public static boolean contains(char[] map, char entry) {
      
      for (char c:map) {
         if (c==entry) return true;
      }
      return false;
   }
   
   private static boolean contains(int[][] map, int entry) {
      
      for (int[] i:map) {
         if (entry>=i[0]&&entry<=i[1]) return true;
      }
      return false;
   }
   
   private static boolean contains(char[][] map, char entry) {
      
      for (char[] i:map) {
         if (entry>=i[0]&&entry<=i[1]) return true;
      }
      return false;
   }
   
   private static boolean isHighSurrogate(int codePoint) {
      
      return codePoint>=55296&&codePoint<=56319;
   }
   
   private static boolean  isLowSurrogate(int codePoint) {
      
      return codePoint >= 56320 && codePoint <= 57343;
   }
   
   public static final Set<Character> c12Entries = Collections.unmodifiableSet(new LinkedHashSet<Character>(Arrays.asList(
                                              new Character[]{0x00a0,0x1680,0x2000,0x2001,
                                                         0x2002,0x2003,0x2004,0x2005,
                                                         0x2006,0x2007,0x2008,0x2009,
                                                         0x200a,0x200b,0x202f,0x2057,
                                                         0x3000})));

   public static final Set<Character> c21Entries = Collections.unmodifiableSet(new LinkedHashSet<Character>(Arrays.asList(
                                              new Character[]{0x0000,0x0001,0x0002,0x0003,
                                                         0x0004,0x0005,0x0006,0x0007,
                                                         0x0008,0x0009,0x000a,0x000b,
                                                         0x000c,0x000d,0x000e,0x000f,
                                                         0x0010,0x0011,0x0012,0x0013,
                                                         0x0014,0x0015,0x0016,0x0017,
                                                         0x0018,0x0019,0x001a,0x001b,
                                                         0x001c,0x001d,0x001e,0x001f,
                                                         0x007F})));
   
   public static final Set<Character> c22Entries = Collections.unmodifiableSet(new LinkedHashSet<Character>(Arrays.asList(
                                              new Character[]{0x0080,0x0081,0x0082,0x0083,
                                                         0x0084,0x0085,0x0086,0x0087,
                                                         0x0088,0x0089,0x008a,0x008b,
                                                         0x008c,0x008d,0x008e,0x008f,
                                                         0x0090,0x0091,0x0092,0x0093,
                                                         0x0094,0x0095,0x0096,0x0097,
                                                         0x0098,0x0099,0x009a,0x009b,
                                                         0x009c,0x009d,0x009e,0x009f,
                                                         0x06dd,0x070e,0x180e,0x200c,
                                                         0x200d,0x2028,0x2029,0x2060,
                                                         0x2061,0x2062,0x2063,0x206a,
                                                         0x206b,0x206c,0x206d,0x206e,
                                                         0x206f,0xfeff,0xfff9,0xfffa,
                                                         0xfffb,0xfffc})));
   
   private static final int[][] c22EntriesS =   new int[][]{{119155,119162}};
   
   private static final char[][] c3Entries =   new char[][]{{0xe000,0xf8ff}};
   
   private static final int[][] c3EntriesS =    new int[][]{{983040,1048573},{1048576,1114109}};
   
   private static final char[][] c4Entries =   new char[][]{{0xfdd0,0xfdef},{0xfffe,0xffff}};
   
   private static final int[][] c4EntriesS =    new int[][]{{131070,131071},
                                                            {196606,196607},
                                                            {262142,262143},
                                                            {327678,327679},
                                                            {393214,393215},
                                                            {458750,458751},
                                                            {524286,524287},
                                                            {589822,589823},
                                                            {655358,655359},
                                                            {720894,720895},
                                                            {786430,786431},
                                                            {851966,851967},
                                                            {917502,917503},
                                                            {983038,983039},
                                                            {1048574,1048575},
                                                            {1114110,1114111}};
   
   private static final char[] c6Entries =        new char[]{0xfff9,0xfffa,0xfffb,0xfffc,0xfffd};
   
   private static final char[][] c7Entries =   new char[][]{{0x2ff0,0x2ffb}};
   
   private static final char[] c8Entries =        new char[]{0x0340,0x0341,0x200e,0x200f,0x202a,
                                                             0x202b,0x202c,0x202d,0x202e,0x206a,
                                                             0x206b,0x206c,0x206d,0x206e,0x206f};
   
   private static final char[] c9Entries =    new char[]{0xe001};

   private static final int[][] c9EntriesS =new int[][]{{917536,917631}};
   
   private static final char[] b1Entries =    new char[]{0x00ad,0x034f,0x1806,0x180b,
                                                         0x180c,0x180d,0x200b,0x200c,
                                                         0x200d,0x2060,0xfe00,0xfe01,
                                                         0xfe02,0xfe03,0xfe04,0xfe05,
                                                         0xfe06,0xfe07,0xfe08,0xfe09,
                                                         0xfe0a,0xfe0b,0xfe0c,0xfe0d,
                                                         0xfe0e,0xfe0f,0xfeff};
   
   /*
    * Retreived from the UnicodeData.txt file of Unicode version 3.2.0
    */
   private static final int[] d1 =    new int[]{1470,
								1472,
								1475,
								1563,
								1567,
								1757,
								1808,
								1969,
								8207,
								64285,
								64318};
   
   /*
    * Retreived from the UnicodeData.txt file of Unicode version 3.2.0
    */
   private static final int[][] d1S =     new int[][]{{1488,1514},
									{1520,1524},
									{1569,1594},
									{1600,1610},
									{1645,1647},
									{1649,1749},
									{1765,1766},
									{1786,1790},
									{1792,1805},
									{1810,1836},
									{1920,1957},
									{64287,64296},
									{64298,64310},
									{64312,64316},
									{64320,64321},
									{64323,64324},
									{64326,64433},
									{64467,64829},
									{64848,64911},
									{64914,64967},
									{65008,65020},
									{65136,65140},
									{65142,65276}};
   
   /*
    * Retreived from the UnicodeData.txt file of Unicode version 3.2.0
    */
   private static final int[] d2 =    new int[]{170,
								181,
								186,
								750,
								890,
								902,
								908,
								1417,
								2307,
								2384,
								2482,
								2519,
								2654,
								2691,
								2701,
								2761,
								2768,
								2784,
								2880,
								2903,
								2947,
								2972,
								3031,
								3262,
								3294,
								3415,
								3517,
								3716,
								3722,
								3725,
								3749,
								3751,
								3773,
								3782,
								3894,
								3896,
								3967,
								3973,
								4047,
								4140,
								4145,
								4152,
								4347,
								4680,
								4696,
								4744,
								4784,
								4800,
								4880,
								6108,
								8025,
								8027,
								8029,
								8126,
								8206,
								8305,
								8319,
								8450,
								8455,
								8469,
								8484,
								8486,
								8488,
								9109,
								13312,
								19893,
								19968,
								40869,
								44032,
								55203,
								55296,
								119970,
								119995,
								120134,
								131072,
								173782,
								983040,
								1048573,
								1048576,
								1114109};
   
   /*
    * Retreived from the UnicodeData.txt file of Unicode version 3.2.0
    */
   private static final int[][] d2S =     new int[][]{{65,90},
									{97,122},
									{192,214},
									{216,246},
									{248,544},
									{546,563},
									{592,685},
									{688,696},
									{699,705},
									{720,721},
									{736,740},
									{904,906},
									{910,929},
									{931,974},
									{976,1013},
									{1024,1154},
									{1162,1230},
									{1232,1269},
									{1272,1273},
									{1280,1295},
									{1329,1366},
									{1369,1375},
									{1377,1415},
									{2309,2361},
									{2365,2368},
									{2377,2380},
									{2392,2401},
									{2404,2416},
									{2434,2435},
									{2437,2444},
									{2447,2448},
									{2451,2472},
									{2474,2480},
									{2486,2489},
									{2494,2496},
									{2503,2504},
									{2507,2508},
									{2524,2525},
									{2527,2529},
									{2534,2545},
									{2548,2554},
									{2565,2570},
									{2575,2576},
									{2579,2600},
									{2602,2608},
									{2610,2611},
									{2613,2614},
									{2616,2617},
									{2622,2624},
									{2649,2652},
									{2662,2671},
									{2674,2676},
									{2693,2699},
									{2703,2705},
									{2707,2728},
									{2730,2736},
									{2738,2739},
									{2741,2745},
									{2749,2752},
									{2763,2764},
									{2790,2799},
									{2818,2819},
									{2821,2828},
									{2831,2832},
									{2835,2856},
									{2858,2864},
									{2866,2867},
									{2870,2873},
									{2877,2878},
									{2887,2888},
									{2891,2892},
									{2908,2909},
									{2911,2913},
									{2918,2928},
									{2949,2954},
									{2958,2960},
									{2962,2965},
									{2969,2970},
									{2974,2975},
									{2979,2980},
									{2984,2986},
									{2990,2997},
									{2999,3001},
									{3006,3007},
									{3009,3010},
									{3014,3016},
									{3018,3020},
									{3047,3058},
									{3073,3075},
									{3077,3084},
									{3086,3088},
									{3090,3112},
									{3114,3123},
									{3125,3129},
									{3137,3140},
									{3168,3169},
									{3174,3183},
									{3202,3203},
									{3205,3212},
									{3214,3216},
									{3218,3240},
									{3242,3251},
									{3253,3257},
									{3264,3268},
									{3271,3272},
									{3274,3275},
									{3285,3286},
									{3296,3297},
									{3302,3311},
									{3330,3331},
									{3333,3340},
									{3342,3344},
									{3346,3368},
									{3370,3385},
									{3390,3392},
									{3398,3400},
									{3402,3404},
									{3424,3425},
									{3430,3439},
									{3458,3459},
									{3461,3478},
									{3482,3505},
									{3507,3515},
									{3520,3526},
									{3535,3537},
									{3544,3551},
									{3570,3572},
									{3585,3632},
									{3634,3635},
									{3648,3654},
									{3663,3675},
									{3713,3714},
									{3719,3720},
									{3732,3735},
									{3737,3743},
									{3745,3747},
									{3754,3755},
									{3757,3760},
									{3762,3763},
									{3776,3780},
									{3792,3801},
									{3804,3805},
									{3840,3863},
									{3866,3892},
									{3902,3911},
									{3913,3946},
									{3976,3979},
									{4030,4037},
									{4039,4044},
									{4096,4129},
									{4131,4135},
									{4137,4138},
									{4160,4183},
									{4256,4293},
									{4304,4344},
									{4352,4441},
									{4447,4514},
									{4520,4601},
									{4608,4614},
									{4616,4678},
									{4682,4685},
									{4688,4694},
									{4698,4701},
									{4704,4742},
									{4746,4749},
									{4752,4782},
									{4786,4789},
									{4792,4798},
									{4802,4805},
									{4808,4814},
									{4816,4822},
									{4824,4846},
									{4848,4878},
									{4882,4885},
									{4888,4894},
									{4896,4934},
									{4936,4954},
									{4961,4988},
									{5024,5108},
									{5121,5750},
									{5761,5786},
									{5792,5872},
									{5888,5900},
									{5902,5905},
									{5920,5937},
									{5941,5942},
									{5952,5969},
									{5984,5996},
									{5998,6000},
									{6016,6070},
									{6078,6085},
									{6087,6088},
									{6100,6106},
									{6112,6121},
									{6160,6169},
									{6176,6263},
									{6272,6312},
									{7680,7835},
									{7840,7929},
									{7936,7957},
									{7960,7965},
									{7968,8005},
									{8008,8013},
									{8016,8023},
									{8031,8061},
									{8064,8116},
									{8118,8124},
									{8130,8132},
									{8134,8140},
									{8144,8147},
									{8150,8155},
									{8160,8172},
									{8178,8180},
									{8182,8188},
									{8458,8467},
									{8473,8477},
									{8490,8493},
									{8495,8497},
									{8499,8505},
									{8509,8511},
									{8517,8521},
									{8544,8579},
									{9014,9082},
									{9372,9449},
									{12293,12295},
									{12321,12329},
									{12337,12341},
									{12344,12348},
									{12353,12438},
									{12445,12447},
									{12449,12538},
									{12540,12543},
									{12549,12588},
									{12593,12686},
									{12688,12727},
									{12784,12828},
									{12832,12867},
									{12896,12923},
									{12927,12976},
									{12992,13003},
									{13008,13054},
									{13056,13174},
									{13179,13277},
									{13280,13310},
									{40960,42124},
									{56191,56192},
									{56319,56320},
									{57343,57344},
									{63743,64045},
									{64048,64106},
									{64256,64262},
									{64275,64279},
									{65313,65338},
									{65345,65370},
									{65382,65470},
									{65474,65479},
									{65482,65487},
									{65490,65495},
									{65498,65500},
									{66304,66334},
									{66336,66339},
									{66352,66378},
									{66560,66597},
									{66600,66637},
									{118784,119029},
									{119040,119078},
									{119082,119142},
									{119146,119154},
									{119171,119172},
									{119180,119209},
									{119214,119261},
									{119808,119892},
									{119894,119964},
									{119966,119967},
									{119973,119974},
									{119977,119980},
									{119982,119993},
									{119997,120000},
									{120002,120003},
									{120005,120069},
									{120071,120074},
									{120077,120084},
									{120086,120092},
									{120094,120121},
									{120123,120126},
									{120128,120132},
									{120138,120144},
									{120146,120483},
									{120488,120777},
									{194560,195101}};
   
   private static final int[] a1 =    new int[]{545,
								907,
								909,
								930,
								975,
								1159,
								1231,
								1376,
								1416,
								1442,
								1466,
								1568,
								1791,
								1806,
								2308,
								2436,
								2473,
								2481,
								2493,
								2526,
								2601,
								2609,
								2612,
								2615,
								2621,
								2653,
								2692,
								2700,
								2702,
								2706,
								2729,
								2737,
								2740,
								2758,
								2762,
								2820,
								2857,
								2865,
								2910,
								2948,
								2961,
								2971,
								2973,
								2998,
								3017,
								3076,
								3085,
								3089,
								3113,
								3124,
								3141,
								3145,
								3204,
								3213,
								3217,
								3241,
								3252,
								3269,
								3273,
								3295,
								3332,
								3341,
								3345,
								3369,
								3401,
								3460,
								3506,
								3516,
								3541,
								3543,
								3715,
								3721,
								3736,
								3744,
								3748,
								3750,
								3756,
								3770,
								3781,
								3783,
								3912,
								3992,
								4029,
								4130,
								4136,
								4139,
								4615,
								4679,
								4681,
								4695,
								4697,
								4743,
								4745,
								4783,
								4785,
								4799,
								4801,
								4815,
								4823,
								4847,
								4879,
								4881,
								4895,
								4935,
								5901,
								5997,
								6001,
								6159,
								8024,
								8026,
								8028,
								8030,
								8117,
								8133,
								8156,
								8181,
								8191,
								9471,
								9752,
								9989,
								10024,
								10060,
								10062,
								10071,
								10160,
								11930,
								12352,
								12687,
								13055,
								13311,
								64311,
								64317,
								64319,
								64322,
								64325,
								65107,
								65127,
								65141,
								65280,
								65511,
								66335,
								119893,
								119965,
								119981,
								119994,
								119996,
								120001,
								120004,
								120070,
								120085,
								120093,
								120122,
								120127,
								120133,
								120145,
								917504};
   
   private static final int[][] a1S =     new int[][]{{564,591},
									{686,687},
									{751,767},
									{848,863},
									{880,883},
									{886,889},
									{891,893},
									{895,899},
									{1015,1023},
									{1270,1271},
									{1274,1279},
									{1296,1328},
									{1367,1368},
									{1419,1424},
									{1477,1487},
									{1515,1519},
									{1525,1547},
									{1549,1562},
									{1564,1566},
									{1595,1599},
									{1622,1631},
									{1774,1775},
									{1837,1839},
									{1867,1919},
									{1970,2304},
									{2362,2363},
									{2382,2383},
									{2389,2391},
									{2417,2432},
									{2445,2446},
									{2449,2450},
									{2483,2485},
									{2490,2491},
									{2501,2502},
									{2505,2506},
									{2510,2518},
									{2520,2523},
									{2532,2533},
									{2555,2561},
									{2563,2564},
									{2571,2574},
									{2577,2578},
									{2618,2619},
									{2627,2630},
									{2633,2634},
									{2638,2648},
									{2655,2661},
									{2677,2688},
									{2746,2747},
									{2766,2767},
									{2769,2783},
									{2785,2789},
									{2800,2816},
									{2829,2830},
									{2833,2834},
									{2868,2869},
									{2874,2875},
									{2884,2886},
									{2889,2890},
									{2894,2901},
									{2904,2907},
									{2914,2917},
									{2929,2945},
									{2955,2957},
									{2966,2968},
									{2976,2978},
									{2981,2983},
									{2987,2989},
									{3002,3005},
									{3011,3013},
									{3022,3030},
									{3032,3046},
									{3059,3072},
									{3130,3133},
									{3150,3156},
									{3159,3167},
									{3170,3173},
									{3184,3201},
									{3258,3261},
									{3278,3284},
									{3287,3293},
									{3298,3301},
									{3312,3329},
									{3386,3389},
									{3396,3397},
									{3406,3414},
									{3416,3423},
									{3426,3429},
									{3440,3457},
									{3479,3481},
									{3518,3519},
									{3527,3529},
									{3531,3534},
									{3552,3569},
									{3573,3584},
									{3643,3646},
									{3676,3712},
									{3717,3718},
									{3723,3724},
									{3726,3731},
									{3752,3753},
									{3774,3775},
									{3790,3791},
									{3802,3803},
									{3806,3839},
									{3947,3952},
									{3980,3983},
									{4045,4046},
									{4048,4095},
									{4147,4149},
									{4154,4159},
									{4186,4255},
									{4294,4303},
									{4345,4346},
									{4348,4351},
									{4442,4446},
									{4515,4519},
									{4602,4607},
									{4686,4687},
									{4702,4703},
									{4750,4751},
									{4790,4791},
									{4806,4807},
									{4886,4887},
									{4955,4960},
									{4989,5023},
									{5109,5120},
									{5751,5759},
									{5789,5791},
									{5873,5887},
									{5909,5919},
									{5943,5951},
									{5972,5983},
									{6004,6015},
									{6109,6111},
									{6122,6143},
									{6170,6175},
									{6264,6271},
									{6314,7679},
									{7836,7839},
									{7930,7935},
									{7958,7959},
									{7966,7967},
									{8006,8007},
									{8014,8015},
									{8062,8063},
									{8148,8149},
									{8176,8177},
									{8275,8278},
									{8280,8286},
									{8292,8297},
									{8306,8307},
									{8335,8351},
									{8370,8399},
									{8427,8447},
									{8507,8508},
									{8524,8530},
									{8580,8591},
									{9167,9215},
									{9255,9279},
									{9291,9311},
									{9748,9749},
									{9854,9855},
									{9866,9984},
									{9994,9995},
									{10067,10069},
									{10079,10080},
									{10133,10135},
									{10175,10191},
									{10220,10223},
									{11008,11903},
									{12020,12031},
									{12246,12271},
									{12284,12287},
									{12439,12440},
									{12544,12548},
									{12589,12592},
									{12728,12783},
									{12829,12831},
									{12868,12880},
									{12924,12926},
									{13004,13007},
									{13175,13178},
									{13278,13279},
									{19894,19967},
									{40870,40959},
									{42125,42127},
									{42183,44031},
									{55204,55295},
									{64046,64047},
									{64107,64255},
									{64263,64274},
									{64280,64284},
									{64434,64466},
									{64832,64847},
									{64912,64913},
									{64968,64975},
									{65021,65023},
									{65040,65055},
									{65060,65071},
									{65095,65096},
									{65132,65135},
									{65277,65278},
									{65471,65473},
									{65480,65481},
									{65488,65489},
									{65496,65497},
									{65501,65503},
									{65519,65528},
									{65536,66303},
									{66340,66351},
									{66379,66559},
									{66598,66599},
									{66638,118783},
									{119030,119039},
									{119079,119081},
									{119262,119807},
									{119968,119969},
									{119971,119972},
									{119975,119976},
									{120075,120076},
									{120135,120137},
									{120484,120487},
									{120778,120781},
									{120832,131069},
									{173783,194559},
									{195102,196605},
									{196608,262141},
									{262144,327677},
									{327680,393213},
									{393216,458749},
									{458752,524285},
									{524288,589821},
									{589824,655357},
									{655360,720893},
									{720896,786429},
									{786432,851965},
									{851968,917501},
									{917506,917535},
									{917632,983037}};
   
}

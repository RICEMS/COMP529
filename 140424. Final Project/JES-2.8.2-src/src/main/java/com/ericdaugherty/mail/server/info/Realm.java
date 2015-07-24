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

package com.ericdaugherty.mail.server.info;

//Java Imports
import com.xlat4cast.jes.dns.internal.Domain;
import java.util.*;

/**
 * A class representing a DIGEST-MD5 Realm.
 *
 * @author Andreas Kyrmegalos
 */
public class Realm implements Persistable{
   
   private static final Realm nullRealm = new Realm();
   
   public static Realm getNullRealm() {
      return nullRealm;
   }

   private String fullRealmName = "null";
   private String collection = "";
   private Domain domain = Domain.getNullDomain();
   
   private final int hashCode;
   
   private String uniqueRealmName = "null";
   
   private List<User> users = new ArrayList<User>();
   
   protected Realm(){
      hashCode = 0;
   }
   
   /**
    * 
    * @param collection The collection part of a Realm
    * @param domain The domain part of a Realm
    * @throws {@link NullPointerException} if the collection or the domain are null
    * @throws {@link IllegalArgumentException} if the hashCode evaluates to zero
    */
   public Realm(String collection, Domain domain) {
      this.fullRealmName = collection+'@'+domain.getDomainName();
      this.collection = collection;
      this.domain = domain;
      uniqueRealmName = collection+'@'+domain.getUniqueName();
      hashCode = 17*collection.hashCode() + domain.hashCode();
      if (hashCode == 0) {
         throw new IllegalArgumentException("Use the nullRealm.");
      }
   }
   
   /**
    * 
    * @param domain The domain part of a Realm
    * @throws {@link NullPointerException} if the domain is null
    * @throws {@link IllegalArgumentException} if the hashCode evaluates to zero
    */
   public Realm(Domain domain) {
      this.fullRealmName = domain.getDomainName();
      this.domain = domain;
      uniqueRealmName = domain.getUniqueName();
      hashCode = uniqueRealmName.hashCode();
      if (hashCode == 0) {
         throw new IllegalArgumentException("Use the nullRealm.");
      }
   }
   
   public void addUser(User user) {
      users.add(user);
   }
   
   public boolean containsUser(User user) {
      return users.contains(user);
   }

   public String getFullRealmName() {
      return fullRealmName;
   }

   public String getCollection() {
      return collection;
   }

   public Domain getDomain() {
      return domain;
   }

   public String getUniqueName() {
      return uniqueRealmName;
   }
   
   public Iterator<User> userIterator() {
      return users.iterator();
   }
   
   public boolean isNullRealm() {
      return hashCode==0;
   }
   
   public boolean isDomainRealm() {
      return hashCode!=0&&collection==null;
   }

   /**
    * 
    * @return the hashCode as computed by String
    */
   @Override
   public final int hashCode() {
      return hashCode;
   }

   /**
    * Two polymorphic Realm Objects are treated as representing
    * the same Realm if the case-sensitive {@link String}
    * depiction of the collection and the case-insensitive {@link String}
    * depiction of the domain are equal for both objects.
    * 
    * @param object the object to consider whether it is meaningfully equivalent to this instance
    * @return true if this instance of Realm is equivalent to the supplied object parameter
    */
   @Override
   public final boolean equals(Object object) {

      if (object==this) return true;
      if (object==null) return false;
      if (!(object instanceof Realm)) return false;
      Realm that = (Realm)object;
      if ((this.collection.length()==0&&that.collection.length()!=0)||
            (!this.collection.equals(that.collection))) return false;
      if (!this.domain.equals(that.domain)) return false;
      return true;
   }
   
   @Override
   public String toString() {
       return "[Realm: "+fullRealmName+"]";
   }
}

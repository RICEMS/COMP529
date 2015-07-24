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

package com.ericdaugherty.mail.server.configuration;

//Java imports
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class TreeNode {
   
   private static final AtomicInteger ai = new AtomicInteger(1);
   private final int hashCode;
   private final String name;
   private final TreeNode parent;
   private String value;
   private final List<TreeNode> children = new ArrayList<TreeNode>();
   private final List<TreeAttribute> attributes = new ArrayList<TreeAttribute>();
   
   public TreeNode(String name, TreeNode parent) {
      this.name = name;
      this.parent = parent;
      this.hashCode = ai.getAndIncrement();
   }

   public String getName() {
      return name;
   }

   public TreeNode getParent() {
      return parent;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      if (!children.isEmpty()) {
         return;
      }
      this.value = value;
   }

   public List<TreeNode> getChildren() {
      return Collections.unmodifiableList(children);
   }
   
   public TreeNode getChild(String name) {
      Iterator<TreeNode> iter = children.iterator();
      TreeNode child;
      while(iter.hasNext()) {
         child = iter.next();
         if (child.getName().equals(name)) {
            return child;
         }
      }
      return null;
   }
   
   public void addChild(TreeNode child) {
      if (children.contains(child)) {
         return;
      }
      if (value!=null) {
         return;
      }
      children.add(child);
   }

   public List<TreeAttribute> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }
   
   public void addAttribute(TreeAttribute attribute) {
      if (attributes.contains(attribute)) {
         return;
      }
      attributes.add(attribute);
   }
   
   @Override
   public int hashCode() {
      return hashCode;
   }
   
   @Override
   public boolean equals(Object object) {
      
      if (object == null) {
         return false;
      }
      if (object == this) {
         return true;
      }
      if (!(object instanceof TreeNode)) {
         return false;
      }
      TreeNode that = (TreeNode)object;
      if (this.hashCode!=that.hashCode) {
         return false;
      }
      return true;
   }
   
   @Override
   public String toString(){
      return "TreeNode: "+name+" - "+value+" - "+attributes;
   }
}

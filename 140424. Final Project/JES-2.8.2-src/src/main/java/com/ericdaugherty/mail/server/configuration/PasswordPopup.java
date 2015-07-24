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

//Java Imports
import com.ericdaugherty.mail.server.Mail;
import java.awt.Color;
import java.awt.Dimension;
import java.util.*;
import java.util.Timer;
import javax.swing.*;

/**
 * A popup used to enter the keystore and/or kerberos 5 password(s) when no password exists
 * for the specified keystore/principal.
 * 
 * @author  Andreas Kyrmegalos
 */
public class PasswordPopup extends JDialog {

    private final String labelName;

    private VaultPassword vp;
    
    private final Object lock;
    
    private volatile boolean working;
    
    private final int[] passwordCheckerLimits;
    
    private final transient TimerTask timerTask;

   /** Creates new form BeanForm */
   PasswordPopup(VaultPassword vp, Object lock) {
      this(vp, null, lock);
   }
   
   /** Creates new form BeanForm */
   PasswordPopup(VaultPassword vp, String labelName, Object lock) {
      this(vp, labelName, lock, null);
   }
   
   /** Creates new form BeanForm */
   PasswordPopup(VaultPassword vp, String labelName, Object lock, int[] passwordCheckerLimits) {
      if (labelName != null) {
         this.labelName = labelName;
      } else {
         this.labelName = "Please enter keystore password:";
      }
      this.vp = vp;
      this.lock = lock;
      this.passwordCheckerLimits = passwordCheckerLimits;
      working = true;
      initComponents();
      if (passwordCheckerLimits != null) {

         getContentPane().removeAll();
         panel.removeAll();
         Dimension dimension;
         dimension = new Dimension(10 + strengthCheckerPanel.getPreferredSize().width + 10, 20 + label.getPreferredSize().height + passfield.getPreferredSize().height + okbutton.getPreferredSize().height + strengthCheckerPanel.getPreferredSize().height + 20);
         setPreferredSize(dimension);
         setMinimumSize(dimension);
         dimension = new Dimension(strengthCheckerPanel.getPreferredSize().width, label.getPreferredSize().height + passfield.getPreferredSize().height + okbutton.getPreferredSize().height + strengthCheckerPanel.getPreferredSize().height);
         panel.setPreferredSize(dimension);

         java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 2;
         panel.add(label, gridBagConstraints);

         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 1;
         gridBagConstraints.gridwidth = 2;
         gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         panel.add(passfield, gridBagConstraints);

         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
         panel.add(okbutton, gridBagConstraints);

         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 2;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
         panel.add(cancelbutton, gridBagConstraints);

         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridy = 3;
         gridBagConstraints.gridwidth = 3;
         panel.add(strengthCheckerPanel, gridBagConstraints);

         getContentPane().add(panel, gridBagConstraints);
      }
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      setAlwaysOnTop(true);
      timerTask = new TimerTask() {
         
         public void run() {
            exit();
            Mail.getInstance().shutdown();
         }
      };
      Timer timer = new Timer();
      timer.schedule(timerTask, 5*60*1000L);
   }
   
   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {
      java.awt.GridBagConstraints gridBagConstraints;

      strengthCheckerPanel = new javax.swing.JPanel();
      requirementsLabel = new javax.swing.JLabel();
      illegalDigitLabel = new javax.swing.JLabel();
      alphaDigitLabel = new javax.swing.JLabel();
      numericDigitLabel = new javax.swing.JLabel();
      QWERTYdigitLabel = new javax.swing.JLabel();
      repeatingDigitLabel = new javax.swing.JLabel();
      neighboringDigitLabel = new javax.swing.JLabel();
      nonASCIILabel = new javax.swing.JLabel();
      digitCountLabel = new javax.swing.JLabel();
      alphabeticLimitLabel = new javax.swing.JLabel();
      numericLimitLabel = new javax.swing.JLabel();
      QWERTYLimitLabel = new javax.swing.JLabel();
      repeatingLimitLabel = new javax.swing.JLabel();
      neighboringLimitLabel = new javax.swing.JLabel();
      nonASCIILimitLabel = new javax.swing.JLabel();
      digitCountLimitLabel = new javax.swing.JLabel();
      illegalDigitsLabel = new javax.swing.JLabel();
      alphaDigitsLabel = new javax.swing.JLabel();
      jLabel2 = new javax.swing.JLabel();
      jLabel3 = new javax.swing.JLabel();
      jLabel5 = new javax.swing.JLabel();
      jLabel6 = new javax.swing.JLabel();
      jLabel7 = new javax.swing.JLabel();
      panel = new javax.swing.JPanel();
      passfield = new javax.swing.JPasswordField();
      okbutton = new javax.swing.JButton();
      cancelbutton = new javax.swing.JButton();
      label = new javax.swing.JLabel();

      strengthCheckerPanel.setLayout(new java.awt.GridBagLayout());

      requirementsLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
      requirementsLabel.setText("Requirements");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.ipadx = 292;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
      strengthCheckerPanel.add(requirementsLabel, gridBagConstraints);

      illegalDigitLabel.setText("No Illegal characters");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipadx = 250;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(illegalDigitLabel, gridBagConstraints);

      alphaDigitLabel.setText("Maximum consecutive (case-sensitive) alphabetic digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipadx = 24;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(alphaDigitLabel, gridBagConstraints);

      numericDigitLabel.setText("Maximum consecutive numeric digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 5;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipadx = 143;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(numericDigitLabel, gridBagConstraints);

      QWERTYdigitLabel.setText("Maximum consecutive (case-sensitive) QWERTY digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 7;
      gridBagConstraints.ipadx = 39;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(QWERTYdigitLabel, gridBagConstraints);

      repeatingDigitLabel.setText("Maximum consecutive (case-sensitive) repeating digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 8;
      gridBagConstraints.ipadx = 29;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(repeatingDigitLabel, gridBagConstraints);

      neighboringDigitLabel.setText("Maximum consecutive (case-sensitive) neighboring digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 9;
      gridBagConstraints.gridheight = 3;
      gridBagConstraints.ipadx = 15;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(neighboringDigitLabel, gridBagConstraints);

      nonASCIILabel.setText("Minimum uppercase digits or non-ASCII alphanumeric digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 12;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
      strengthCheckerPanel.add(nonASCIILabel, gridBagConstraints);

      digitCountLabel.setText("Minimum number of digits");
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 14;
      gridBagConstraints.ipadx = 213;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 12, 12, 0);
      strengthCheckerPanel.add(digitCountLabel, gridBagConstraints);

      alphabeticLimitLabel.setText("4");
      alphabeticLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(alphabeticLimitLabel, gridBagConstraints);

      numericLimitLabel.setText("3");
      numericLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 5;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(numericLimitLabel, gridBagConstraints);

      QWERTYLimitLabel.setText("3");
      QWERTYLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 7;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(QWERTYLimitLabel, gridBagConstraints);

      repeatingLimitLabel.setText("3");
      repeatingLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 8;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(repeatingLimitLabel, gridBagConstraints);

      neighboringLimitLabel.setText("3");
      neighboringLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 9;
      gridBagConstraints.gridheight = 3;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(neighboringLimitLabel, gridBagConstraints);

      nonASCIILimitLabel.setText("2");
      nonASCIILimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 12;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 0, 0);
      strengthCheckerPanel.add(nonASCIILimitLabel, gridBagConstraints);

      digitCountLimitLabel.setText("10");
      digitCountLimitLabel.setMinimumSize(new java.awt.Dimension(20, 14));
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 14;
      gridBagConstraints.ipady = 3;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 18, 12, 0);
      strengthCheckerPanel.add(digitCountLimitLabel, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 12);
      strengthCheckerPanel.add(illegalDigitsLabel, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 12);
      strengthCheckerPanel.add(alphaDigitsLabel, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 5;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 12);
      strengthCheckerPanel.add(jLabel2, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 7;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(9, 6, 0, 12);
      strengthCheckerPanel.add(jLabel3, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 8;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 34;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 12);
      strengthCheckerPanel.add(jLabel5, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 12;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 12);
      strengthCheckerPanel.add(jLabel6, gridBagConstraints);
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 14;
      gridBagConstraints.ipadx = 55;
      gridBagConstraints.ipady = 14;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
      gridBagConstraints.insets = new java.awt.Insets(9, 6, 12, 12);
      strengthCheckerPanel.add(jLabel7, gridBagConstraints);

      setTitle("password entry");
      setMinimumSize(new java.awt.Dimension(240, 87));
      setName("password entry"); // NOI18N
      addWindowListener(new java.awt.event.WindowAdapter() {
         public void windowClosing(java.awt.event.WindowEvent evt) {
            formWindowClosing(evt);
         }
      });
      getContentPane().setLayout(new java.awt.GridBagLayout());

      panel.setMinimumSize(new java.awt.Dimension(240, 87));
      panel.setPreferredSize(new java.awt.Dimension(240, 87));
      panel.setLayout(new java.awt.GridBagLayout());

      passfield.setName("passfield"); // NOI18N
      passfield.setPreferredSize(new java.awt.Dimension(200, 27));
      passfield.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyPressed(java.awt.event.KeyEvent evt) {
            passfieldKeyPressed(evt);
         }
         public void keyReleased(java.awt.event.KeyEvent evt) {
            passfieldKeyReleased(evt);
         }
         public void keyTyped(java.awt.event.KeyEvent evt) {
            passfieldKeyTyped(evt);
         }
      });
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.gridwidth = 2;
      panel.add(passfield, gridBagConstraints);

      okbutton.setText("OK");
      okbutton.setMaximumSize(new java.awt.Dimension(90, 23));
      okbutton.setMinimumSize(new java.awt.Dimension(90, 23));
      okbutton.setName("OK"); // NOI18N
      okbutton.setPreferredSize(new java.awt.Dimension(90, 23));
      okbutton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            okbuttonActionPerformed(evt);
         }
      });
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
      panel.add(okbutton, gridBagConstraints);

      cancelbutton.setText("Cancel");
      cancelbutton.setMaximumSize(new java.awt.Dimension(90, 23));
      cancelbutton.setMinimumSize(new java.awt.Dimension(90, 23));
      cancelbutton.setName("Cancel"); // NOI18N
      cancelbutton.setPreferredSize(new java.awt.Dimension(90, 23));
      cancelbutton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cancelbuttonActionPerformed(evt);
         }
      });
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
      panel.add(cancelbutton, gridBagConstraints);

      label.setText(labelName);
      label.setName("keystorepass"); // NOI18N
      gridBagConstraints = new java.awt.GridBagConstraints();
      gridBagConstraints.gridwidth = 2;
      panel.add(label, gridBagConstraints);

      getContentPane().add(panel, new java.awt.GridBagConstraints());
   }// </editor-fold>//GEN-END:initComponents

   private void okbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okbuttonActionPerformed
      vp.setUserPass(passfield.getPassword().clone());
      exit();
   }//GEN-LAST:event_okbuttonActionPerformed

   private void cancelbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelbuttonActionPerformed
      exit();
   }//GEN-LAST:event_cancelbuttonActionPerformed

   private void passfieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passfieldKeyPressed

      final int keyCode = evt.getKeyCode();
      SwingUtilities.invokeLater(new Runnable() {

         public void run() {
            if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
               keyPressedKeyCode = keyCode;
            }
            else {
               keyPressedKeyCode = -1;
            }
         }
      });

   }//GEN-LAST:event_passfieldKeyPressed

   private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
      exit();
   }//GEN-LAST:event_formWindowClosing

   private void passfieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passfieldKeyTyped
      
      SwingUtilities.invokeLater(new Runnable() {

         public void run() {
            if (passwordCheckerLimits != null && passfield.getPassword().length!=0) {
               
               Color ok = Color.green;
               Color fail = Color.red;
               Map<String, String> results = PasswordStrengthChecker.checkStrength(new String(passfield.getPassword()));
               if (Boolean.valueOf(results.get("success"))) {
                  illegalDigitLabel.setForeground(ok);
                     illegalDigitsLabel.setText("");
                  alphaDigitLabel.setForeground(ok);
                  alphabeticLimitLabel.setForeground(ok);
                     alphaDigitsLabel.setText("");
                  numericDigitLabel.setForeground(ok);
                  numericLimitLabel.setForeground(ok);
                     jLabel2.setText("");
                  QWERTYdigitLabel.setForeground(ok);
                  QWERTYLimitLabel.setForeground(ok);
                     jLabel3.setText("");
                  repeatingDigitLabel.setForeground(ok);
                  repeatingLimitLabel.setForeground(ok);
                  neighboringDigitLabel.setForeground(ok);
                  neighboringLimitLabel.setForeground(ok);
                     jLabel5.setText("");
                  nonASCIILabel.setForeground(ok);
                  nonASCIILimitLabel.setForeground(ok);
                  digitCountLabel.setForeground(ok);
                  digitCountLimitLabel.setForeground(ok);
                  okbutton.setEnabled(true);
               } else {

                  if (results.get("illegalChar") != null) {
                     illegalDigitLabel.setForeground(fail);
                     illegalDigitsLabel.setText(results.get("illegalChar"));
                  } else {
                     illegalDigitLabel.setForeground(ok);
                     illegalDigitsLabel.setText("");
                  }
                  if (results.get("consecutiveAlpha") != null) {
                     alphaDigitLabel.setForeground(fail);
                     alphabeticLimitLabel.setForeground(fail);
                     alphaDigitsLabel.setText(results.get("consecutiveAlpha"));
                  } else {
                     alphaDigitLabel.setForeground(ok);
                     alphabeticLimitLabel.setForeground(ok);
                     alphaDigitsLabel.setText("");
                  }
                  if (results.get("consecutiveNumeric") != null) {
                     numericDigitLabel.setForeground(fail);
                     numericLimitLabel.setForeground(fail);
                     jLabel2.setText(results.get("consecutiveNumeric"));
                  } else {
                     numericDigitLabel.setForeground(ok);
                     numericLimitLabel.setForeground(ok);
                     jLabel2.setText("");
                  }
                  if (results.get("QWERTYDigits") != null) {
                     QWERTYdigitLabel.setForeground(fail);
                     QWERTYLimitLabel.setForeground(fail);
                     jLabel3.setText(results.get("QWERTYDigits"));
                  } else {
                     QWERTYdigitLabel.setForeground(ok);
                     QWERTYLimitLabel.setForeground(ok);
                     jLabel3.setText("");
                  }
                  if (results.get("lowEntropy") != null) {
                     repeatingDigitLabel.setForeground(fail);
                     repeatingLimitLabel.setForeground(fail);
                     neighboringDigitLabel.setForeground(fail);
                     neighboringLimitLabel.setForeground(fail);
                     jLabel5.setText(results.get("lowEntropy"));
                  } else {
                     repeatingDigitLabel.setForeground(ok);
                     repeatingLimitLabel.setForeground(ok);
                     neighboringDigitLabel.setForeground(ok);
                     neighboringLimitLabel.setForeground(ok);
                     jLabel5.setText("");
                  }
                  if (!(results.get("upperCase") != null || results.get("nonASCIIAlphaNumeric") != null)) {
                     nonASCIILabel.setForeground(fail);
                     nonASCIILimitLabel.setForeground(fail);
                  } else {
                     nonASCIILabel.setForeground(ok);
                     nonASCIILimitLabel.setForeground(ok);
                  }
                  if (results.get("leastNumber") == null) {
                     digitCountLabel.setForeground(fail);
                     digitCountLimitLabel.setForeground(fail);
                  } else {
                     digitCountLabel.setForeground(ok);
                     digitCountLimitLabel.setForeground(ok);
                  }
                  okbutton.setEnabled(false);
               }
            }
         }
      });
   }//GEN-LAST:event_passfieldKeyTyped

   private void passfieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passfieldKeyReleased
      
      final int keyCode = evt.getKeyCode();
      SwingUtilities.invokeLater(new Runnable() {

         public void run() {
            
            if (keyPressedKeyCode == keyCode) {
               if (keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                  if (okbutton.isEnabled()) {
                     vp.setUserPass(passfield.getPassword().clone());
                     exit();
                  }
               }
               else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                  exit();
               }
            }
         }
      });
   }//GEN-LAST:event_passfieldKeyReleased
   
   private int keyPressedKeyCode = -1;
   
   private synchronized void exit() {

      if (!working) return;
      for (int i=passfield.getPassword().length-1;i>=0;i--) {
         passfield.getPassword()[i] = 0x00;
      }
      setVisible(false);
      setEnabled(false);
      dispose();
      working = false;
      timerTask.cancel();
      synchronized(lock) {
         lock.notifyAll();
      }
   }
   
   public boolean isWorking() {
      return working;
   }
   
   static class PasswordPopupRunnable implements Runnable {

      private final PasswordPopup ppu;

      PasswordPopupRunnable(PasswordPopup ppu) {
         this.ppu = ppu;
      }
      public void run() {
         ppu.pack();
         ppu.setLocationRelativeTo(null);
         ppu.setVisible(true);
      }
   }
   
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel QWERTYLimitLabel;
   private javax.swing.JLabel QWERTYdigitLabel;
   private javax.swing.JLabel alphaDigitLabel;
   private javax.swing.JLabel alphaDigitsLabel;
   private javax.swing.JLabel alphabeticLimitLabel;
   private javax.swing.JButton cancelbutton;
   private javax.swing.JLabel digitCountLabel;
   private javax.swing.JLabel digitCountLimitLabel;
   private javax.swing.JLabel illegalDigitLabel;
   private javax.swing.JLabel illegalDigitsLabel;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JLabel jLabel6;
   private javax.swing.JLabel jLabel7;
   private javax.swing.JLabel label;
   private javax.swing.JLabel neighboringDigitLabel;
   private javax.swing.JLabel neighboringLimitLabel;
   private javax.swing.JLabel nonASCIILabel;
   private javax.swing.JLabel nonASCIILimitLabel;
   private javax.swing.JLabel numericDigitLabel;
   private javax.swing.JLabel numericLimitLabel;
   private javax.swing.JButton okbutton;
   private javax.swing.JPanel panel;
   private transient javax.swing.JPasswordField passfield;
   private javax.swing.JLabel repeatingDigitLabel;
   private javax.swing.JLabel repeatingLimitLabel;
   private javax.swing.JLabel requirementsLabel;
   private javax.swing.JPanel strengthCheckerPanel;
   // End of variables declaration//GEN-END:variables
   
}

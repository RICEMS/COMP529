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

package com.ericdaugherty.mail.server.services.smtp;

//Java imports
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Local imports
import com.ericdaugherty.mail.server.info.EmailAddress;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A class representing an instance of a JES SMTP Message.
 *
 * @author Andreas Kyrmegalos
 */
public class SMTPMessageImpl extends AbstractPersistableSMTPMessage implements SMTPMessage {

    //***************************************************************
    // Variables
    //***************************************************************

    private Date timeReceived;
    private Date scheduledDelivery;
    private int deliveryAttempts;
    private EmailAddress fromAddress;
    private boolean mime8bit;
    private List<EmailAddress> toAddresses = new ArrayList<EmailAddress>();
    private String smtpUID;
    private final List<byte[]> dataLines = new ArrayList<byte[]>();
    private long size;

    //***************************************************************
    // Constructor
    //***************************************************************

    /**
     * Instantiates a new message with the current time.
     */
    SMTPMessageImpl() {
       Date now = new Date();
       timeReceived = now;
       scheduledDelivery = now;
    }

    @Override
    public int hashCode() {
       return getSmtpUid().hashCode();
    }

    @Override
    public boolean equals(Object object) {
       if (!(object instanceof SMTPMessage)) {
         return false;
      }
      SMTPMessage that = (SMTPMessage) object;
      if (!this.getSmtpUid().equals(that.getSmtpUid())) return false;
      return true;
    }

    @Override
    public Date getTimeReceived() {
        return (Date)timeReceived.clone();
    }

    @Override
    public void setTimeReceived(@NonNull Date timeReceived) {
        this.timeReceived = (Date)timeReceived.clone();
    }

    @Override
    public Date getScheduledDelivery() {
        return (Date)scheduledDelivery.clone();
    }

    @Override
    public void setScheduledDelivery(@NonNull Date scheduledDelivery) {
        this.scheduledDelivery = (Date)scheduledDelivery.clone();
    }

    @Override
    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    @Override
    public void setDeliveryAttempts(int deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    @Override
    public EmailAddress getFromAddress(){ return fromAddress; }

    @Override
    public void setFromAddress(EmailAddress fromAddress){ this.fromAddress = fromAddress; }

    @Override
    public List<EmailAddress> getToAddresses() { return toAddresses; }

    @Override
    public void setToAddresses( List<EmailAddress> toAddresses ) { this.toAddresses = toAddresses; }

    @Override
    public void addToAddress( EmailAddress toAddress ) { if (!toAddresses.contains(toAddress)) toAddresses.add( toAddress ); }

    @Override
    public boolean is8bitMIME() {
       return mime8bit;
    }

    @Override
    public void set8bitMIME(boolean mime8bit) {
       this.mime8bit = mime8bit;
    }

    @Override
    public List<byte[]> getDataLines() { return dataLines; }
    
    @Override
    public final void setSmtpUid(String smtpUID) {
       this.smtpUID = smtpUID;
    }

    @Override
    public void incrementSize(long increment) {
       size += increment;
    }

    @Override
    public long getSize() {
       if (size == 0) {
          size = super.getSize();
       }
       return size;
    }
    
    @Override
    public String getSmtpUid() {
       return smtpUID;
    }
}
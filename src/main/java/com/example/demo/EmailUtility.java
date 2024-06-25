package com.example.demo;

import javax.mail.*;
import javax.mail.internet.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class EmailUtility {
	
	@Value("${email.from}")
	private String from;
	
	@Value("${email.to}")
	private String to;
	
	public void sendEmail(String msg, String status) {
		
        // Setup mail server properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "MailHost.health.state.ny.usmail.svc.ny.gov");
        props.put("mail.smtp.port", "25");
        Session session = Session.getInstance(props);
        
        try {
            // Create a default MimeMessage object
            MimeMessage message = new MimeMessage(session);
            
            // Set From: header field of the header
            message.setFrom(new InternetAddress(from));
            
            // Set To: header field of the header
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            
            // Set Subject: header field
            message.setSubject(msg);
            
            // Now set the actual message
            message.setText(msg);
            
            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
	}
}

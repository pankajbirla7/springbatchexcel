package com.example.demo;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.utility.ExceptionUtils;
import com.example.utility.Utility;

import java.util.Properties;

@Component
public class EmailUtility {
	
	static final Logger logger = LoggerFactory.getLogger(EmailUtility.class);

	
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
            logger.info("Email sent successfully....");
        } catch (Exception mex) {
            mex.printStackTrace();
            logger.error("Exception occured while sending email due to :: "+ExceptionUtils.getStackTrace(mex));
        }
	}
	
public void sendEmailWithAttcahment(String msg, String status, String filePath) {
		
        // Setup mail server properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "MailHost.health.state.ny.usmail.svc.ny.gov");
        props.put("mail.smtp.port", "25");
        Session session = Session.getInstance(props);
        
        try {
            MimeMessage message = new MimeMessage(session);
            
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(msg);
         // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText(msg);

            // Create a multipart message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            String filename = filePath;
            DataSource source = new FileDataSource(filename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);
            
            // Send message
            Transport.send(message);
            logger.info("Email sent successfully with attchiing file :: "+filePath);
        } catch (MessagingException mex) {
            mex.printStackTrace();
            logger.error("Exception occured while sending email with file attcahed is :: "+filePath+" -- due to :: "+ExceptionUtils.getStackTrace(mex));
        }
	}
}

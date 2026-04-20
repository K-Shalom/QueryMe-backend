package com.year2.queryme.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTemporaryPassword(String to, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Temporary Password for QueryMe");
        message.setText("Welcome to QueryMe!\n\n" +
                "An account has been created for you. Your temporary password is: " + tempPassword + "\n\n" +
                "You will be required to change this password when you first log in.");
        
        try {
            mailSender.send(message);
            System.out.println("Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            System.out.println("FALLBACK: View the password here: " + tempPassword);
        }
    }
}

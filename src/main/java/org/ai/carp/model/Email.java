package org.ai.carp.model;


import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

@Component
public class Email {
    private static final Logger logger = LoggerFactory.getLogger(Email.class);

    private static Email ourInstance;

    public static Email getInstance() {
        return ourInstance;
    }

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    TemplateEngine templateEngine;

    public void sendSimpleEmail(String deliver, String[] receiver, String[] carbonCopy, String subject, String content)
            throws MailException {

        long startTimestamp = System.currentTimeMillis();
        logger.info("Start send mail ... ");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(deliver);
            message.setTo(receiver);
            message.setCc(carbonCopy);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            logger.info("Send mail success, cost {} million seconds", System.currentTimeMillis() - startTimestamp);
        } catch (MailException e) {
            logger.error("Send mail failed, error message is : {} \n", e.getMessage());
//            e.printStackTrace();
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private Email() {
        ourInstance = this;
    }

}
package org.ai.carp.model;


import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.model.user.User;
import org.ai.carp.model.user.VerifyCodeRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;

@Component
public class Email {
    private static final Logger logger = LoggerFactory.getLogger(Email.class);

    private static Email ourInstance;
    private VerifyCodeRepository verifyCodeRepository;

    public VerifyCodeRepository getVerifyCodeRepository() {
        return verifyCodeRepository;
    }

    public String getEmailAddress(User user) {
        if (!StringUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        } else if (user.getUsername().matches("[0-9]+")) {
            return String.format("%s@mail.sustech.edu.cn", user.getUsername());
        }
        throw new InvalidRequestException("I don't known your email address, please contact administrator");
    }


    @Autowired
    public void setVerifyCodeRepository(VerifyCodeRepository verifyCodeRepository) {
        this.verifyCodeRepository = verifyCodeRepository;
    }

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

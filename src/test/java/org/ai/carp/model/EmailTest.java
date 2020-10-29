package org.ai.carp.model;

import org.ai.carp.CarpServerApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = CarpServerApplication.class)
public class EmailTest {

    @Test
    public void testSendSimple() {
        Email email = Email.getInstance();
        String[] receiver = {"11712225@mail.sustech.edu.cn"};
        String[] carbonCopy = {"11712225@mail.sustech.edu.cn"};
        String subject = "Verify Code for NCS judge platform";
        String content = "Your verfy code is 123456";
        try {
            email.sendSimpleEmail(receiver, carbonCopy, subject, content);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}

package org.ai.carp.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.util.VerifyCodeGeneUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.Email;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.user.User;
import org.ai.carp.model.user.VerifyCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user/verify_code")
public class SendVerifyCodeController {

    @PostMapping
    public SendVerifyCodeResponse post(@RequestBody SendVerifyCodeRequest request, HttpSession session) {
        User user = UserUtils.findUserByUserName(request.userName);

        String deliver = "11712225@mail.sustech.edu.cn";
        String[] receiver = {Email.getInstance().getEmailAddress(user)};
        String[] carbonCopy = {};
        String subject = "Verify Code for NCS judge platform";
        String code = VerifyCodeGeneUtils.getCode();
        String content = String.format("Your verfy code is %s", code);
        try {
            Email email = Email.getInstance();
            email.sendSimpleEmail(deliver, receiver, carbonCopy, subject, content);
            VerifyCode verifyCode = email.getVerifyCodeRepository().save(new VerifyCode(user, code));
            return new SendVerifyCodeResponse(verifyCode.getGenerateTime().toString());
        } catch (Exception e) {
            throw new InvalidRequestException("send email failed!");
        }
    }

}

class SendVerifyCodeRequest {
    @JsonProperty("userName")
    public String userName;
}

class SendVerifyCodeResponse {

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    private String codeId;

    public SendVerifyCodeResponse(String codeId) {
        this.codeId = codeId;
    }
}

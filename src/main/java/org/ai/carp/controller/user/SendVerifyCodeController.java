package org.ai.carp.controller.user;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.model.Database;
import org.ai.carp.model.Email;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.user.User;
import org.ai.carp.model.user.VerifyCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user/verify_code")
public class SendVerifyCodeController {

    @PostMapping
    public SendVerifyCodeResponse post(HttpSession session) {
        User user = UserUtils.getUser(session, User.MAX);
        if(user==null){
            throw new InvalidRequestException("invalid user!");
        }
        String deliver = "hy_a12@163.com";
        String[] receiver = {String.format("%s@mail.sustech.edu.cn",user.getUsername())};
        String[] carbonCopy = {"11610303@mail.sustech.edu.cn"};
        String subject = "Verify Code for NCS judge platform";
        String code = "123456";
        String content = String.format("Your verfy code is %s", code);
        try {
            Email.getInstance().sendSimpleEmail(deliver, receiver, carbonCopy, subject, content);
            VerifyCode verifyCode = Database.getInstance().getVerifyCodeRepository().save(new VerifyCode(user, code));
            return new SendVerifyCodeResponse(verifyCode.getGenerateTime().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new InvalidRequestException("send email failed!");
    }

}
//
//class ChangePasswordRequest {
//    @JsonProperty("old")
//    public String oldP;
//    @JsonProperty("new")
//    public String newP;
//}
//
class SendVerifyCodeResponse {

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    private String codeId;

    public SendVerifyCodeResponse(String codeId){
        this.codeId = codeId;
    }
}

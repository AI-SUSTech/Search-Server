package org.ai.carp.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.controller.util.VerifyCodeGeneUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.Email;
import org.ai.carp.model.user.User;
import org.ai.carp.model.user.VerifyCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user/change/password")
public class ChangePasswordController {

    @PostMapping
    public ChangePasswordResponse post(@RequestBody ChangePasswordRequest request, HttpSession session) {
//        if (StringUtils.isEmpty(request.oldP)) {
//            throw new InvalidRequestException("No old password!");
//        }
        if (StringUtils.isEmpty(request.userName)) {
            throw new InvalidRequestException("No student ID!");
        }
        if (StringUtils.isEmpty(request.newP)) {
            throw new InvalidRequestException("No new password!");
        }
        if (StringUtils.isEmpty(request.code)) {
            throw new InvalidRequestException("No verify code!");
        }
        if (request.newP.length() > 32) {
            throw new InvalidRequestException("Password too long!");
        }
//        User user = UserUtils.getUser(session, User.MAX);
        User user = UserUtils.findUserByUserName(request.userName);


        VerifyCode verifyCode = Email.getInstance().getVerifyCodeRepository().findTopByUserOrderByGenerateTimeDesc(user);
        if(!VerifyCodeGeneUtils.checkVerifyCode(verifyCode, request.code)){
            throw new InvalidRequestException("Wrong verify code!");
        }
//        if (!user.passwordMatches(request.oldP)) {
//            throw new InvalidRequestException("Wrong old password!");
//        }
        user.setPassword(request.newP);
        Database.getInstance().getUsers().save(user);
        return new ChangePasswordResponse(user.getId());
    }

}

class ChangePasswordRequest {
    @JsonProperty("userName")
    public String userName;
    @JsonProperty("new")
    public String newP;
    @JsonProperty("code")
    public String code;

}

class ChangePasswordResponse {

    private String uid;

    ChangePasswordResponse(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }
}

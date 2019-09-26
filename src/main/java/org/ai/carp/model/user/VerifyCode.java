package org.ai.carp.model.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Date;

public class VerifyCode {

    @DBRef
    @Indexed
    protected User user;

    public VerifyCode(User user, String code){
        this.user = user;
        this.code = code;
    }

    @Id
    private String id;


    String code;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(Date generateTime) {
        this.generateTime = generateTime;
    }

    protected Date generateTime;


}

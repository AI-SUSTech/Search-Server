package org.ai.carp.controller.util;

import org.ai.carp.model.user.VerifyCode;

import java.util.Date;
import java.util.Random;

public class VerifyCodeGeneUtils {
    private static Random random = new Random(System.currentTimeMillis());

    public static String getCode(){
        return String.format("%06d", (random.nextInt(1000000) + 1000000) % 1000000);
    }

    public static boolean checkVerifyCode(VerifyCode verifyCode, String code){
        if(verifyCode.getCode().equals(code)){
            // check if it is five minutes generated code
            return verifyCode.getGenerateTime().getTime() > System.currentTimeMillis() - 1000 * 300 ;
        }
        return false;
    }
}

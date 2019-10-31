package org.ai.carp.controller.judge;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Deadline {
    private static Date ddl;

    static{
        try{
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ddl = df.parse("2019-11-29 23:56:00");
        }catch(Exception e){
            System.out.println("parse error:" + e.getMessage());
        }
    }

    public static boolean isDDL(){
        return new Date().getTime() >= ddl.getTime();
    }


}

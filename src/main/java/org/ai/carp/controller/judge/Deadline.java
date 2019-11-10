package org.ai.carp.controller.judge;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Deadline {
    private static Date impDDL;
    private static Date iseDDL;
    private static Date ncsDDL;


    static{
        try{
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            iseDDL = df.parse("2019-11-10 23:56:00");
            impDDL = df.parse("2019-11-17 23:56:00");
            ncsDDL = df.parse("2019-11-17 23:56:00");
        }catch(Exception e){
            System.out.println("parse error:" + e.getMessage());
        }
    }

    public static Date getImpDDL(){
        return impDDL;
    }

    public static Date getIseDDL(){
        return iseDDL;
    }

    public static boolean isDDL(Date ddl){
        return new Date().getTime() >= ddl.getTime();
    }


    public static Date getNcsDDL() {
        return ncsDDL;
    }
}

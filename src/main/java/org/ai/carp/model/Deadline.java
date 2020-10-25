package org.ai.carp.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class Deadline {
    private static Date impDDL;
    private static Date iseDDL;
    private static Date ncsDDL;

    private static final Logger logger = LoggerFactory.getLogger(Deadline.class);

    @Autowired
    public Deadline(@Value("${ddl.imp}") String DdlImp,
                    @Value("${ddl.ise}") String DdlIse,
                    @Value("${ddl.ncs}") String DdlNcs) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            impDDL = df.parse(DdlImp);
            iseDDL = df.parse(DdlIse);
            ncsDDL = df.parse(DdlNcs);
        } catch (Exception e) {
            System.out.println("parse error:" + e.getMessage());
        }
    }

    public static Date getImpDDL() {
        return impDDL;
    }

    public static Date getIseDDL() {
        return iseDDL;
    }

    public static boolean isDDL(Date ddl) {
        return new Date().getTime() >= ddl.getTime();
    }


    public static Date getNcsDDL() {
        return ncsDDL;
    }
}

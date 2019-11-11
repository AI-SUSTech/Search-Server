package org.ai.carp.controller.admin.judge;

import org.ai.carp.controller.judge.QueryTopResult;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.user.User;
import org.ai.carp.service.BaseFunction;
import org.ai.carp.service.FunctionFactory;
import org.ai.carp.service.ISEFunction;
import org.ai.carp.service.NCSFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GetSubmitList {


    @GetMapping
    @RequestMapping(value = "/api/admin/judge/submit/ise", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> getISE(HttpSession session) throws IOException {
        UserUtils.getUser(session, User.ROOT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BaseFunction baseFunction = FunctionFactory.getCaseFunction(BaseDataset.ISE);
            Workbook wb = baseFunction.getFinalGrades();
            wb.write(baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "ise.xlsx");
        headers.setContentLength(baos.size());
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }



    @GetMapping
    @RequestMapping(value = "/api/admin/judge/submit/imp", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> getIMP(HttpSession session) throws IOException {
        UserUtils.getUser(session, User.ROOT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BaseFunction baseFunction = FunctionFactory.getCaseFunction(BaseDataset.IMP);
            Workbook wb = baseFunction.getFinalGrades();
            wb.write(baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "imp.xlsx");
        headers.setContentLength(baos.size());
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

}

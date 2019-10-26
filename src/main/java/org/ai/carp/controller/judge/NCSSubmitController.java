package org.ai.carp.controller.judge;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.*;
import org.ai.carp.model.Database;
import org.ai.carp.service.BaseFunction;
import org.ai.carp.service.FunctionFactory;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.*;
import org.ai.carp.model.user.User;
import org.ai.carp.runner.JudgeRunner;
import org.bson.types.Binary;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


@RestController
@RequestMapping("/api/judge/submit")
public class NCSSubmitController {

    private static Date ddl;

    static{
        try{
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ddl = df.parse("2019-10-28 23:56:00");
        }catch(Exception e){
            System.out.println("parse error:" + e.getMessage());
        }
    }


    @PostMapping
    public NCSSubmitResponse post(@RequestBody NCSPostCase postCase, HttpSession session) {
        User user = UserUtils.getUser(session, User.USER);
        if(user.passwordMatches(user.getUsername())){
            throw new PermissionDeniedException("Please change your password!");
        }

        if(new Date().getTime() >= ddl.getTime()){
            throw new InvalidRequestException("Deadline has passed!");
        }

        if (StringUtils.isEmpty(postCase.data)) {
            throw new InvalidRequestException("No data!");
        }
        BaseDataset dataset = DatasetUtils.apiGetById(postCase.dataset);
        if (!dataset.isEnabled()) {
            throw new PermissionDeniedException("Dataset is disabled!");
        }
        if ((!dataset.isSubmittable() || dataset.isFinalJudge()) && user.getType() > User.ADMIN) {
            throw new PermissionDeniedException("Dataset is not submittable!");
        }
        if (user.getType() == User.USER &&
                CaseUtils.countPreviousDay(user) >= CARPCase.DAILY_LIMIT) {
            throw new PermissionDeniedException("You have reached daily limits on submission!");
        }

        Binary archive = ParameterFileUtils.convertSubmit((String) postCase.data);
        // todo should use other methods to mark it is ncs
        BaseFunction caseBaseFunction;
        try {
            caseBaseFunction = FunctionFactory.getCaseFunction(dataset.getType());
        } catch (Exception e) {
            throw new PermissionDeniedException(e.getMessage());
        }
        BaseCase baseCase = caseBaseFunction.insert(user, dataset, archive);
        Database.getInstance().getLiteCases().insert(new LiteCase(baseCase));
        JudgeRunner.queue.add(baseCase);
        int remain = NCSCase.DAILY_LIMIT - CaseUtils.countPreviousDay(user);
        return new NCSSubmitResponse(baseCase.getId(), remain);
    }

}

class NCSSubmitResponse {

    private Object cid;
    private int remain;

    public NCSSubmitResponse(Object cid, int remain) {
        this.cid = cid;
        this.remain = remain;
    }

    public Object getCid() {
        return cid;
    }

    public int getRemain() {
        return remain;
    }
}

class NCSPostCase {
    public String dataset;
    public Object data;
}

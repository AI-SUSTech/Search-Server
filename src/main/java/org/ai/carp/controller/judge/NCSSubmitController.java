package org.ai.carp.controller.judge;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.ArchiveUtils;
import org.ai.carp.controller.util.CaseUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.Database;
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

@RestController
@RequestMapping("/api/judge/submit")
public class NCSSubmitController {

    @PostMapping
    public NCSSubmitResponse post(@RequestBody NCSPostCase postCase, HttpSession session) {
        User user = UserUtils.getUser(session, User.USER);
//        if (new Date().getTime() >= new Date(2019,8,27,22, 30).getTime()){//1544803200000L) {
//            throw new InvalidRequestException("Deadline has passed!");
//        }
        if (StringUtils.isEmpty(postCase.data)) {
            throw new InvalidRequestException("No data!");
        }
//        BaseDataset dataset = DatasetUtils.apiGetById(postCase.dataset);
//        if (!dataset.isEnabled()) {
//            throw new PermissionDeniedException("Dataset is disabled!");
//        }
//        if ((!dataset.isSubmittable() || dataset.isFinalJudge()) && user.getType() > User.ADMIN) {
//            throw new PermissionDeniedException("Dataset is not submittable!");
//        }
        if (user.getType() == User.USER &&
                CaseUtils.countPreviousDay(user) >= CARPCase.DAILY_LIMIT) {
            throw new PermissionDeniedException("You have reached daily limits on submission!");
        }
        Binary archive = ArchiveUtils.convertSubmission((String) postCase.data, "test.txt");
        BaseCase baseCase;

        baseCase = Database.getInstance().getNcsCases().insert(new NCSCase(user, archive));

        Database.getInstance().getLiteCases().insert(new LiteCase(baseCase));
        JudgeRunner.queue.add(baseCase);
        int remain = CARPCase.DAILY_LIMIT - CaseUtils.countPreviousDay(user);
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

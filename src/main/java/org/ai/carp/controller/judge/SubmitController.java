package org.ai.carp.controller.judge;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.ArchiveUtils;
import org.ai.carp.controller.util.CaseUtils;
import org.ai.carp.controller.util.DatasetUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.service.BaseFunction;
import org.ai.carp.model.Database;
import org.ai.carp.service.FunctionFactory;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.*;
import org.ai.carp.model.user.User;
import org.ai.carp.runner.JudgeRunner;
import org.bson.types.Binary;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController
@RequestMapping("/api/judge/submit")
public class SubmitController {

    @PostMapping
    public SubmitResponse post(@RequestBody PostCase postCase, HttpSession session) {
        User user = UserUtils.getUser(session, User.USER);
        if (user.passwordMatches(user.getUsername())) {
            throw new PermissionDeniedException("Please change your password!");
        }

        if (StringUtils.isEmpty(postCase.data)) {
            throw new InvalidRequestException("No data!");
        }

        BaseDataset dataset = DatasetUtils.apiGetById(postCase.dataset);
        switch (dataset.getType()) {
            case BaseDataset.IMP:
                if (Deadline.isDDL(Deadline.getImpDDL())) {
                    throw new InvalidRequestException("IMP Deadline has passed!");
                }
                break;
            case BaseDataset.ISE:
                if (Deadline.isDDL(Deadline.getIseDDL())) {
                    throw new InvalidRequestException("ISE Deadline has passed!");
                }
                break;
        }
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
        Binary archive = ArchiveUtils.convertSubmission(postCase.data, dataset.getEntry());
        BaseCase baseCase;

        try {
            BaseFunction baseFunction = FunctionFactory.getCaseFunction(dataset.getType());
            baseCase = baseFunction.insert(user, dataset, archive);
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid dataset type!");
        }
//        switch (dataset.getType()) {
//            case BaseDataset.CARP:
//                baseCase = Database.getInstance().getCarpCases().insert(new CARPCase(user, (CARPDataset)dataset, archive));
//                break;
//            case BaseDataset.ISE:
//                baseCase = Database.getInstance().getIseCases().insert(new ISECase(user, (ISEDataset)dataset, archive));
//                break;
//            case BaseDataset.IMP:
//                baseCase = Database.getInstance().getImpCases().insert(new IMPCase(user, (IMPDataset)dataset, archive));
//                break;
//            default:
//                throw new InvalidRequestException("Invalid dataset type!");
//        }
        Database.getInstance().getLiteCases().insert(new LiteCase(baseCase));
        JudgeRunner.queue.add(baseCase);
        int remain = CARPCase.DAILY_LIMIT - CaseUtils.countPreviousDay(user);
        return new SubmitResponse(baseCase.getId(), remain);
    }

    @GetMapping
    public CountSubmitResponse get(HttpSession session) {
        User user = UserUtils.getUser(session, User.ADMIN);
        int imps = CaseUtils.countIMPSubmit().size();
        int ises = CaseUtils.countISESubmit().size();
        return new CountSubmitResponse(imps, ises, user.getUsername());
    }

    static class CountSubmitResponse{
        public CountSubmitResponse(int imps, int ises, String queryer) {
            this.imps = imps;
            this.ises = ises;
            this.queryer = queryer;
        }

        public int imps;
        public int ises;
        public String queryer;
    }

}

//class SubmitResponse {
//
//    private String cid;
//    private int remain;
//
//    public SubmitResponse(String cid, int remain) {
//        this.cid = cid;
//        this.remain = remain;
//    }
//
//    public String getCid() {
//        return cid;
//    }
//
//    public int getRemain() {
//        return remain;
//    }
//}
//
//class PostCase {
//    public String dataset;
//    public String data;
//}

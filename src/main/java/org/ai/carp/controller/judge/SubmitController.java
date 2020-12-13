package org.ai.carp.controller.judge;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.ArchiveUtils;
import org.ai.carp.controller.util.CaseUtils;
import org.ai.carp.controller.util.DatasetUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.Deadline;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.CARPCase;
import org.ai.carp.model.judge.IMPCase;
import org.ai.carp.model.judge.LiteCase;
import org.ai.carp.model.user.User;
import org.ai.carp.runner.JudgeRunner;
import org.ai.carp.service.BaseFunction;
import org.ai.carp.service.FunctionFactory;
import org.bson.types.Binary;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/judge/submit")
public class SubmitController {

    private SubmitResponse changeUserCode(String userName, PostCase postCase) throws InvalidRequestException {
        User user = Database.getInstance().getUsers().findByUsername(userName);
        if (user == null) {
            throw new InvalidRequestException("not exist user:" + userName);
        }
        IMPCase finalSubmit = Database.getInstance().getImpCases()
                .findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(user, Deadline.getImpDDL());
        if (finalSubmit == null) {
            throw new InvalidRequestException("no code!");
        }
        Binary archive = ArchiveUtils.convertSubmission(postCase.data, "IMP.py");

        finalSubmit.setArchive(archive);
        finalSubmit.reset();
        BaseCase baseCase = CaseUtils.saveCase(finalSubmit);
        JudgeRunner.queue.add(baseCase);

        int remain = CARPCase.DAILY_LIMIT - CaseUtils.countPreviousDay(user);
        System.out.println("change latest code of:" + user.getUsername());

        return new SubmitResponse(finalSubmit.getId(), remain);

    }


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
        if(user.getType() != User.ADMIN) {
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
        Database.getInstance().getLiteCases().insert(new LiteCase(baseCase));
        JudgeRunner.queue.add(baseCase);
        int remain = CARPCase.DAILY_LIMIT - CaseUtils.countPreviousDay(user);
        return new SubmitResponse(baseCase.getId(), remain);
    }

}

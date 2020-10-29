package org.ai.carp.controller.judge;

import org.ai.carp.CARPSetup;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.DatasetUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.CARPDataset;
import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.user.User;
import org.ai.carp.service.FunctionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/judge/top")
public class QueryTopController {
    private static final Logger logger = LoggerFactory.getLogger(QueryTopController.class);
    public static final int COUNT_LEADERBOARD = 20;


    @GetMapping
    public QueryTopResult get(@RequestParam("dataset") String did, HttpSession session) {
        User user = UserUtils.getUser(session, User.MAX);
        BaseDataset dataset = DatasetUtils.apiGetById(did);
        if (dataset.isFinalJudge() && user.getType() > User.ADMIN) {
            throw new PermissionDeniedException("Permission denied!");
        }
        List<BaseCase> allBaseCases = new ArrayList<>();
        Set<String> invalidUids = new HashSet<>();
        try {
            allBaseCases = FunctionFactory.getCaseFunction(dataset.getType()).queryAllDatasetOfUser(dataset);
        } catch (Exception e) {
            throw new InvalidRequestException("unknown dataset type");
        }
        if (dataset.getType() == BaseDataset.CARP) {
            invalidUids = Database.getInstance().getCarpCases()
                    .findCARPCasesByDatasetAndStatusAndValidAndTimedout((CARPDataset) dataset, BaseCase.FINISHED, false, false)
                    .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                    .map(BaseCase::getUserId).collect(Collectors.toSet());
        }
        return new QueryTopResult(dataset, allBaseCases, invalidUids, user.getType() <= User.ADMIN);
    }

}

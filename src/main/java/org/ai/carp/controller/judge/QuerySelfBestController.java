package org.ai.carp.controller.judge;

import org.ai.carp.controller.exceptions.PermissionDeniedException;
import org.ai.carp.controller.util.DatasetUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.service.BaseFunction;
import org.ai.carp.service.FunctionFactory;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/judge/best")
public class QuerySelfBestController {

    public static final int COUNT_BEST = 10;

    @GetMapping
    public QuerySelfBestResult get(@RequestParam("dataset") String did, HttpSession session) {
        User user = UserUtils.getUser(session, User.USER);
        BaseDataset dataset = DatasetUtils.apiGetById(did);
        if (dataset.isFinalJudge()) {
            return new QuerySelfBestResult(QueryTopResult.getFinalList(dataset.getId()), user);
        }
        List<BaseCase> bestCases;

        try {
            BaseFunction baseFunction = FunctionFactory.getCaseFunction(dataset.getType());
            bestCases = baseFunction.getBestResult(user, dataset);
        } catch (Exception e) {
            throw new PermissionDeniedException("unknown problem type dataset");
        }
        return new QuerySelfBestResult(bestCases);
    }
}

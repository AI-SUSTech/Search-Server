package org.ai.carp.controller.admin.judge;

import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.util.DatasetUtils;
import org.ai.carp.controller.util.UserUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.judge.NCSParameter;
import org.ai.carp.service.FunctionFactory;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@RestController
public class QueryUserCaseController {

    @GetMapping
    @RequestMapping(value = "/api/admin/judge/query", params = {"user", "dataset"})
    public List<BaseCase> get(@RequestParam("user") String username, @RequestParam("dataset") String datasetname, HttpSession session) {
        UserUtils.getUser(session, User.ADMIN);
        User user = Database.getInstance().getUsers().findByUsername(username);
        if (user == null) {
            throw new InvalidRequestException("User does not exist!");
        }
        BaseDataset dataset = DatasetUtils.findByName(datasetname);
        if (dataset == null) {
            throw new InvalidRequestException("Dataset does not exist!");
        }

        List<BaseCase> baseCases;
        try {
            baseCases = FunctionFactory.getCaseFunction(dataset.getType())
                    .queryUserCaseOfDataset(user, dataset);
        } catch (Exception e) {
            throw new InvalidRequestException("unknown problem type!");
        }
        return baseCases;
    }

    @GetMapping
    @RequestMapping(value = "/api/admin/judge/query", params = {"user"})
    public List<BaseCase> getUserOnly(@RequestParam("user") String username, HttpSession session) {
        UserUtils.getUser(session, User.ADMIN);
        User user = Database.getInstance().getUsers().findByUsername(username);
        if (user == null) {
            throw new InvalidRequestException("User does not exist!");
        }
        return FunctionFactory.getAllCases(user);
    }

    @GetMapping
    @RequestMapping(value = "/api/admin/judge/query_ncs_para", params = {"user"})
    public List<NCSParameter> getNCSParameter(@RequestParam("user") String username, HttpSession session) {
        UserUtils.getUser(session, User.ADMIN);
        User user = Database.getInstance().getUsers().findByUsername(username);
        if (user == null) {
            throw new InvalidRequestException("User does not exist!");
        }
        return Database.getInstance().getNcsParameterRepository().findNCSParametersByUser(user);
    }

}

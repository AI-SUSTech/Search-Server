package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.IMPCase;
import org.ai.carp.model.user.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.types.Binary;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

public class IMPFunction implements BaseFunction {

    private static final Logger logger = LoggerFactory.getLogger(IMPFunction.class);

    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getImpCases().save((IMPCase) baseCase);
    }

    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getImpCases()
                .insert(new IMPCase(user, dataset.getId(), archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        baseCase.setValid(rootNode.get("valid").asBoolean());
        baseCase.setReason(rootNode.get("reason").asText());
        ((IMPCase) baseCase).setInfluence(rootNode.get("influence").asDouble());
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        logger.info(user.toString());
        bestCases = Database.getInstance().getImpCases()
                .findIMPCasesByDatasetIdAndUserAndStatusAndValidOrderByInfluenceDescTimeAscSubmitTimeAsc(
                        dataset.getId(), 
                        user, 
                        BaseCase.FINISHED, 
                        true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getImpCases()
                .findIMPCasesByUserAndDatasetIdOrderBySubmitTimeDesc(user, dataset.getId())
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getImpCases()
                .findIMPCasesByDatasetIdAndStatusAndValidOrderByInfluenceDescTimeAscSubmitTimeAsc(
                        dataset.getId(), BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase) c).collect(Collectors.toList());
    }

    @Override
    public Workbook getFinalGrades() {
        return null;
    }
}

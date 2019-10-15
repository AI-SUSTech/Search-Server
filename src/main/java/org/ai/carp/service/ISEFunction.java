package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.controller.util.ISEUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.ISECase;
import org.ai.carp.model.user.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.types.Binary;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ISEFunction implements BaseFunction {

    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getIseCases().save((ISECase) baseCase);
    }

    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getIseCases()
                .insert(new ISECase(user, (ISEDataset) dataset, archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        ISEUtils.checkResult((ISECase)baseCase);
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getIseCases()
                .findISECasesByDatasetAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (ISEDataset)dataset, user, BaseCase.FINISHED, true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getIseCases()
                .findISECasesByUserAndDatasetOrderBySubmitTimeDesc(user, (ISEDataset)dataset)
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getIseCases()
                .findISECasesByDatasetAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (ISEDataset)dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase)c).collect(Collectors.toList());
    }

    @Override
    public Workbook getFinalGrades() {
        return null;
    }


}

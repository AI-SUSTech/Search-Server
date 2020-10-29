package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.controller.util.CARPUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.CARPDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.CARPCase;
import org.ai.carp.model.user.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.types.Binary;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

public class CARPFunction implements BaseFunction {

    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getCarpCases().save((CARPCase) baseCase);
    }

    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getCarpCases()
                .insert(new CARPCase(user, (CARPDataset) dataset, archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        CARPUtils.checkResult((CARPCase) baseCase);
    }


    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getCarpCases()
                .findCARPCasesByDatasetAndUserAndStatusAndValidOrderByCostAscTimeAscSubmitTimeAsc(
                        (CARPDataset) dataset, user, BaseCase.FINISHED, true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return bestCases;
    }


    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getCarpCases()
                .findCARPCasesByUserAndDatasetOrderBySubmitTimeDesc(user, (CARPDataset) dataset)
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getCarpCases()
                .findCARPCasesByDatasetAndStatusAndValidOrderByCostAscTimeAscSubmitTimeAsc(
                        (CARPDataset) dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase) c).collect(Collectors.toList());
    }

    @Override
    public Workbook getFinalGrades() {
        return null;
    }

}

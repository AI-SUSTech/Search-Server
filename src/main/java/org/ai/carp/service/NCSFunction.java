package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.NCSCase;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

public class NCSFunction implements BaseFunction {
    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getNcsCases().save((NCSCase) baseCase);
    }

    /**
     *
     * @param user the user whom submit this case to judge
     * @param dataset useless, should be null
     * @param archive useless, should be null
     * @return new inserted NCSCase
     */
    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getNcsCases()
                .insert(new NCSCase(user, dataset, archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        baseCase.setValid(rootNode.get("valid").asBoolean());
        baseCase.setReason(rootNode.get("reason").asText());
        baseCase.setResult(rootNode.get("influence").asDouble());
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getNcsCases()
                .findNCSCasesByDatasetAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (NCSDataset) dataset,
                        user,
                        BaseCase.FINISHED,
                        true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getNcsCases()
                .findNCSCasesByUserAndDatasetOrderBySubmitTimeDesc(user, (NCSDataset)dataset)
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getNcsCases()
                .findNCSCasesByDatasetAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (NCSDataset)dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase)c).collect(Collectors.toList());
    }


}

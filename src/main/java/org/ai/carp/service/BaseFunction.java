package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.CARPDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;

import java.util.List;

public interface BaseFunction {

    BaseCase save(BaseCase baseCase);
    BaseCase insert(User user, BaseDataset dataset, Binary archive);
    void afterGetResult(BaseCase baseCase, JsonNode rootNode);

    List<BaseCase> getBestResult(User user, BaseDataset dataset);

    List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset);
}

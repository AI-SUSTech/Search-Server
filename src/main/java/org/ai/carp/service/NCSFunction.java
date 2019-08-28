package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.NCSCase;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;

import java.util.List;

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
                .insert(new NCSCase(user, archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {

    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        return null;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        return null;
    }


}

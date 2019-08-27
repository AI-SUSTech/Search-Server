package org.ai.carp.model.judge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.zip.ZipOutputStream;


@Document(collection = "cases_ncs")
public class NCSCase extends BaseCase {

    double result;

    public NCSCase(User user, Binary archive) {
        super(user, archive);
    }

    @Override
    public int getType() {
        return BaseDataset.NCS;
    }

    @Override
    public void setDataset(BaseDataset dataset) {

    }

    @Override
    public double getResult() {
        return result;
    }

    @Override
    public void setResult(double result) {
        this.result = result;
    }

    @Override
    protected String buildConfig() throws JsonProcessingException {
        return null;
    }

    @Override
    protected void buildDataset(ObjectNode node) {

    }

    @Override
    protected void writeData(ZipOutputStream zos) throws IOException {

    }
}

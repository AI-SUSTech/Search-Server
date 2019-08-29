package org.ai.carp.model.judge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.zip.ZipOutputStream;


@Document(collection = "cases_ncs")
public class NCSCase extends BaseCase {

    double result;
    // Submission
    @DBRef
    @Indexed
    private NCSDataset dataset;

    public NCSCase(User user, BaseDataset dataset, Binary archive) {
        super(user, archive);
        this.dataset = (NCSDataset) dataset;
    }

    @Override
    public int getType() {
        return BaseDataset.NCS;
    }

    @Override
    public void setDataset(BaseDataset dataset) {
        this.dataset = (NCSDataset) dataset;
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("entry", "test.txt");
        node.put("parameters", "-i $network -s $seeds -m $model -t $time");
        node.put("time", dataset.getTime());
        node.put("memory", dataset.getMemory());
        node.put("cpu", dataset.getCpu());
        return mapper.writeValueAsString(node);
    }

    @Override
    protected void buildDataset(ObjectNode node) {

    }

    @Override
    protected void writeData(ZipOutputStream zos) throws IOException {

    }

    public BaseDataset getDataset() {
        return dataset;
    }

    public String getDatasetName() {
        return dataset.getName();
    }
}

package org.ai.carp.model.judge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Document(collection = "cases_imp")
public class IMPCase extends BaseCase {

    private static Random random = new Random();

    // Submission
    @Indexed
    private String datasetId;

    // Result
    private double influence;

    public IMPCase(User user, String datasetId, Binary archive) {
        super(user, archive);
        this.datasetId = datasetId;
    }

    @Override
    public void setDataset(BaseDataset dataset) {
        this.datasetId = dataset.getId();
    }

    public void setInfluence(double influence) {
        this.influence = influence;
    }

    @JsonIgnore
    public IMPDataset getDataset() {
        IMPDataset dataset = Database.getInstance().getImpDatasets().findDatasetById(datasetId);
        while (dataset == null) {
            dataset = Database.getInstance().getImpDatasets().findDatasetById(datasetId);
        }
        return dataset;
    }

    @Override
    public int getType() {
        return BaseDataset.IMP;
    }

    public String getDatasetName() {
        return getDataset().getName();
    }

    @JsonIgnore
    public double getInfluence() {
        return influence;
    }

    @Override
    public double getResult() {
        return influence;
    }

    @Override
    public void setResult(double result) {
        influence = result;
    }

    @Override
    protected String buildConfig() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        IMPDataset dataset = getDataset();
        ObjectNode node = mapper.createObjectNode();
        node.put("entry", "IMP.py");
        node.put("network", "network.dat");
        node.put("parameters", "-i $network -k $seedCount -m $model -t $time");
        node.put("time", dataset.getTime());
        node.put("memory", dataset.getMemory());
        node.put("cpu", dataset.getCpu());
        node.put("seedCount", dataset.getSeedCount());
        node.put("model", dataset.getModel());
        return mapper.writeValueAsString(node);
    }

    @Override
    protected void buildDataset(ObjectNode node) {
        IMPDataset dataset = getDataset();
        node.put("network", dataset.getNetwork());
        node.put("seedCount", dataset.getSeedCount());
    }

    @Override
    protected void writeData(ZipOutputStream zos) throws IOException {
        ZipEntry data = new ZipEntry("data/network.dat");
        zos.putNextEntry(data);
        zos.write(getDataset().getNetwork().getBytes());
        zos.closeEntry();
    }

    @Override
    public String toString() {
        return String.format("IMPCase[id=%s, user=%s, dataset=%s, status=%d, influence=%f]",
                id, user.getUsername(), getDataset().getName(), status, influence);
    }
}

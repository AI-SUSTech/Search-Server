package org.ai.carp.model.judge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Document(collection = "cases_ise")
public class ISECase extends BaseCase {

    private static Random random = new Random();

    // Submission
    @Indexed
    private String datasetId;

    // Result
    private double influence;

    public ISECase(User user, String datasetId, Binary archive) {
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
    public ISEDataset getDataset() {
        return Database.getInstance().getIseDatasets().findDatasetById(datasetId);
    }

    @Override
    public int getType() {
        return BaseDataset.ISE;
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
        ObjectNode node = mapper.createObjectNode();
        ISEDataset iseDataset = getDataset();
        node.put("entry", "ISE.py");
        node.put("network", "network.dat");
        node.put("seeds", "seeds.dat");
        node.put("parameters", "-i $network -s $seeds -m $model -t $time");
        node.put("time", iseDataset.getTime());
        node.put("memory", iseDataset.getMemory());
        node.put("cpu", iseDataset.getCpu());
        node.put("model", iseDataset.getModel());
        return mapper.writeValueAsString(node);
    }

    @Override
    protected void buildDataset(ObjectNode node) {
    }

    @Override
    protected void writeData(ZipOutputStream zos) throws IOException {
        ZipEntry data = new ZipEntry("data/network.dat");
        ISEDataset iseDataset = getDataset();
        zos.putNextEntry(data);
        zos.write(iseDataset.getNetwork().getBytes());
        zos.closeEntry();
        data = new ZipEntry("data/seeds.dat");
        zos.putNextEntry(data);
        zos.write(iseDataset.getSeeds().getBytes());
        zos.closeEntry();
    }

    @Override
    public String toString() {
        return String.format("ISECase[id=%s, user=%s, dataset=%s, status=%d, influence=%f]",
                id, user.getUsername(), getDataset().getName(), status, influence);
    }
}

package org.ai.carp.model.dataset;


import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datasets_ncs")
public class NCSDataset extends BaseDataset{
    @Override
    public int getType() {
        return BaseDataset.NCS;
    }

    @Override
    public String getEntry() {
        return "parameter";
    }
}

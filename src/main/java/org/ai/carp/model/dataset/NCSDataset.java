package org.ai.carp.model.dataset;


import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datasets_ncs")
public class NCSDataset extends BaseDataset{

    private String data;

    public void setData(String content){
        this.data = content;
    }

    public NCSDataset(String name, int time, int memory, int cpu, String data) {
        super(name, time, memory, cpu);
        this.data = data;
    }


    @Override
    public int getType() {
        return BaseDataset.NCS;
    }

    @Override
    public String getEntry() {
        return "parameter";
    }
}

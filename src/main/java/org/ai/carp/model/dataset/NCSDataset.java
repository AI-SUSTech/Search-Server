package org.ai.carp.model.dataset;


import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datasets_ncs")
public class NCSDataset extends BaseDataset {

    private int problem_index;

    static final String[] data_sets = new String[30];

    static {
        data_sets[6] = "F6: Shifted Rosenbrock’s Function";
        data_sets[12] = "F12: Schwefel’s Problem";
    }

    public NCSDataset(String name, int time, int memory, int cpu, int problem_index) {
        super(name, time, memory, cpu);
        this.problem_index = problem_index;
    }

    public int getProblem_index() {
        return problem_index;
    }

    public void setProblem_index(int problem_index) {
        this.problem_index = problem_index;
    }

    @Override
    public int getType() {
        return BaseDataset.NCS;
    }

    @Override
    public String getEntry() {
        return "parameter.json";
    }
}

package org.ai.carp.model.judge;


import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.user.User;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ncs_parameter")
public class NCSParameter {

    public NCSParameter(User user) {
        this.user = user;
    }
    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public int getN() {
        return N;
    }

    public void setN(int n) {
        N = n;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public void setR(Double r) {
        this.r = r;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public void setN(Integer n) {
        N = n;
    }

    public void generateHash() {
        int hash = 0;
        hash += epoch.hashCode();
        hash += r.hashCode();
        hash += N.hashCode();
        hash += lambda.hashCode();
        this.hash = hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if(obj instanceof NCSParameter){
            NCSParameter anObj = (NCSParameter)obj;
            if(!anObj.lambda.equals(this.lambda))
                return false;
            if(!anObj.r.equals(this.r))
                return false;
            if(!anObj.epoch.equals(this.epoch))
                return false;
            return anObj.N.equals(this.N);
        }
        return false;
    }

    @Indexed
    protected int hash;

//    @Id
//    @AutoValue
//    protected String id;

    @DBRef
    @Indexed
    protected User user;

    public Double getLambda() {
        return lambda;
    }

    public void setLambda(Double lambda) {
        this.lambda = lambda;
    }

    public NCSDataset getDataset() {
        return dataset;
    }

    public void setDataset(NCSDataset dataset) {
        this.dataset = dataset;
    }

    @DBRef
    @Indexed
    protected NCSDataset dataset;

    private Double lambda;

    private Double r;

    private Integer epoch;
    private Integer N;

}

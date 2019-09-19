package org.ai.carp.model.judge;


import org.ai.carp.model.user.User;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ncs_parameter")
public class NCSParameter {

    public NCSParameter(User user) {
        this.user = user;
    }

    public int getMaxT() {
        return maxT;
    }

    public void setMaxT(int maxT) {
        this.maxT = maxT;
    }

    public Object getSigma() {
        return sigma;
    }

    public void setSigma(Object sigma) {
        this.sigma = sigma;
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

    @Id
    protected String id;

    @DBRef
    @Indexed
    protected User user;

    private int maxT;
    private Object sigma;

    private double r;

    private int epoch;
    private int N;

}

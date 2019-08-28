package org.ai.carp.service;

import org.ai.carp.model.dataset.BaseDataset;

public class FunctionFactory {

    private static CARPFunction carpFunction;
    private static IMPFunction impFunction;
    private static ISEFunction iseFunction;
    private static NCSFunction ncsFunction;

    static {
        carpFunction = new CARPFunction();
        impFunction = new IMPFunction();
        iseFunction = new ISEFunction();
        ncsFunction = new NCSFunction();
    }



    public static BaseFunction getCaseFunction(int problemType) throws Exception {
        switch (problemType){
            case BaseDataset
                    .CARP:
                return carpFunction;
            case BaseDataset
                    .IMP:
                return impFunction;
            case BaseDataset
                    .ISE:
                return iseFunction;
            case BaseDataset
                    .NCS:
                return ncsFunction;
        }
        throw new Exception("unknown problem type");
    }



}

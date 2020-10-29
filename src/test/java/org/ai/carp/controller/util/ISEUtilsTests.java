package org.ai.carp.controller.util;

import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.ISECase;
import org.junit.Test;

import java.io.IOException;

public class ISEUtilsTests {

    @Test
    public void testCheckResult() throws IOException {
        ISEDataset dataset = new ISEDataset("test", 10, 256, 1, "IC",
                ResourceUtils.readResource("network.txt"),
                ResourceUtils.readResource("seeds.txt"), 5, 0.01);
        System.out.println(dataset.getId());
        ISECase iseCase = new ISECase(null, "test_dataset_id", null);
        iseCase.setStatus(ISECase.FINISHED);
        iseCase.setExitcode(0);
        String data = ResourceUtils.readResource("network_ise_ic.txt");
        iseCase.setStdout(data);
        checkResult(iseCase, dataset);
        // TODO: assertions
    }

    private static void checkResult(ISECase iseCase, ISEDataset dataset) {
        if (!CaseUtils.checkResult(iseCase)) {
            return;
        }
        double stdInfluence = dataset.getInfluence();
        double bias = dataset.getBias();
        String stdout = iseCase.getStdout();
        String firstLine = stdout.replaceAll("\r", "").split("\n")[0];
        try {
            iseCase.setInfluence(Double.valueOf(firstLine));
            boolean result = checkIsLargeBias(stdInfluence, iseCase.getInfluence(), bias);
            iseCase.setValid(!result);
            if (result) {
                iseCase.setReason("Bias Too Large");
            } else {
                iseCase.setReason("Solution accepted");
            }
        } catch (NumberFormatException ignored) {
            iseCase.setValid(false);
            iseCase.setReason("Invalid number format");
        }
    }

    private static boolean checkIsLargeBias(double standard, double student, double toleranceRatio) {
        double max_bias = standard * toleranceRatio;
        double bias = Math.abs(standard - student);
        return bias > max_bias;
    }

}

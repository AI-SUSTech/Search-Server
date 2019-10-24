package org.ai.carp.controller.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.model.judge.NCSParameter;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;

import java.io.IOException;
import java.util.Base64;

public class ParameterFileUtils {
    public static Binary convertSubmit(String encoded) {
        if (encoded.length() > 87500) {
            throw new InvalidRequestException("Files size exceeds 64KB!");
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        return new Binary(bytes);
    }

    private static NCSParameter checkParameterRange(NCSParameter parameter){
        if(parameter.getLambda() <= 0 || parameter.getLambda() >= 10){
            throw new InvalidRequestException("require an float lambda in (0, 10)!");
        }
        if(parameter.getR() <= 0){
            throw new InvalidRequestException("r should be not negative!");
        }
        if(parameter.getN()<1 || parameter.getN()>100){
            throw new InvalidRequestException("require an integer in [1, 100]!");
        }
        if(parameter.getEpoch() > 300000 || parameter.getEpoch() <= 0)
            throw new InvalidRequestException("epoch do not valid!");
        parameter.generateHash();
        return parameter;
    }

    public static NCSParameter checkSubmitPara(User user, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = mapper.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidRequestException("not a json format file!");
        }
        JsonNode jsonNode;
        jsonNode = rootNode.get("lambda");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an float lambda in (0, 10)!");
        }
        double lambda = jsonNode.asDouble();

        jsonNode = rootNode.get("r");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require a float r!");
        }
        double r = jsonNode.asDouble();

        jsonNode = rootNode.get("epoch");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an integer epoch < 300000!");
        }
        int epoch = jsonNode.asInt();

        jsonNode = rootNode.get("n");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an integer n in [1, 100]!");
        }
        int n = jsonNode.asInt();

        NCSParameter ncsParameter = new NCSParameter(user);
        ncsParameter.setEpoch(epoch);
        ncsParameter.setR(r);
        ncsParameter.setN(n);
        ncsParameter.setLambda(lambda);
        return checkParameterRange(ncsParameter);
    }
}

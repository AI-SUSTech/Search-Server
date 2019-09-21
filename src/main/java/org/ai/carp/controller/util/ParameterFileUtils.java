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

    public static NCSParameter checkSubmitPara(User user, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = mapper.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidRequestException("not a json format file!");
        }
        JsonNode jsonNode;
        jsonNode = rootNode.get("Tmax");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an integer Tmax!");
        }
        int Tmax = jsonNode.asInt();

        jsonNode = rootNode.get("sigma");
        if(jsonNode == null || !jsonNode.isArray()){
            throw new InvalidRequestException("require an one dimension array sigma");
        }
        double[] sigmaArray = new double[jsonNode.size()];
        for (int i = 0; i < sigmaArray.length; i++) {
            JsonNode sigmaNode = jsonNode.get(i);
            if(sigmaNode.isNumber())
                sigmaArray[i] = sigmaNode.asDouble();
            else
                throw new InvalidRequestException("array sigma contains non-number data!");
        }

        jsonNode = rootNode.get("r");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require a float r!");
        }
        double r = jsonNode.asDouble();

        jsonNode = rootNode.get("epoch");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an integer epoch!");
        }
        int epoch = jsonNode.asInt();

        jsonNode = rootNode.get("n");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an integer n!");
        }
        int n = jsonNode.asInt();

        NCSParameter ncsParameter = new NCSParameter(user);
        ncsParameter.setEpoch(epoch);
        ncsParameter.setR(r);
        ncsParameter.setMaxT(Tmax);
        ncsParameter.setN(n);
        ncsParameter.setSigma(sigmaArray);
        ncsParameter.generateHash();
        return ncsParameter;
    }
}

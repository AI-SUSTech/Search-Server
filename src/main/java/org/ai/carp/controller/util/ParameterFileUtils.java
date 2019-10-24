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
        jsonNode = rootNode.get("lambda");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require an float lambda in (0, 10)!");
        }
        double lambda = jsonNode.asDouble();
	if(lambda <= 0 || lambda >= 10){
		throw new InvalidRequestException("require an float lambda in (0, 10)!");
	}

        jsonNode = rootNode.get("r");
        if(jsonNode == null || !jsonNode.isNumber()){
            throw new InvalidRequestException("require a float r!");
        }
        double r = jsonNode.asDouble();
	if(r <= 0){
		throw new InvalidRequestException("r should be not negative!");
	}  

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
	if(n<1 || n>100){
		throw new InvalidRequestException("require an interger in [1, 100]!");
	}
	if(epoch > 300000)
		throw new InvalidRequestException("epoch do not valid!");

        NCSParameter ncsParameter = new NCSParameter(user);
        ncsParameter.setEpoch(epoch);
        ncsParameter.setR(r);
        ncsParameter.setN(n);
        ncsParameter.setLambda(lambda);
        ncsParameter.generateHash();
        return ncsParameter;
    }
}

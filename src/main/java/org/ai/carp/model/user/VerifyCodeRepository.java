package org.ai.carp.model.user;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VerifyCodeRepository extends MongoRepository<VerifyCode, String> {

   VerifyCode findTopByUserOrderByGenerateTimeDesc(User user);


}

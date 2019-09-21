package org.ai.carp.model.judge;

import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NCSParameterRepository extends MongoRepository<NCSParameter, String> {

    List<NCSParameter> findNCSParametersByDatasetAndHashAndUserNot(
            NCSDataset dataset,
            int hash,
            User user
    );
}

package org.ai.carp.model.dataset;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface NCSDatasetRepository extends MongoRepository<NCSDataset, String> {

    NCSDataset findDatasetByName(String name);
}

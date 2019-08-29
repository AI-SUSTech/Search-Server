package org.ai.carp.model.judge;

import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NCSCaseRepository extends MongoRepository<NCSCase, String> {


    List<NCSCase> findNCSCasesByDatasetAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
            NCSDataset dataset,
            User user,
            int status,
            boolean valid,
            Pageable pageable
    );

    List<NCSCase> findNCSCasesByUserAndDatasetOrderBySubmitTimeDesc(User user, NCSDataset dataset);

    List<NCSCase> findNCSCasesByDatasetAndStatusAndValidOrderByTimeAscSubmitTimeAsc(NCSDataset dataset, int finished, boolean b);
}

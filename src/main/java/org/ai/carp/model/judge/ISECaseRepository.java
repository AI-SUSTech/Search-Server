package org.ai.carp.model.judge;

import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ISECaseRepository extends MongoRepository<ISECase, String> {

    List<ISECase> findISECasesByUserOrderBySubmitTimeDesc(User user, Pageable pageable);

    int countCARPCasesByUser(User user);

    ISECase findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(User user, Date endTime);
    
    List<ISECase> findISECasesByUserAndSubmitTimeAfter(User user, Date endTime);

    List<ISECase> findISECasesBySubmitTimeAfterAndDatasetId(Date endTime, String datasetId);

    List<ISECase> findISECasesByDatasetIdOrderBySubmitTimeDesc(String datasetId);

    List<ISECase> findISECasesByUserOrderBySubmitTimeDesc(User user);

    List<ISECase> findISECasesByUserAndDatasetIdOrderBySubmitTimeDesc(User user, String datasetId);

    List<ISECase> findISECasesByStatusNotIn(List<Integer> status, Pageable pageable);

    List<ISECase> findISECasesByDatasetIdAndStatusAndValidAndTimedout(String datasetId, int status, boolean valid, boolean timedout);

    List<ISECase> findISECasesByDatasetIdAndStatusAndValidOrderByTimeAscSubmitTimeAsc(String datasetId, int status, boolean valid);

    List<ISECase> findISECasesByDatasetIdAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(String datasetId, User user, int status, boolean valid, Pageable pageable);

    int countISECasesByUserAndSubmitTimeAfter(User user, Date startTime);

    List<ISECase> findISECasesByDatasetIdAndStatus(String datasetId, int status);

    List<ISECase> findISECasesByStatus(int status);
}

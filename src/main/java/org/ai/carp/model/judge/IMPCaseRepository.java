package org.ai.carp.model.judge;

import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface IMPCaseRepository extends MongoRepository<IMPCase, String> {

    List<IMPCase> findIMPCasesByUserOrderBySubmitTimeDesc(User user, Pageable pageable);

    int countIMPCasesByUser(User user);

    IMPCase findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(User user, Date endTime);

    IMPCase findFirstByUserAndSubmitTimeBeforeAndValidOrderBySubmitTimeDesc(User user, Date endTime, boolean valid);


    List<IMPCase> findIMPCasesByDatasetIdOrderBySubmitTimeDesc(String datasetId);

    List<IMPCase> findIMPCasesByUserOrderBySubmitTimeDesc(User user);

    List<IMPCase> findIMPCasesByUserAndDatasetIdOrderBySubmitTimeDesc(User user, String datasetId);

    List<IMPCase> findIMPCasesByStatusNotIn(List<Integer> status, Pageable pageable);

    List<IMPCase> findIMPCasesByDatasetIdAndStatusAndValidAndTimedout(String datasetId, int status, boolean valid, boolean timedout);

    List<IMPCase> findIMPCasesByDatasetIdAndStatusAndValidOrderByInfluenceDescTimeAscSubmitTimeAsc(String datasetId, int status, boolean valid);

    List<IMPCase> findIMPCasesByDatasetIdAndUserAndStatusAndValidOrderByInfluenceDescTimeAscSubmitTimeAsc(String datasetId, User user, int status, boolean valid, Pageable pageable);

    int countIMPCasesByUserAndSubmitTimeAfter(User user, Date startTime);

}

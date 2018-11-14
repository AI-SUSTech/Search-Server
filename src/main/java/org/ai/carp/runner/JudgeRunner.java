package org.ai.carp.runner;

import org.ai.carp.model.judge.CARPCase;
import org.ai.carp.model.judge.CARPCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@AutoConfigureAfter(CARPCaseRepository.class)
public class JudgeRunner {

    private static final Logger logger = LoggerFactory.getLogger(JudgeRunner.class);

    public static BlockingQueue<CARPCase> queue = new LinkedBlockingQueue<>();

    private CARPCaseRepository carpCases;

    @Autowired
    private void setCarpCases(CARPCaseRepository carpCases) {
        this.carpCases = carpCases;
    }

    @Async
    public Future start() throws InterruptedException {
        logger.info("Judge runner started");
        try {
            while (true) {
                CARPCase c = queue.poll(5, TimeUnit.SECONDS);
                if (c != null) {
                    // Dispatch job
                    try {
                        String encodedCase = c.getWorkerJson();
                        String worker = JudgePool.getInstance().dispatchJob(c.getId(), encodedCase);
                        synchronized (JudgePool.getInstance()) {
                            while (worker == null) {
                                logger.info("No worker available for judging, {} remains", queue.size() + 1);
                                JudgePool.getInstance().wait(10000);
                                worker = JudgePool.getInstance().dispatchJob(c.getId(), encodedCase);
                            }
                        }
                        logger.info("Case {} dispatched to worker {}: {} - {}", c.getId(), worker, c.getUser().getUsername(), c.getDataset().getName());
                    } catch (IOException e) {
                        logger.error("Case {} is broken", c.getId());
                        // TODO: Handle invalid cases
                        c.setStatus(CARPCase.ERROR);
                        c.setReason("Case is broken.");
                        carpCases.save(c);
                    }
                } else {
                    // Check for dead jobs
                    List<Integer> finishedStatus = new ArrayList<>();
                    finishedStatus.add(CARPCase.FINISHED);
                    finishedStatus.add(CARPCase.ERROR);
                    List<CARPCase> deadCases = carpCases.findCARPCasesByStatusNotIn(finishedStatus);
                    if (deadCases.isEmpty()) {
                        continue;
                    }
                    Map<String, CARPCase> deadCasesMap = new HashMap<>();
                    for (CARPCase carpCase : deadCases) {
                        deadCasesMap.put(carpCase.getId(), carpCase);
                    }
                    for (JudgeWorker worker : JudgePool.getInstance().getWorkers()) {
                        for (CARPCase carpCase : worker.jobs) {
                            deadCasesMap.remove(carpCase.getId());
                        }
                    }
                    for (CARPCase inQueue : queue) {
                        deadCasesMap.remove(inQueue.getId());
                    }
                    if (deadCasesMap.size() <= 0) {
                        continue;
                    }
                    for (CARPCase deadCase : deadCasesMap.values()) {
                        deadCase.setStatus(CARPCase.WAITING);
                        CARPCase saved = carpCases.save(deadCase);
                        queue.add(saved);
                    }
                    logger.info("Restart {} dead cases.", deadCasesMap.size());
                }
            }
        } catch (RuntimeException e) {
            logger.error("Exception thrown", e);
            throw e;
        }
    }
}

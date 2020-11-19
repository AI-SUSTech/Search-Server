package org.ai.carp;

import org.ai.carp.model.Deadline;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.ISECase;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.LiteCase;
import org.ai.carp.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class ISEJudgeFinal {

    private static final Logger logger = LoggerFactory.getLogger(ISEJudgeFinal.class);

    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(ISEJudgeFinal.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        // exportFinalJudge();
//        disableDatasets();
        // addDatasets();
//        addCases();
        addUserCases("11810305");
        // fixSwap();
        // addCaseBySubmitIdAndTime("5dc52181e788581b27462dbd", "2019-11-08T08:04:17.185+0000");//this is for ISE
    }


    private static void fixSwap() {
        Database.getInstance().getIseDatasets().findAll().stream()
                .filter(d -> d.getName().contains("random"))
                .forEach(d -> {
                    List<ISECase> iseCases = Database.getInstance().getIseCases()
                            .findISECasesByDatasetIdAndStatusAndValidOrderByTimeAscSubmitTimeAsc(d.getId(), BaseCase.FINISHED, false);
                    for (ISECase iseCase : iseCases) {
                        iseCase.setStatus(BaseCase.WAITING);
                        logger.info(Database.getInstance().getIseCases().save(iseCase).toString() + iseCase.getReason());
                    }
                });
    }


    private static void disableDatasets() {
        Database.getInstance().getIseDatasets().findAll().forEach(c -> {
            if (c.getName().contains("random")) {
                Database.getInstance().getIseDatasets().delete(c);
                logger.info("drop:" + c.toString());
                return;
            }
            c.setEnabled(false);
            Database.getInstance().getIseDatasets().save(c);
            logger.info(c.toString());
        });
    }

    private static void addDatasets() throws FileNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("datasets_ise_final.csv");
        if (is == null) {
            logger.error("datasets_ise_final.csv not found");
            return;
        }
        Scanner scanner = new Scanner(is);
        String datasetPath = classLoader.getResource("datasets_ise_final").getPath();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\r", "");
            String[] splitted = line.split(",");
            if (StringUtils.isEmpty(splitted[0])) {
                continue;
            }
            String network = new Scanner(new File(datasetPath + "/" + splitted[0] + ".txt"))
                    .useDelimiter("\\Z").next().replace("\r", "");
            String seeds = new Scanner(new File(datasetPath + "/" + splitted[1] + ".txt"))
                    .useDelimiter("\\Z").next().replace("\r", "");
            String name = String.format("%s-%s-%s", splitted[0], splitted[1], splitted[2]);
            ISEDataset dataset = new ISEDataset(name, Integer.valueOf(splitted[3])
                    , Integer.valueOf(splitted[4]), Integer.valueOf(splitted[5])
                    , splitted[2], network, seeds, Double.valueOf(splitted[6]), Double.valueOf(splitted[7]));
            dataset.setEnabled(true);
            dataset.setSubmittable(false);
            dataset.setFinalJudge(true);
            dataset = Database.getInstance().getIseDatasets().insert(dataset);
            logger.info(dataset.toString());
        }
    }

    private static void addCaseBySubmitIdAndTime(String submitId, String date) {
        Date endTime = Deadline.getIseDDL();
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream().filter(BaseDataset::isFinalJudge).collect(Collectors.toList());

        List<ISECase> cases = new ArrayList<>();

        ISECase submission = (ISECase) Database.getInstance().getLiteCases()
                .findLiteCaseByFullId(submitId).getFullCase();
        if (submission == null || submission.getArchive() == null) {
            logger.info("not submission found id:" + submitId);
            return;
        } else {
            logger.info("found:" + submitId);
            logger.info("detail:" + submission.toString());
            logger.info("submit time:" + submission.getSubmitTime().toString());
            logger.info("is before ddl:" + (submission.getSubmitTime().getTime() < endTime.getTime()));
            logger.info("you said it submit in:" + date + ", do you want to rejudge it?");

            logger.info("rejudge start!");
        }

        User u = submission.getUser();

        for (ISEDataset dataset : datasets) {
            //remove previous case
            List<ISECase> isecases = Database.getInstance().getIseCases()
                    .findISECasesByUserAndDatasetIdOrderBySubmitTimeDesc(u, dataset.getId());
            Database.getInstance().getIseCases().deleteAll(isecases);
            logger.info(String.format("remove %d isecase of %s:%s", isecases.size(), u.getUsername(), dataset.getName()));

            for (int i = 0; i < 5; i++) {
                cases.add(new ISECase(u, dataset.getId(), submission.getArchive()));
            }
        }
        Collections.shuffle(cases);
        for (ISECase c : cases) {
            ISECase newC = Database.getInstance().getIseCases().insert(c);
            Database.getInstance().getLiteCases().insert(new LiteCase(c));
            logger.info(newC.toString());
        }

    }

    private static void addUserCases(String userName) {
        Date endTime = Deadline.getIseDDL();
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream().filter(BaseDataset::isFinalJudge).collect(Collectors.toList());
        // Query users
        User u = Database.getInstance().getUsers().findByUsername(userName);
        if (u == null) {
            logger.info("user not found:" + userName);
            return;
        }
        List<ISECase> cases = new ArrayList<>();
        ISECase submission = Database.getInstance().getIseCases()
                .findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(u, endTime);
        if (submission == null || submission.getArchive() == null) {
            return;
        }
        for (ISEDataset dataset : datasets) {
            //remove previous case
            List<ISECase> isecases = Database.getInstance().getIseCases()
                    .findISECasesByUserAndDatasetIdOrderBySubmitTimeDesc(u, dataset.getId());
            for (int i = 0; i < isecases.size(); i++) {
                isecases.get(i).setStatus(0);
                Database.getInstance().getIseCases().save(isecases.get(i));
                logger.info(isecases.get(i).toString());
            }

            // Database.getInstance().getIseCases().deleteAll(isecases);
            logger.info(String.format("remove %d isecase of %s:%s", isecases.size(), u.getUsername(), dataset.getName()));

            for (int i = 0; i < 5; i++) {
                cases.add(new ISECase(u, dataset.getId(), submission.getArchive()));
            }
        }
        // Collections.shuffle(cases);
        // for (ISECase c : cases) {
        //     ISECase newC = Database.getInstance().getIseCases().insert(c);
        //     Database.getInstance().getLiteCases().insert(new LiteCase(c));
        //     logger.info(newC.toString());
        // }
    }

    private static void addCases() {
        Date endTime = Deadline.getIseDDL();
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream().filter(BaseDataset::isFinalJudge).collect(Collectors.toList());
        // Query users
        List<User> users = Database.getInstance().getUsers().findAllByType(User.USER);
        List<ISECase> cases = new ArrayList<>();
        for (User u : users) {
            ISECase submission = Database.getInstance().getIseCases()
                    .findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(u, endTime);
            if (submission == null || submission.getArchive() == null) {
                logger.info("Record not found: " + u.getUsername());
                continue;
            }
            for (ISEDataset dataset : datasets) {
                for (int i = 0; i < 5; i++) {
                    cases.add(new ISECase(u, dataset.getId(), submission.getArchive()));
                }
            }
        }
        Collections.shuffle(cases);
        for (ISECase c : cases) {
            ISECase newC = Database.getInstance().getIseCases().insert(c);
            Database.getInstance().getLiteCases().insert(new LiteCase(c));
            logger.info(newC.toString());
        }
    }

    private static void exportFinalJudge() throws IOException {
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream().filter(BaseDataset::isFinalJudge).collect(Collectors.toList());

        Date judgeTime = new Date(120, 10, 17, 4, 47);
        HashMap<String, Set<String>> basicFinal = new HashMap<>();
        HashMap<String, Set<String>> bonusFinal = new HashMap<>();
        for (ISEDataset iseDataset : datasets) {
            HashMap<String, List<ISECase>> userISECasesMap = new HashMap<>();
            String datasetName = iseDataset.getName();
            List<ISECase> iseCases = Database.getInstance().getIseCases().findISECasesBySubmitTimeAfterAndDatasetId(judgeTime, iseDataset.getId());
            for (ISECase iseCase : iseCases) {
                if (!userISECasesMap.containsKey(iseCase.getUser().getUsername())) {
                    userISECasesMap.put(iseCase.getUser().getUsername(), new ArrayList<>());
                }
                userISECasesMap.get(iseCase.getUser().getUsername()).add(iseCase);

                if (!basicFinal.containsKey(iseCase.getUser().getUsername())) {
                    basicFinal.put(iseCase.getUser().getUsername(), new HashSet<>());
                }
                if (!bonusFinal.containsKey(iseCase.getUser().getUsername())) {
                    bonusFinal.put(iseCase.getUser().getUsername(), new HashSet<>());
                }
                HashMap<String, Set<String>> finalMap = datasetName.contains("random-graph50000-50000") ? bonusFinal : basicFinal;
                if (iseCase.getReason().equals("Solution accepted")) {
                    finalMap.get(iseCase.getUser().getUsername()).add(datasetName);
                }
                // logger.info(iseCase.toString());
            }
            try {
                FileWriter writer = new FileWriter(datasetName + ".csv");
                writer.write(getGraphHeader() + "\n");
                userISECasesMap.forEach((username, cases) -> {
                    try {
                        writer.write(graphCaseContent(username, cases) + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                logger.info(String.format("Write file %s down", datasetName));
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        FileWriter finalFile = new FileWriter("final.csv");
        finalFile.write("ID,valid,basic,bonus\n");
        basicFinal.forEach((username, finalSet) -> {
            if (!bonusFinal.containsKey(username)) {
                logger.error("username not found: " + username);
            }
            int valid = bonusFinal.get(username).size() + finalSet.size();
            String content = String.format("%s,%d,%d,%d\n", username, valid, finalSet.size(), bonusFinal.get(username).size());
            try {
                finalFile.write(content + "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        finalFile.close();
    }

    private static String getGraphHeader() {
        String res = "ID";
        for (int i = 1; i <= 5; i++) {
            res += String.format(",%d,Time,Reason", i);
        }
        return res;
    }

    private static String graphCaseContent(String userName, List<ISECase> cases) {
        String res = userName;
        for (ISECase iseCase : cases) {
            res += String.format(",%s,%s,%s", iseCase.getInfluence(), iseCase.getTime(), iseCase.getReason());
        }
        return res;
    }


    private static String finalHeader() {
        return "ID,valid,basic,bonus";
    }

}

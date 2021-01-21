package org.ai.carp;

import org.ai.carp.controller.util.ParameterFileUtils;
import org.ai.carp.model.Deadline;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.IMPCase;
import org.ai.carp.model.judge.ISECase;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.LiteCase;
import org.ai.carp.model.user.User;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class ISEJudgeFinal {

    private static final Logger logger = LoggerFactory.getLogger(ISEJudgeFinal.class);

    private static final String lateSubmissionISEFolderName = "lateSubmissionISE";
    private static final String iseFinalResultFileName = "iseFinalResult.csv";

    private static final String iseFinalDatasetCsv = "datasets_ise_final.csv";
    private static final String iseFinalDatasetFolder = "datasets_ise_final";

    private static final String[] bonusDatasetsName = {"random-graph50000-50000"};
    private static final int casesNumber = 5;


    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(ISEJudgeFinal.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        exportLateSubmissionGrade();
//        addLateSubmissionCases();
//        exportFinalJudge();
//        disableDatasets();
//        addDatasets();
//        addCases();
    }

    private static void disableDatasets() {
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll();
        for (int i = 0; i < datasets.size(); i++) {
            ISEDataset c = datasets.get(i);
            if (c.getName().contains("random")) {
                List<ISECase> cases = Database.getInstance().getIseCases().
                        findISECasesByDatasetIdOrderBySubmitTimeDesc(c.getId());
                deleteCases(cases);
                Database.getInstance().getIseDatasets().delete(c);
                logger.info("drop: " + c.toString());
                continue;
            }
            c.setEnabled(false);
            Database.getInstance().getIseDatasets().save(c);
            logger.info("Disable: " + c.toString());
        }
    }

    private static void addDatasets() throws FileNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(iseFinalDatasetCsv);
        if (is == null) {
            logger.error(String.format("%s not found", iseFinalDatasetCsv));
            return;
        }
        Scanner scanner = new Scanner(is);
        String datasetPath = classLoader.getResource(iseFinalDatasetFolder).getPath();
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
            String name = String.format("%s-%s-%s-new", splitted[0], splitted[1], splitted[2]);
            ISEDataset dataset = new ISEDataset(name, Integer.parseInt(splitted[3])
                    , Integer.parseInt(splitted[4]), Integer.parseInt(splitted[5])
                    , splitted[2], network, seeds, Double.parseDouble(splitted[6]), Double.parseDouble(splitted[7]));
            dataset.setEnabled(true);
            dataset.setSubmittable(false);
            dataset.setFinalJudge(true);
            dataset = Database.getInstance().getIseDatasets().insert(dataset);
            logger.info(dataset.toString());
        }
    }

    private static void addCases() {
        Date endTime = Deadline.getIseDDL();
        // Query datasets
        List<ISEDataset> datasets = Database
                .getInstance()
                .getIseDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        datasets.forEach(c -> {
            logger.info(c.toString());
        });
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

    private static boolean isBonusDataset(String datasetName) {
        for (String s : bonusDatasetsName) {
            if (datasetName.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getLateSubmissionList() {
        List<ISEDataset> datasets = Database
                .getInstance()
                .getIseDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File lateSubmissionISEFolder = new File(Objects.requireNonNull(classLoader.getResource(lateSubmissionISEFolderName)).getPath());
        File[] lateSubmissionISEs = lateSubmissionISEFolder.listFiles();
        List<String> lateSubmissionUsers = new ArrayList<>();
        for (File lateSubmissionISE : lateSubmissionISEs) {
            String fileName = lateSubmissionISE.getName();
            String username = fileName.substring(0, fileName.lastIndexOf("."));
            lateSubmissionUsers.add(username);
        }
        return lateSubmissionUsers;
    }

    private static boolean isLateSubmissionUser(List<String> users, String username) {
        for (String user : users) {
            if (user.equals(username)) {
                return true;
            }
        }
        return false;
    }

    private static void exportFinalJudge() throws IOException {
        exportJudgeResultAfterTime(Deadline.getIseDDL());
    }

    private static String getGraphHeader() {
        String res = "ID";
        for (int i = 1; i <= 5; i++) {
            res += String.format(",%d,Time,Reason", i);
        }
        return res;
    }

    private static String graphCaseContent(String userName, List<ISECase> cases) {
        StringBuilder res = new StringBuilder(userName);
        for (ISECase iseCase : cases) {
            res.append(String.format(",%s,%s,%s", iseCase.getInfluence(), iseCase.getTime(), iseCase.getReason()));
        }
        return res.toString();
    }

    private static String finalHeader() {
        return "ID,valid,basic,bonus";
    }

    private static void deleteAllCases() {
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        datasets.forEach(dataset -> {
            List<ISECase> cases = Database.getInstance().getIseCases().findISECasesByDatasetIdOrderBySubmitTimeDesc(dataset.getId());
            deleteCases(cases);
        });
    }

    private static void deleteCases(List<ISECase> cases) {
        cases.forEach(ISEJudgeFinal::deleteCase);
    }

    private static void deleteCase(ISECase oneCase) {
        LiteCase liteCase = Database.getInstance().getLiteCases().findLiteCaseByFullId(oneCase.getId());
        Database.getInstance().getIseCases().delete(oneCase);
        Database.getInstance().getLiteCases().delete(liteCase);
        logger.info("Remove cases: " + oneCase.toString());
    }

    private static void addLateSubmissionCases() throws IOException {
        List<ISEDataset> datasets = Database
                .getInstance()
                .getIseDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File lateSubmissionISEFolder = new File(Objects.requireNonNull(classLoader.getResource(lateSubmissionISEFolderName)).getPath());
        File[] lateSubmissionISEs = lateSubmissionISEFolder.listFiles();

        List<ISECase> cases = new ArrayList<>();

        for (File lateSubmissionISE : lateSubmissionISEs) {
            String fileName = lateSubmissionISE.getName();
            String username = fileName.substring(0, fileName.lastIndexOf("."));

            User user = Database.getInstance().getUsers().findByUsername(username);
            if (user == null) {
                continue;
            }

            byte[] buffer = Files.readAllBytes(lateSubmissionISE.toPath());
            String base64 = Base64.getEncoder().encodeToString(buffer);
            Binary binary = ParameterFileUtils.convertSubmit(base64);

            datasets.forEach(d -> {
                for (int i = 0; i < 5; i++) {
                    cases.add(new ISECase(user, d.getId(), binary));
                }
            });
        }

        Collections.shuffle(cases);
        cases.forEach(c -> {
            ISECase newC = Database.getInstance().getIseCases().insert(c);
            Database.getInstance().getLiteCases().insert(new LiteCase(c));
            logger.info(newC.toString());
        });
    }

    private static void exportJudgeResultAfterTime(Date judgeTime) throws IOException {
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());

        HashMap<String, Set<String>> basicFinal = new HashMap<>();
        HashMap<String, Set<String>> bonusFinal = new HashMap<>();
        for (ISEDataset iseDataset : datasets) {
            HashMap<String, List<ISECase>> userISECasesMap = new HashMap<>();
            String datasetName = iseDataset.getName();
            List<ISECase> iseCases = Database.getInstance().getIseCases().
                    findISECasesBySubmitTimeAfterAndDatasetId(judgeTime, iseDataset.getId());
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
                HashMap<String, Set<String>> finalMap = isBonusDataset(datasetName) ? bonusFinal : basicFinal;
                if (iseCase.getReason().equals("Solution accepted")) {
                    finalMap.get(iseCase.getUser().getUsername()).add(datasetName);
                }
            }


            try {
                FileWriter writer = new FileWriter(String.format("%s.csv", datasetName));
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


        FileWriter finalFile = new FileWriter(iseFinalResultFileName);
        finalFile.write(String.format("%s\n", finalHeader()));
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

    private static void exportLateSubmissionGrade() throws IOException {
        Date judgeTime = new Date(2021 - 1900, Calendar.JANUARY, 20);
        exportJudgeResultAfterTime(judgeTime);
    }
}

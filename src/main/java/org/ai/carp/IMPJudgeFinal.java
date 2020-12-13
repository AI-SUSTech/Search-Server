package org.ai.carp;

import org.ai.carp.controller.util.ParameterFileUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.judge.IMPCase;
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
import java.util.*;
import java.util.stream.Collectors;

import org.ai.carp.model.Deadline;


@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class IMPJudgeFinal {

    private static final Logger logger = LoggerFactory.getLogger(IMPJudgeFinal.class);
    private static final int casesNumber = 5;

    private static final String lateSubmissionIMPFolderName = "lateSubmissionIMP";
    private static final String impFinalResultFileName = "impFinalResult.csv";

    private static final String impFinalDatasetCsv = "datasets_imp_final.csv";
    private static final String impFinalDatasetFolder = "datasets_imp_final";

    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(IMPJudgeFinal.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
//        addInsufficientCases();
//        addLateSubmissionCases();
//        deleteAllCases();
//        exportFinalJudge();
//        disableDatasets();
//        addDatasets();
//        addFinalCases();
    }

    /**
     * If there are some datasets named with "random" (in fact it should be the final dataset):
     *      remove all of the cases about them and drop the dataset
     * Else:
     *      set unable.
     */
    private static void disableDatasets() {
        Database.getInstance().getImpDatasets().findAll().forEach(c -> {
            if (c.getName().contains("random")) {
                List<IMPCase> cases = Database.getInstance().getImpCases().
                        findIMPCasesByDatasetIdOrderBySubmitTimeDesc(c.getId());

                deleteCases(cases);
                Database.getInstance().getImpDatasets().delete(c);
                logger.info("drop:" + c.toString() + "drop this type case:" + cases.size());
                return;
            }
            c.setEnabled(false);
            Database.getInstance().getImpDatasets().save(c);
            logger.info(c.toString());
        });
    }

    /**
     * Add final judge datasets
     * @throws FileNotFoundException
     */
    private static void addDatasets() throws FileNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(impFinalDatasetCsv);
        if (is == null) {
            logger.error(String.format("%s not found", impFinalDatasetCsv));
            return;
        }
        Scanner scanner = new Scanner(is);
        String datasetPath = classLoader.getResource(impFinalDatasetFolder).getPath();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\r", "");
            String[] splited = line.split(",");
            if (StringUtils.isEmpty(splited[0])) {
                continue;
            }
            String network = new Scanner(new File(datasetPath + "/" + splited[0] + ".txt"))
                    .useDelimiter("\\Z").next().replace("\r", "");
            String name = String.format("%s-%s-%s", splited[0], splited[1], splited[2]);
            IMPDataset dataset = new IMPDataset(name, Integer.parseInt(splited[3])
                    , Integer.parseInt(splited[4]), Integer.parseInt(splited[5])
                    , Integer.parseInt(splited[1]), splited[2], network);
            dataset.setEnabled(true);
            dataset.setSubmittable(false);
            dataset.setFinalJudge(true);
            dataset = Database.getInstance().getImpDatasets().insert(dataset);
            logger.info(dataset.toString());
        }
    }

    /**
     * Add final judge cases
     */
    private static void addFinalCases() {
        Date endTime = Deadline.getImpDDL();
        // Query datasets
        List<IMPDataset> datasets = Database
                .getInstance()
                .getImpDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .collect(Collectors.toList());
        // Query users
        List<User> users = Database.getInstance().getUsers().findAllByType(User.USER);
        List<IMPCase> cases = new ArrayList<>();
        for (User u : users) {
            IMPCase submission;
            submission = Database.getInstance().getImpCases()
                    .findFirstByUserAndSubmitTimeBeforeOrderBySubmitTimeDesc(u, endTime);
            if (submission == null || submission.getArchive() == null) {
                continue;
            }
            for (IMPDataset dataset : datasets) {
                for (int i = 0; i < casesNumber; i++) {
                    cases.add(new IMPCase(u, dataset.getId(), submission.getArchive()));
                }
            }
        }
        addIMPCases(cases);
    }

    private static void deleteAllCases() {
        List<IMPDataset> datasets = Database.
                getInstance().
                getImpDatasets().
                findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        datasets.forEach(dataset -> {
            List<IMPCase> cases = Database.
                    getInstance().
                    getImpCases().
                    findIMPCasesByDatasetIdOrderBySubmitTimeDesc(dataset.getId());
            deleteCases(cases);
        });
    }

    private static void exportFinalJudge() throws IOException {

        List<IMPDataset> datasets = Database.getInstance().getImpDatasets().findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());


        List<String> headers = new ArrayList<>();
        datasets.forEach(dataset -> headers.add(dataset.getName()));

        String headerContent = "user";
        for (String header : headers) {
            headerContent = String.format("%s,%s", headerContent, header);
        }

        FileWriter writer = new FileWriter(impFinalResultFileName);
        writer.write(String.format("%s\n", headerContent));


        List<User> users = Database.getInstance().getUsers().findAllByType(User.USER);
        List<HashMap<String, Double>> userResults = new ArrayList<>();
        users.forEach(user -> {
            HashMap<String, Double> userJudgeResults = new HashMap<>();
            datasets.forEach(dataset -> {
                double max = 0;
                List<IMPCase> judgeCases = Database.getInstance().getImpCases().findIMPCasesByDatasetIdAndUser(dataset.getId(), user);
                for (IMPCase judgeCase : judgeCases) {
                    max = Math.max(max, judgeCase.getInfluence());
                }
                userJudgeResults.put(dataset.getName(), max);
            });

            String line = user.getUsername();
            for (String header : headers) {
                line = String.format("%s,%f", line, userJudgeResults.get(header));
            }
            try {
                writer.write(String.format("%s\n", line));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.close();

    }

    /**
     * If some cases are deleted by mistake.
     * Use this method to reAdd them instead of add all cases again
     */
    private static void addInsufficientCases() {
        List<IMPDataset> datasets = Database
                .getInstance()
                .getImpDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .collect(Collectors.toList());
        List<User> users = Database.getInstance().getUsers().findAllByType(User.USER);
        List<IMPCase> cases = new ArrayList<>();
        for (User user : users) {
            logger.info(String.format("User: %s", user.toString()));
            IMPCase submission = Database
                    .getInstance()
                    .getImpCases()
                    .findFirstByUserOrderBySubmitTimeDesc(user);
            if (submission == null || submission.getArchive() == null) {
                continue;
            }
            HashMap<String, Integer> datasetsMap = new HashMap<>();
            datasets.forEach(d -> {
                datasetsMap.put(d.getId(), 0);
            });
            List<IMPCase> userCases = Database.
                    getInstance().
                    getImpCases().
                    findIMPCasesByUserOrderBySubmitTimeDesc(user);
            userCases.forEach(c -> {
                IMPDataset dataset = c.getDataset();
                if (datasetsMap.containsKey(dataset.getId())) {
                    datasetsMap.put(dataset.getId(), datasetsMap.get(dataset.getId()) + 1);
                }
            });

            datasetsMap.forEach((key, value) -> {
                for (int i = value; i < casesNumber; i++) {
                    cases.add(new IMPCase(user, key, submission.getArchive()));
                }
            });
        }
        addIMPCases(cases);
    }

    /**
     * Some students will re-submit the imp file after IMP DDL.
     * This method is used to add these cases
     * @throws IOException
     */
    private static void addLateSubmissionCases() throws IOException {
        List<IMPDataset> datasets = Database
                .getInstance()
                .getImpDatasets()
                .findAll()
                .stream()
                .filter(BaseDataset::isFinalJudge)
                .filter(BaseDataset::isEnabled)
                .collect(Collectors.toList());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File lateSubmissionIMPFolder = new File(Objects.requireNonNull(classLoader.getResource(lateSubmissionIMPFolderName)).getPath());
        File[] lateSubmissionIMPs = lateSubmissionIMPFolder.listFiles();

        List<IMPCase> cases = new ArrayList<>();

        for (File lateSubmissionIMP : lateSubmissionIMPs) {
            String fileName = lateSubmissionIMP.getName();
            String username = fileName.substring(0, fileName.lastIndexOf("."));

            User user = Database.getInstance().getUsers().findByUsername(username);
            if (user == null) {
                continue;
            }

            byte[] buffer = Files.readAllBytes(lateSubmissionIMP.toPath());
            String base64 = Base64.getEncoder().encodeToString(buffer);
            Binary binary = ParameterFileUtils.convertSubmit(base64);

            datasets.forEach(d -> {
                for (int i = 0; i < casesNumber; i++) {
                    cases.add(new IMPCase(user, d.getId(), binary));
                }
            });
        }

        Collections.shuffle(cases);
        cases.forEach(c -> {
            IMPCase newC = Database.getInstance().getImpCases().insert(c);
            Database.getInstance().getLiteCases().insert(new LiteCase(c));
            logger.info(newC.toString());
        });
    }

    private static void addIMPCases(List<IMPCase> cases) {
        Collections.shuffle(cases);
        for (IMPCase impCases : cases) {
            IMPCase newCase = Database.getInstance().getImpCases().insert(impCases);
            Database.getInstance().getLiteCases().insert(new LiteCase(impCases));
            logger.info(String.format("Insert cases: %s", newCase.toString()));
        }
        logger.info(String.format("Add %d final cases", cases.size()));
    }

    private static void deleteCases(List<IMPCase> cases) {
        cases.forEach(IMPJudgeFinal::deleteCase);
    }

    private static void deleteCase(IMPCase oneCase) {
        LiteCase liteCase = Database.
                getInstance().
                getLiteCases().
                findLiteCaseByFullId(oneCase.getId());
        Database.getInstance().getImpCases().delete(oneCase);
        Database.getInstance().getLiteCases().delete(liteCase);
        logger.info("Remove cases: " + oneCase.toString());

    }
}

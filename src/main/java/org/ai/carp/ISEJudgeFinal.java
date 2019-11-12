package org.ai.carp;

import org.ai.carp.controller.judge.Deadline;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.ISECase;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class ISEJudgeFinal {

    private static final Logger logger = LoggerFactory.getLogger(ISEJudgeFinal.class);

    public static void main(String[] args) throws FileNotFoundException {
        SpringApplication app = new SpringApplication(ISEJudgeFinal.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        //disableDatasets();
        //addDatasets();
        //addCases();
        addUserCases("11712815");
    }

    private static void disableDatasets() {
        Database.getInstance().getIseDatasets().findAll().forEach(c -> {
            if(c.getName().contains("random")){
                Database.getInstance().getIseDatasets().delete(c);
                logger.info("drop:"+c.toString());
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
        String datasetPath = classLoader.getResource("datasets_imp_final").getPath();
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

    private static void addUserCases(String userName) {
        Date endTime = Deadline.getIseDDL();
        // Query datasets
        List<ISEDataset> datasets = Database.getInstance().getIseDatasets().findAll()
                .stream().filter(BaseDataset::isFinalJudge).collect(Collectors.toList());
        // Query users
        User u = Database.getInstance().getUsers().findByUsername(userName);
        if(u==null){
            logger.info("user not found:"+userName);
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
                .findISECasesByUserAndDatasetOrderBySubmitTimeDesc(u, dataset);
            Database.getInstance().getIseCases().deleteAll(isecases);
            logger.info(String.format("remove %d isecase of %s:%s", isecases.size(), u.getUsername(), dataset.getName()));

            for (int i=0; i<5; i++) {
                cases.add(new ISECase(u, dataset, submission.getArchive()));
            }
        }
        Collections.shuffle(cases);
        for (ISECase c : cases) {
            ISECase newC = Database.getInstance().getIseCases().insert(c);
            Database.getInstance().getLiteCases().insert(new LiteCase(c));
            logger.info(newC.toString());
        }
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
                continue;
            }
            for (ISEDataset dataset : datasets) {
                for (int i=0; i<5; i++) {
                    cases.add(new ISECase(u, dataset, submission.getArchive()));
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

}

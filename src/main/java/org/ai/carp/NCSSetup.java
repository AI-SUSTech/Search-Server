package org.ai.carp;

import org.ai.carp.controller.util.ParameterFileUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.judge.NCSCase;
import org.ai.carp.model.judge.NCSParameter;
import org.ai.carp.model.user.User;
import org.ai.carp.model.user.UserRepository;
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
import java.net.URISyntaxException;
import java.util.*;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class NCSSetup {

    private static final Logger logger = LoggerFactory.getLogger(NCSSetup.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NCSSetup.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        cutNCSLog();
//        addUsers();
//        addDatasets();
    }

    private static void addUsers() {
        if (Database.getInstance().getUsers().findByUsername("root") == null) {
            Database.getInstance().getUsers().insert(new User("root", "123", User.ROOT));
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("users.csv");
        if (is == null) {
            logger.error("users.csv not found!");
            return;
        }
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\r", "");
            String[] splitted = line.split(",");
            if (StringUtils.isEmpty(splitted[0])) {
                continue;
            }
            UserRepository userRepository = Database.getInstance().getUsers();
            User existUser = userRepository.findByUsername(splitted[0]);
            LinkedList<User> users = new LinkedList<>();
            if (existUser == null) {
                int role;
                switch (splitted[1]) {
                    case "ADMIN":
                        role = User.ADMIN;
                        break;
                    case "USER":
                        role = User.USER;
                        break;
                    case "WORKER":
                        role = User.WORKER;
                        break;
                    default:
                        logger.error("Invalid user info for {}", splitted[0]);
                        continue;
                }
                User user = new User(splitted[0], splitted[2], role);
                if (splitted.length >= 4) {
                    user.setEmail(splitted[3].trim());
                }
                user = Database.getInstance().getUsers().insert(user);
                logger.info(user.toString());
            } else {
                if (splitted.length >= 4) {
                    if (true || existUser.getUsername().equals("hya")
                            || existUser.getUsername().equals("zqh")) {
                        existUser.setType(User.ADMIN);
                        logger.info("add admin: " + existUser.toString());
                    }
                    existUser.setEmail(splitted[3].trim());
                    users.add(existUser);
                    logger.info("add email: " + existUser.toString());
                }
            }
            userRepository.saveAll(users);
        }
        scanner.close();
    }

    private static void cutNCSLog() {
        List<NCSCase> caseList = Database.getInstance().getNcsCases().findAll();
        List<NCSCase> modifiedCase = new ArrayList<>();
        List<NCSCase> invalidCase = new ArrayList<>();
        for (NCSCase ncsCase : caseList) {
            boolean modifiedFlag = false;
            String stdout = ncsCase.getStdout();
            if (stdout != null && stdout.length() > 200) {
                ncsCase.setStdout(stdout.substring(stdout.length() - 200));
                modifiedFlag = true;
            }
            String stderr = ncsCase.getStderr();
            if (stderr != null && stderr.length() > 200) {
                ncsCase.setStderr(stderr.substring(stderr.length() - 200));
                modifiedFlag = true;
            }
            if (modifiedFlag) {
                modifiedCase.add(ncsCase);
            }
            try {
                ParameterFileUtils.checkSubmitPara(ncsCase.getUser(), ncsCase.getArchive().getData());
            } catch (Exception e) {
                ncsCase.setStdout(e.getMessage());
                invalidCase.add(ncsCase);
            }
        }
        Database.getInstance().getNcsCases().saveAll(modifiedCase);
        logger.info("modified cases number: " + modifiedCase.size());
        int f12 = 0, f6 = 0, f29 = 0;
        int invalidValue = 0;
        for (NCSCase ncsCase : invalidCase) {
            if(!ncsCase.isValid()){
                invalidValue ++;
                continue;
            }
            NCSDataset dataset = (NCSDataset) ncsCase.getDataset();
            switch (dataset.getProblem_index()) {
                case 6:
                    f6++;
                    logger.info("invalid parameter: " + ncsCase.getDatasetName() + " " +ncsCase.toString() + " " + new String(ncsCase.getArchive().getData()) +
                            ncsCase.getResult() + " "+ ncsCase.getStdout() + " " + ncsCase.getStderr());
                    break;
                case 12:
                    f12++;
                    logger.info("invalid parameter: " + ncsCase.getDatasetName() + " " +ncsCase.toString() + " " + new String(ncsCase.getArchive().getData()) +
                            ncsCase.getResult() + " "+ ncsCase.getStdout() + " " + ncsCase.getStderr());
                    break;
                case 29:
                    f29++;
                    logger.info("invalid parameter: " + ncsCase.getDatasetName() + " " +ncsCase.toString() + " " + new String(ncsCase.getArchive().getData()) +
                            ncsCase.getResult() + " "+ ncsCase.getStdout() + " " + ncsCase.getStderr());
                    break;
            }
            ncsCase.setValid(false);
            ncsCase.setReason("invalid parameter range");
            // logger.info("set invalid parameter to error: " + ncsCase.getDatasetName() + " " +ncsCase.toString() + " " + new String(ncsCase.getArchive().getData()) +
            //         ncsCase.getResult() + " "+ ncsCase.getStdout() + " " + ncsCase.getStderr());
        }
        logger.info(String.format("invalid parameter count: %d f6:%d f12:%d f29:%d invalid_res: %d", invalidCase.size(), f6, f12, f29, invalidValue));
        Database.getInstance().getNcsCases().saveAll(invalidCase);
    }

    private static void addDatasets() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("datasets_ncs.csv");
        if (is == null) {
            logger.error("datasets_ncs.csv not found");
            return;
        }
        Scanner scanner = new Scanner(is);
        Map<String, NCSDataset> map = new HashMap<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\r", "");
            String[] splitted = line.split(",");
            if (StringUtils.isEmpty(splitted[0])) {
                continue;
            }
            String dataName = String.format("F%s-%s", splitted[0], splitted[1].trim());
            map.put(dataName, new NCSDataset(dataName,
                    Integer.valueOf(splitted[2]),
                    Integer.valueOf(splitted[3]),
                    Integer.valueOf(splitted[4]),
                    Integer.valueOf(splitted[5]))
            );
        }
        for (String name : map.keySet()) {
            NCSDataset existDataset = Database.getInstance().getNcsDatasets().findDatasetByName(name);
            if (existDataset != null) {
                if (existDataset.getProblem_index() != 29) {
                    existDataset.setSubmittable(false);
                    existDataset.setEnabled(false);
                    existDataset = Database.getInstance().getNcsDatasets().save(existDataset);
                    logger.info("modify submittable to false: " + existDataset.toString());
                } else {
                    existDataset.setSubmittable(true);
                    existDataset = Database.getInstance().getNcsDatasets().save(existDataset);
                    logger.info("modify submittable to true: " + existDataset.toString());
                }
                continue;
            }
            NCSDataset dataset = map.get(name);
            if (dataset == null) {
                logger.error("Definition not found for {}", name);
                continue;
            }
            dataset.setEnabled(true);
            dataset.setSubmittable(true);
            dataset = Database.getInstance().getNcsDatasets().insert(dataset);
            logger.info(dataset.toString());
        }
//        try {
//            File datasets = new File(classLoader.getResource("datasets_ncs").toURI());
//            File[] list = datasets.listFiles((dir, name) -> name.endsWith(".txt"));
//            for (File f : list) {
//                try {
//                    String name = f.getName().replaceAll(".txt", "");
//                    if (Database.getInstance().getNcsDatasets().findDatasetByName(name) != null) {
//                        continue;
//                    }
//                    NCSDataset dataset = map.get(name);
//                    if (dataset == null) {
//                        logger.error("Definition not found for {}", name);
//                        continue;
//                    }
//                    String content = new Scanner(f).useDelimiter("\\Z").next().replace("\r", "");
//                    dataset.setData(content);
//                    dataset.setEnabled(true);
//                    dataset.setSubmittable(true);
//                    dataset = Database.getInstance().getNcsDatasets().insert(dataset);
//                    logger.info(dataset.toString());
//                } catch (FileNotFoundException e) {
//                    logger.error("Failed to read dataset {}", f.getName(), e);
//                }
//            }
//        } catch (URISyntaxException e) {
//            logger.error("Failed to get dataset path!", e);
//        }
    }

}

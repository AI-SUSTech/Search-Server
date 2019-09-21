package org.ai.carp;

import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.NCSDataset;
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class NCSSetup {

    private static final Logger logger = LoggerFactory.getLogger(NCSSetup.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NCSSetup.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        addUsers();
        addDatasets();
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
            if (Database.getInstance().getUsers().findByUsername(splitted[0]) == null) {
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
                user = Database.getInstance().getUsers().insert(user);
                logger.info(user.toString());
            }
        }
        scanner.close();
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
        for(String name: map.keySet()) {
            if (Database.getInstance().getNcsDatasets().findDatasetByName(name) != null) {
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

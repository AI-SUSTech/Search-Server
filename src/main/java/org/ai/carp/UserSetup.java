package org.ai.carp;

/**
 * @author Isaac Chen
 * @email ccccym666@gmail.com
 * @date 2020/10/25 23:29
 */

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
public class UserSetup {

    private static final Logger logger = LoggerFactory.getLogger(UserSetup.class);
    private static final String rootEmail = "sw584290791@2980.com";

    /**
     * users.csv is the file containing the information of all of the student in AI course.
     * The format of this file is as below:
     * {username},{role},{password},{email(option)}
     * Space between each item is not allowed
     */
    private static final String userCsvFile = "users.csv";
    private static final String rootUsername = "root";
    private static final String rootPassword = "password";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UserSetup.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        addUsers();
        addEmailForRoot();
    }

    private static void addEmailForRoot() {
        User root = Database.getInstance().getUsers().findByUsername(rootUsername);
        if (root == null) {
            root = new User(rootUsername, rootPassword, User.ROOT);
        }
        root.setEmail(rootEmail);
        Database.getInstance().getUsers().save(root);
    }

    private static void addUsers() {
        if (Database.getInstance().getUsers().findByUsername(rootUsername) == null) {
            Database.getInstance().getUsers().insert(new User(rootUsername, rootPassword, User.ROOT));
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(userCsvFile);
        if (is == null) {
            logger.error(String.format("%s not found!", userCsvFile));
            return;
        }
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\r", "");
            String[] split = line.split(",");
            if (StringUtils.isEmpty(split[0])) {
                continue;
            }
            UserRepository userRepository = Database.getInstance().getUsers();
            User existUser = userRepository.findByUsername(split[0]);
            LinkedList<User> users = new LinkedList<>();
            if (existUser == null) {
                int role;
                switch (split[1]) {
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
                        logger.error("Invalid user info for {}", split[0]);
                        continue;
                }
                User user = new User(split[0], split[2], role);
                if (split.length >= 4) {
                    user.setEmail(split[3].trim());
                }
                user = Database.getInstance().getUsers().insert(user);
                logger.info(user.toString());
            } else {
                if (split.length >= 4) {
                    existUser.setType(User.ADMIN);
                    logger.info("add admin: " + existUser.toString());
                    existUser.setEmail(split[3].trim());
                    users.add(existUser);
                    logger.info("add email: " + existUser.toString());
                }
            }
            userRepository.saveAll(users);
        }
        scanner.close();
    }

}

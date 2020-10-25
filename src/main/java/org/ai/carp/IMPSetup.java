package org.ai.carp;

import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.IMPDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@ComponentScan(basePackages = {"org.ai.carp.model"})
@SpringBootApplication
@EnableMongoRepositories("org.ai.carp.model")
public class IMPSetup {

    private static final Logger logger = LoggerFactory.getLogger(IMPSetup.class);
    /**
     * This csv file is a overall description for all datasets of imp
     * It is in the form of {filename},{seedCount},{model},{time},{memory},{cpu}
     * Space between each item is not allowed
     */
    private static final String impDatasetCsv = "datasets_imp.csv";
    /**
     * This csv file is a overall description for all datasets of imp
     * It is in the form of {filename},{seedfile},{model},{limitTime},{memory},{cpu},{influence},{bias}
     * Space between each item is not allowed
     */
    private static final String iseDatasetCsv = "datasets_ise.csv";
    // All of the dataset files, which end with .txt, are listed under the folder below
    private static final String impDataFolder = "datasets_imp";
    private static final String iseDataFolder = "datasets_ise";

    public static void main(String[] args) throws URISyntaxException {
        SpringApplication app = new SpringApplication(IMPSetup.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
//        addISEDatasets();
        addIMPDatasets();
//        enableIseDatasets(true);
    }

    private static void addIMPDatasets() throws URISyntaxException {
        Map<String, File> fileMap = getDataFileMap(impDataFolder);
        Scanner scanner = getDataScanner(impDatasetCsv);
        if (scanner == null) {
            return;
        }
//        name, seedCount, model, limitTime, memory, cpu,
        while (scanner.hasNextLine()) {
            String[] split = splitCsvData(scanner.nextLine());
            if (split == null || split.length == 0) {
                continue;
            }
            String name = split[0] + "-" + split[1] + "-" + split[2];
            if (Database.getInstance().getImpDatasets().findDatasetByName(name) != null) {
                continue;
            }
            try {
                File networkFile = fileMap.get(split[0]);
                String network = new Scanner(networkFile).useDelimiter("\\Z").next().replace("\r", "");
                IMPDataset dataset = new IMPDataset(name, Integer.valueOf(split[3])
                        , Integer.valueOf(split[4]), Integer.valueOf(split[5])
                        , Integer.valueOf(split[1]), split[2], network);
                dataset.setEnabled(true);
                dataset.setSubmittable(true);
                dataset.setFinalJudge(false);
                dataset = Database.getInstance().getImpDatasets().insert(dataset);
                logger.info(dataset.toString());
            } catch (Exception e) {
                logger.error("Error adding dataset", e);
            }
        }
    }

    private static void addISEDatasets() throws URISyntaxException {
        Map<String, File> fileMap = getDataFileMap(iseDataFolder);
        Scanner scanner = getDataScanner(iseDatasetCsv);
        if (scanner == null) {
            return;
        }
//        name, seedfile, model, limitTime, memory, cpu, influence, bias
        while (scanner.hasNextLine()) {
            String[] split = splitCsvData(scanner.nextLine());
            if (split == null || split.length == 0) {
                continue;
            }
            String name = split[0] + "-" + split[1] + "-" + split[2];
            if (Database.getInstance().getIseDatasets().findDatasetByName(name) != null) {
                continue;
            }
            try {
                File networkFile = fileMap.get(split[0]);
                String network = new Scanner(networkFile).useDelimiter("\\Z").next().replace("\r", "");
                File seedsFile = fileMap.get(split[1]);
                String seeds = new Scanner(seedsFile).useDelimiter("\\Z").next().replace("\r", "");
                ISEDataset dataset = new ISEDataset(name, Integer.valueOf(split[3])
                        , Integer.valueOf(split[4]), Integer.valueOf(split[5])
                        , split[2], network, seeds, Double.valueOf(split[6])
                        , Double.valueOf(split[7]));
                dataset.setEnabled(true);
                dataset.setSubmittable(true);
                dataset.setFinalJudge(false);

                dataset = Database.getInstance().getIseDatasets().insert(dataset);
                logger.info(dataset.toString());
            } catch (Exception e) {
                logger.error("Error adding dataset", e);
            }
        }
    }

    private static void changeDatasetsInfo() {
        Database.getInstance().getImpDatasets().findAll().forEach(c ->
        {
            c.setEnabled(true);
            c.setSubmittable(true);
            c.setFinalJudge(false);
            if (c.getName().startsWith("NetHEPT")) {
                c.setMemory(2000);
                logger.info("change memory" + c.toString());
            }
            Database.getInstance().getImpDatasets().save(c);
            logger.info(c.toString());
        });
    }

    private static void enableIseDatasets(boolean enable) {
        Database.getInstance().getIseDatasets().findAll().forEach(c ->
        {
            c.setEnabled(enable);
            Database.getInstance().getIseDatasets().save(c);
            logger.info(c.toString());
        });
    }

    private static Scanner getDataScanner(String csvFile) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(csvFile);
        if (is == null) {
            logger.error(String.format("%s not found", csvFile));
            return null;
        }
        return new Scanner(is);
    }

    private static Map<String, File> getDataFileMap(String folder) throws URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File datasets = new File(classLoader.getResource(folder).toURI());
        File[] list = datasets.listFiles((dir, name) -> name.endsWith(".txt"));
        Map<String, File> fileMap = new HashMap<>();
        for (File f : list) {
            fileMap.put(f.getName().replace(".txt", ""), f);
        }
        return fileMap;
    }

    private static String[] splitCsvData(String lineData) {
        if (lineData.startsWith("#")) {
            return null;
        }
        lineData = lineData.replaceAll("\r", "");
        String[] split = lineData.split(",");
        if (StringUtils.isEmpty(split[0])) {
            return null;
        }
        return split;
    }
}

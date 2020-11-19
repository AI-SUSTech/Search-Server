package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.controller.judge.QueryTopResult;
import org.ai.carp.controller.util.ISEUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.ISECase;
import org.ai.carp.model.user.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ISEFunction implements BaseFunction {
    private static final Logger logger = LoggerFactory.getLogger(BaseFunction.class);

    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getIseCases().save((ISECase) baseCase);
    }

    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getIseCases()
                .insert(new ISECase(user, dataset.getId(), archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        ISEUtils.checkResult((ISECase) baseCase);
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getIseCases()
                .findISECasesByDatasetIdAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        dataset.getId(), user, BaseCase.FINISHED, true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getIseCases()
                .findISECasesByUserAndDatasetIdOrderBySubmitTimeDesc(user, dataset.getId())
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getIseCases()
                .findISECasesByDatasetIdAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        dataset.getId(), BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase) c).collect(Collectors.toList());
    }

    private Stream<ISECase> getTopResult(ISEDataset dataset) {
        List<ISECase> caseList = Database.getInstance().getIseCases().findISECasesByDatasetIdAndStatus(dataset.getId(), BaseCase.FINISHED);
        List<ISECase> errorList = Database.getInstance().getIseCases().findISECasesByDatasetIdAndStatus(dataset.getId(), BaseCase.ERROR);
        HashMap<String, ISECase> map = new HashMap<>();
        for (ISECase iseCase : caseList) {
            if (iseCase.getUser().getType() <= User.ADMIN) {
                continue;
            }
            String userName = iseCase.getUser().getUsername();
            if (!map.containsKey(userName)
                    || !map.get(userName).isValid()) {
                map.put(userName, iseCase);
            }
        }
        for (ISECase iseCase : errorList) {
            if (iseCase.getUser().getType() <= User.ADMIN) {
                continue;
            }
            String userName = iseCase.getUser().getUsername();
            if (!map.containsKey(userName)) {
                map.put(userName, iseCase);
            }
        }
        return map.values().stream();
    }

    @Override
    public Workbook getFinalGrades() {
        Workbook wb = new XSSFWorkbook();
        Sheet finalSheet = wb.createSheet("Final");
        Row finalTitle = finalSheet.createRow(0);
        finalTitle.createCell(0).setCellValue("ID");
        Map<String, Row> stuFinalMap = new HashMap<>();

        IntWrapper baseCol = new IntWrapper();
        baseCol.num = -3;
        Database.getInstance().getIseDatasets().findAll()
                .stream().filter(ISEDataset::isEnabled).forEach(iseDataset -> {
            // Add combined data
            baseCol.num += 4;
            finalTitle.createCell(baseCol.num).setCellValue(iseDataset.getName());
            finalTitle.createCell(baseCol.num + 1).setCellValue("Time");
            finalTitle.createCell(baseCol.num + 2).setCellValue("Result");
            finalTitle.createCell(baseCol.num + 3).setCellValue("Reason");


            getTopResult(iseDataset).forEach(c -> {
                Row r;
                if (!stuFinalMap.containsKey(c.getUser().getUsername())) {
                    r = finalSheet.createRow(finalSheet.getLastRowNum() + 1);
                    r.createCell(0).setCellValue(c.getUser().getUsername());
                    stuFinalMap.put(c.getUser().getUsername(), r);
                } else {
                    r = stuFinalMap.get(c.getUser().getUsername());
                }
                r.createCell(baseCol.num).setCellValue(c.getSubmitTime().toString());
                r.createCell(baseCol.num + 1).setCellValue(c.getTime());
                r.createCell(baseCol.num + 2).setCellValue(c.getResult());
                r.createCell(baseCol.num + 3).setCellValue(c.getReason());
            });
            finalSheet.autoSizeColumn(baseCol.num);
            finalSheet.autoSizeColumn(baseCol.num + 1);
            finalSheet.autoSizeColumn(baseCol.num + 2);
            finalSheet.autoSizeColumn(baseCol.num + 3);
        });
        return wb;
    }

    public Workbook getFinalFinalGrades() {
        Workbook wb = new XSSFWorkbook();
        Sheet finalSheet = wb.createSheet("Final");
        Row finalTitle = finalSheet.createRow(0);
        finalTitle.createCell(0).setCellValue("ID");
        Map<String, Row> stuFinalMap = new HashMap<>();
        IntWrapper baseCol = new IntWrapper();
        baseCol.num = -2;
        Database.getInstance().getIseDatasets().findAll()
                .stream().filter(ISEDataset::isFinalJudge).forEach(d -> {
            // Add combined data
            baseCol.num += 3;
            finalTitle.createCell(baseCol.num).setCellValue(d.getName());
            finalTitle.createCell(baseCol.num + 1).setCellValue("Time");
            finalTitle.createCell(baseCol.num + 2).setCellValue("Count");
            QueryTopResult.getFinalList(d.getId()).forEach(c -> {
                Row r;
                if (!stuFinalMap.containsKey(c.getUserName())) {
                    r = finalSheet.createRow(finalSheet.getLastRowNum() + 1);
                    r.createCell(0).setCellValue(c.getUserName());
                    stuFinalMap.put(c.getUserName(), r);
                } else {
                    r = stuFinalMap.get(c.getUserName());
                }
                r.createCell(baseCol.num).setCellValue(c.getResult());
                r.createCell(baseCol.num + 1).setCellValue(c.getTime());
                r.createCell(baseCol.num + 2).setCellValue(c.getCount());
            });
            finalSheet.autoSizeColumn(baseCol.num);
            finalSheet.autoSizeColumn(baseCol.num + 1);
            finalSheet.autoSizeColumn(baseCol.num + 2);
            // Create dataset sheet
            int nameLen = d.getName().length();
            String sheetName = nameLen > 30 ? d.getName().substring(nameLen - 30) : d.getName();
            Sheet sheet = wb.createSheet(sheetName);
            // System.out.println(String.format("add %s now has sheet:", d.getName()));
            // for(int k=0;k<wb.getNumberOfSheets();k++) {
            //     System.out.println(wb.getSheetName(k));
            // }
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("ID");
            for (int i = 0; i < 5; i++) {
                row.createCell(i * 3 + 1).setCellValue(String.valueOf(i + 1));
                row.createCell(i * 3 + 2).setCellValue("Time");
                row.createCell(i * 3 + 3).setCellValue("Reason");
            }
            // Add dataset data
            Map<String, Row> stuMap = new HashMap<>();
            Database.getInstance().getIseCases()
                    .findISECasesByDatasetIdOrderBySubmitTimeDesc(d.getId())
                    .stream().forEach(c -> {
                Row r;
                if (!stuMap.containsKey(c.getUser().getUsername())) {
                    r = sheet.createRow(sheet.getLastRowNum() + 1);
                    r.createCell(0).setCellValue(c.getUser().getUsername());
                    stuMap.put(c.getUser().getUsername(), r);
                } else {
                    r = stuMap.get(c.getUser().getUsername());
                }
                r.createCell(r.getLastCellNum()).setCellValue(c.getResult());
                r.createCell(r.getLastCellNum()).setCellValue(c.getTime());
                r.createCell(r.getLastCellNum()).setCellValue(c.getReason());
            });
            for (int i = 0; i <= 15; i++) {
                sheet.autoSizeColumn(i);
            }
        });
        return wb;
    }

    private class IntWrapper {
        public int num = 0;
    }


}

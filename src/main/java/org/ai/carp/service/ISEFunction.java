package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.admin.judge.GetISEGrades;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.controller.judge.QueryTopResult;
import org.ai.carp.controller.util.ISEUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.ISEDataset;
import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.ISECase;
import org.ai.carp.model.judge.NCSCase;
import org.ai.carp.model.user.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.Binary;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ISEFunction implements BaseFunction {

    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getIseCases().save((ISECase) baseCase);
    }

    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        return Database.getInstance()
                .getIseCases()
                .insert(new ISECase(user, (ISEDataset) dataset, archive));
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        ISEUtils.checkResult((ISECase)baseCase);
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getIseCases()
                .findISECasesByDatasetAndUserAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (ISEDataset)dataset, user, BaseCase.FINISHED, true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getIseCases()
                .findISECasesByUserAndDatasetOrderBySubmitTimeDesc(user, (ISEDataset)dataset)
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getIseCases()
                .findISECasesByDatasetAndStatusAndValidOrderByTimeAscSubmitTimeAsc(
                        (ISEDataset)dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase)c).collect(Collectors.toList());
    }

    @Override
    public Workbook getFinalGrades() {
        Workbook wb = new XSSFWorkbook();
        Sheet finalSheet = wb.createSheet("Final");
        Row finalTitle = finalSheet.createRow(0);
        finalTitle.createCell(0).setCellValue("ID");
        Map<String, Row> stuFinalMap = new HashMap<>();
        IntWrapper baseCol = new IntWrapper();
        baseCol.num = -2;
        Database.getInstance().getIseDatasets().findAll()
                .stream().filter(ISEDataset::isSubmittable).forEach(d -> {
            // Add combined data
            baseCol.num += 3;
            finalTitle.createCell(baseCol.num).setCellValue(d.getName());
            finalTitle.createCell(baseCol.num+1).setCellValue("Time");
            finalTitle.createCell(baseCol.num+2).setCellValue("Count");
            QueryTopResult.getFinalList(d.getId()).forEach(c -> {
                Row r;
                if (!stuFinalMap.containsKey(c.getUserName())) {
                    r = finalSheet.createRow(finalSheet.getLastRowNum()+1);
                    r.createCell(0).setCellValue(c.getUserName());
                    stuFinalMap.put(c.getUserName(), r);
                } else {
                    r = stuFinalMap.get(c.getUserName());
                }
                r.createCell(baseCol.num).setCellValue(c.getResult());
                r.createCell(baseCol.num+1).setCellValue(c.getTime());
                r.createCell(baseCol.num+2).setCellValue(c.getCount());
            });
            finalSheet.autoSizeColumn(baseCol.num);
            finalSheet.autoSizeColumn(baseCol.num+1);
            finalSheet.autoSizeColumn(baseCol.num+2);
            // Create dataset sheet
            Sheet sheet = wb.createSheet(d.getName());
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("ID");
            for (int i=0; i<5; i++) {
                row.createCell(i*3+1).setCellValue(String.valueOf(i+1));
                row.createCell(i*3+2).setCellValue("Time");
                row.createCell(i*3+3).setCellValue("Reason");
            }
            // Add dataset data
            Map<String, Row> stuMap = new HashMap<>();
            Database.getInstance().getIseCases()
                    .findISECasesByDatasetOrderBySubmitTimeDesc(d)
                    .stream().forEach(c -> {
                Row r;
                if (!stuMap.containsKey(c.getUser().getUsername())) {
                    r = sheet.createRow(sheet.getLastRowNum()+1);
                    r.createCell(0).setCellValue(c.getUser().getUsername());
                    stuMap.put(c.getUser().getUsername(), r);
                } else {
                    r = stuMap.get(c.getUser().getUsername());
                }
                r.createCell(r.getLastCellNum()).setCellValue(c.getResult());
                r.createCell(r.getLastCellNum()).setCellValue(c.getTime());
                r.createCell(r.getLastCellNum()).setCellValue(c.getReason());
            });
            for (int i=0; i<=15; i++) {
                sheet.autoSizeColumn(i);
            }
        });
        return wb;
    }

    private class IntWrapper {
        public int num = 0;
    }


}

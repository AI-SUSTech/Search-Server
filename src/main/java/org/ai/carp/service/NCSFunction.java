package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.judge.QuerySelfBestController;
import org.ai.carp.controller.judge.QueryTopResult;
import org.ai.carp.controller.util.ParameterFileUtils;
import org.ai.carp.model.Database;
import org.ai.carp.model.dataset.BaseDataset;
import org.ai.carp.model.dataset.NCSDataset;
import org.ai.carp.model.judge.BaseCase;
import org.ai.carp.model.judge.NCSCase;
import org.ai.carp.model.judge.NCSParameter;
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

public class NCSFunction implements BaseFunction {
    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getNcsCases().save((NCSCase) baseCase);
    }

    /**
     *
     * @param user the user whom submit this case to judge
     * @param dataset useless, should be null
     * @param archive useless, should be null
     * @return new inserted NCSCase
     */
    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        NCSParameter ncsParameter = ParameterFileUtils.checkSubmitPara(user, archive.getData());
        ncsParameter.setDataset((NCSDataset) dataset);
        if(user.getType() > User.ADMIN){
                int hash = ncsParameter.getHash();
                List<NCSParameter> parameters = Database.getInstance().getNcsParameterRepository()
                        .findNCSParametersByDatasetAndHashAndUserNot(
                                (NCSDataset) dataset,
                                hash,
                                user
                        );
                for(NCSParameter parameter:parameters) {
                if (parameter.equals(ncsParameter))
                        throw new InvalidRequestException(
                                String.format("You parameter is the same as %s", parameter.getUser().getUsername()));
                }
        }
        BaseCase resCase =  Database.getInstance()
                .getNcsCases()
                .insert(new NCSCase(user, dataset, archive));
        ncsParameter.setCaseId(resCase.getId());
        Database.getInstance().getNcsParameterRepository().insert(ncsParameter);
        return resCase;
    }

    @Override
    public void afterGetResult(BaseCase baseCase, JsonNode rootNode) {
        baseCase.setValid(rootNode.get("valid").asBoolean());
        baseCase.setReason(rootNode.get("reason").asText());
        baseCase.setResult(rootNode.get("influence").asDouble());
    }

    @Override
    public List<BaseCase> getBestResult(User user, BaseDataset dataset) {
        List<BaseCase> bestCases;
        bestCases = Database.getInstance().getNcsCases()
                .findNCSCasesByDatasetAndUserAndStatusAndValidOrderByResultAscTimeAscSubmitTimeDesc(
                        (NCSDataset) dataset,
                        user,
                        BaseCase.FINISHED,
                        true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getNcsCases()
                .findNCSCasesByUserAndDatasetOrderBySubmitTimeDesc(user, (NCSDataset)dataset)
                .stream().map(c -> (BaseCase)c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getNcsCases()
                .findNCSCasesByDatasetAndStatusAndValidOrderByResultAscTimeAscSubmitTimeAsc(
                        (NCSDataset)dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase)c).collect(Collectors.toList());
    }
    private class IntWrapper {
        public int num = 0;
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
        Database.getInstance().getNcsDatasets().findAll()
                .stream().filter(NCSDataset::isFinalJudge).forEach(d -> {
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
            Database.getInstance().getNcsCases()
                    .findIMPCasesByDatasetOrderBySubmitTimeDesc(d)
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


}

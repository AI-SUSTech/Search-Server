package org.ai.carp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.ai.carp.controller.exceptions.InvalidRequestException;
import org.ai.carp.controller.judge.QuerySelfBestController;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NCSFunction implements BaseFunction {
    @Override
    public BaseCase save(BaseCase baseCase) {
        return Database.getInstance().getNcsCases().save((NCSCase) baseCase);
    }

    /**
     * @param user    the user whom submit this case to judge
     * @param dataset useless, should be null
     * @param archive useless, should be null
     * @return new inserted NCSCase
     */
    @Override
    public BaseCase insert(User user, BaseDataset dataset, Binary archive) {
        NCSParameter ncsParameter = ParameterFileUtils.checkSubmitPara(user, archive.getData());
        ncsParameter.setDataset((NCSDataset) dataset);
        if (user.getType() > User.ADMIN) {
            int hash = ncsParameter.getHash();
            List<NCSParameter> parameters = Database.getInstance().getNcsParameterRepository()
                    .findNCSParametersByDatasetAndHashAndUserNot(
                            (NCSDataset) dataset,
                            hash,
                            user
                    );
            for (NCSParameter parameter : parameters) {
                if (parameter.equals(ncsParameter))
                    throw new InvalidRequestException(
                            String.format("You parameter is the same as %s", parameter.getUser().getUsername()));
            }
        }
        BaseCase resCase = Database.getInstance()
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
                .findNCSCasesByDatasetAndUserAndStatusAndValidOrderByResultDescTimeAscSubmitTimeDesc(
                        (NCSDataset) dataset,
                        user,
                        BaseCase.FINISHED,
                        true,
                        PageRequest.of(0, QuerySelfBestController.COUNT_BEST))
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return bestCases;
    }

    @Override
    public List<BaseCase> queryUserCaseOfDataset(User user, BaseDataset dataset) {
        List<BaseCase> baseCases;
        baseCases = Database.getInstance().getNcsCases()
                .findNCSCasesByUserAndDatasetOrderBySubmitTimeDesc(user, (NCSDataset) dataset)
                .stream().map(c -> (BaseCase) c).collect(Collectors.toList());
        return baseCases;
    }

    @Override
    public List<BaseCase> queryAllDatasetOfUser(BaseDataset dataset) {
        return Database.getInstance().getNcsCases()
                .findNCSCasesByDatasetAndStatusAndValidOrderByResultDescTimeAscSubmitTimeAsc(
                        (NCSDataset) dataset, BaseCase.FINISHED, true)
                .stream().filter(c -> c.getUser().getType() > User.ADMIN)
                .map(c -> (BaseCase) c).collect(Collectors.toList());
    }

    private static class IntWrapper {
        int num = 0;
    }

    private Stream<NCSCase> getTopResult(NCSDataset dataset) {
        List<NCSCase> caseList = Database.getInstance().getNcsCases().findNCSCaseByDatasetAndStatusAndValid(dataset, BaseCase.FINISHED, true);
        HashMap<String, NCSCase> map = new HashMap<>();
        for (NCSCase ncsCase : caseList) {
            if (ncsCase.getUser().getType() <= User.ADMIN) {
                continue;
            }
            String userName = ncsCase.getUser().getUsername();
            if (!map.containsKey(userName) || map.get(userName).getResult() < ncsCase.getResult()) {
                map.put(userName, ncsCase);
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
        baseCol.num = -2;
        Database.getInstance().getNcsDatasets().findAll()
                .stream().filter(NCSDataset::isEnabled).forEach(ncsDataset -> {
            // Add combined data
            baseCol.num += 3;
            finalTitle.createCell(baseCol.num).setCellValue(ncsDataset.getName());
            finalTitle.createCell(baseCol.num + 1).setCellValue("Time");
            finalTitle.createCell(baseCol.num + 2).setCellValue("Result");


            getTopResult(ncsDataset).forEach(c -> {
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
            });
            finalSheet.autoSizeColumn(baseCol.num);
            finalSheet.autoSizeColumn(baseCol.num + 1);
            finalSheet.autoSizeColumn(baseCol.num + 2);
        });
        return wb;
    }


}

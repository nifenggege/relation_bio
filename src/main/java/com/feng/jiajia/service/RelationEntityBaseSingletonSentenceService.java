package com.feng.jiajia.service;

import com.feng.jiajia.model.Entity;
import com.feng.jiajia.model.Relation;
import com.feng.jiajia.utils.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Set;

/**
 * 基于实体下标查找
 * 只找单句中的实体关系
 * 对应需求一
 */
public class RelationEntityBaseSingletonSentenceService extends  AbstractRelationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationEntityBaseSingletonSentenceService.class);
    private static final String RELATION_PATH = "data/relation/";
    /**
     * 处理一篇文章
     * @param env
     * @param fileName
     * @param entityList
     * @param relationList
     * @throws IOException
     */
    public void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList,
                           BufferedWriter bwAll, BufferedWriter bwExtAll) throws IOException {

        String path = BASE_PATH+SEN_SPL_TEXT_PATH+env+"/" + fileName;
        String name = fileName.split("\\.")[0];
        String savePath = BASE_PATH+RELATION_PATH+env+"/" + name +  RESULT_SUFFIX;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savePath), "utf-8"));

        List<String> sentenceList = getSentenceList(br);
        List<String> resultList = Lists.newArrayList();
        List<String> resultExtList = Lists.newArrayList();
        int offset = 0;
        for(String line : sentenceList){
            int last = offset + line.length();
            //1. 找到句子中的实体
            List<Entity> containsEntity = getEntityListFromSentence(line, entityList, offset, last);
            if(containsEntity.size()<2){
                offset = last+1;
                continue;
            }
            //2. 判断实体间的关系
            for(int i=0; i<containsEntity.size(); i++){
                for(int j=i+1; j<containsEntity.size(); j++){
                    Entity firstEntity = containsEntity.get(i);
                    Entity secondEntity = containsEntity.get(j);
                    String firstType = firstEntity.getType();
                    String secondType = secondEntity.getType();
                    if(matchRelation(firstEntity.getType(), secondEntity.getType())){
                        String extInfo = "";
                        String relation = processRelation(relationList, offset, line, firstEntity, secondEntity);
                        if(StringUtils.isEmpty(relation) || resultList.contains(relation)){
                            continue;
                        }
                        resultList.add(relation);
                        resultExtList.add(fileName + "\t" + firstType+":"+firstEntity.getAliasName() + "\t" +
                                secondType+":"+secondEntity.getAliasName());
                    }
                }
            }

            offset = last+1;
        }

        writeFile(bw, resultList);
        writeFile(bwAll, resultList);
        writeFile(bwExtAll, resultExtList);
        br.close();
        bw.close();
    }

    String getSavePath() {
        return RELATION_PATH;
    }

    public static void main(String[] args) {
        RelationEntityBaseSingletonSentenceService service = new RelationEntityBaseSingletonSentenceService();
        service.buildRelation("train");
        service.buildRelation("dev");
        service.buildRelation("test");
    }
}
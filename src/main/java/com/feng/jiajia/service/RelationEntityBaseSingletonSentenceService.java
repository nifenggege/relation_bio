package com.feng.jiajia.service;

import com.feng.jiajia.model.Entity;
import com.feng.jiajia.model.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * 基于实体下标查找
 * 只找单句中的实体关系
 * 对应需求一
 */
public class RelationEntityBaseSingletonSentenceService extends  AbstractRelationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationEntityBaseSingletonSentenceService.class);

    public static final String RELATION_PATH = "data/relation/";

    public void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList) throws IOException {

        String path = BASE_PATH+SEN_SPL_TEXT_PATH+env+"/" + fileName;
        String name = fileName.split("\\.")[0];
        String savePath = BASE_PATH+RELATION_PATH+env+"/" + name +  RESULT_SUFFIX;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savePath), "utf-8"));
        String line = null;
        int offset = 0;
        while((line=br.readLine())!=null){

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

                        if(firstEntity.getMaxPoint().begin > secondEntity.getMaxPoint().begin){
                            Entity temp = firstEntity;
                            firstEntity = secondEntity;
                            secondEntity = temp;
                        }

                        if(firstEntity.getMaxPoint().end > secondEntity.getMaxPoint().begin){
                            LOGGER.error("实体间存在交叉，不处理：{}=={}", firstEntity, secondEntity );
                            continue;
                        }
                        int firstStart = firstEntity.getMaxPoint().begin-offset;
                        int firstEnd = firstEntity.getMaxPoint().end-offset;
                        String firstEntityName = line.substring(firstStart, firstEnd);

                        int secondStart = secondEntity.getMaxPoint().begin-offset;
                        int secondEnd = secondEntity.getMaxPoint().end-offset;
                        String secondEntityName = line.substring(secondStart, secondEnd);
                        String result = line.substring(0, firstStart) + (("Bacteria".equals(firstEntity.getType()))?"PROT_1":"PROT_2") +
                            line.substring(firstEnd, secondStart) + ("Bacteria".equals(secondEntity.getType())?"PROT_1":"PROT_2")+ line.substring(secondEnd);

                        if( (firstEntity.getIndexList().size()>1 || firstEntityName.equals(firstEntity.getName())) &&
                                secondEntity.getIndexList().size()>1 || secondEntityName.equals(secondEntity.getName())){
                            if(containsRelation(firstEntity.getAliasName(), secondEntity.getAliasName(), relationList)){
                                bw.write("+1\t" + result);
                                bw.newLine();
                            }else{
                                bw.write("-1\t" + result);
                                bw.newLine();
                            }
                        }else{
                            LOGGER.error("获取的值不匹配{}=={}", firstEntity, secondEntity);
                        }
                    }
                }
            }

            offset = last+1;
        }

        br.close();
        bw.close();
    }

    public static void main(String[] args) {
        RelationEntityBaseSingletonSentenceService service = new RelationEntityBaseSingletonSentenceService();
        service.buildRelation("train");
    }
}
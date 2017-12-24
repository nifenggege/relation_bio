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
 * 基于扩句子，可以自己制定跨句子的句子数 CROSS_SENTENCE_VALUE
 * 对应需求二
 */
public class RelationEntityBaseCrossSentenceService extends AbstractRelationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationEntityBaseCrossSentenceService.class);

    private static final int CROSS_SENTENCE_VALUE = 2;
    private static final String RELATION_PATH = "data/cross_relation_"+CROSS_SENTENCE_VALUE+"/";

    public void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList) throws IOException {

        String path = BASE_PATH+SEN_SPL_TEXT_PATH+env+"/" + fileName;
        String name = fileName.split("\\.")[0];
        String savePath = BASE_PATH+RELATION_PATH+env+"/" + name +  RESULT_SUFFIX;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savePath), "utf-8"));
        String line = null;
        List<String> sentenceList = Lists.newArrayList();
        while((line=br.readLine())!=null){
            sentenceList.add(line);
        }
        Set<String> resultSet = Sets.newHashSet();
        //分句子,跨x个句子就需要获取x种文件
        for(int m=0; m<CROSS_SENTENCE_VALUE; m++){
            List<String> crossSentenceList = Lists.newArrayList();
            String str = "";
            int index = 0;
            for(index=0; index<=m && index<sentenceList.size(); index++){
                str += sentenceList.get(index)+" ";
            }
            if(!StringUtils.isEmpty(str)) {
                crossSentenceList.add(str.substring(0, str.length() - 1));
            }
            while(index<sentenceList.size()) {
                String temp = "";
                for(int j=0; j<CROSS_SENTENCE_VALUE && index<sentenceList.size(); j++,index++){
                    temp +=sentenceList.get(index)+" ";
                }
                if(!StringUtils.isEmpty(temp)) {
                    crossSentenceList.add(temp.substring(0, temp.length() - 1));
                }
            }

            int offset = 0;
            for(int k=0; k<crossSentenceList.size(); k++){
                line = crossSentenceList.get(k);
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
                                    resultSet.add("+1\t" + result);
                                }else{
                                    resultSet.add("-1\t" + result);
                                }
                            }else{
                                LOGGER.error("获取的值不匹配{}=={}", firstEntity, secondEntity);
                            }
                        }
                    }
                }
                offset = last+1;
            }
        }

        for(String key : resultSet){
            bw.write(key);
            bw.newLine();
        }

        br.close();
        bw.close();
    }

    public static void main(String[] args) {
        RelationEntityBaseCrossSentenceService service = new RelationEntityBaseCrossSentenceService();
        service.buildRelation("train");
    }
}
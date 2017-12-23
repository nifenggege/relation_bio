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
 */
public class RelationEntityBaseCrossSentenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationEntityBaseCrossSentenceService.class);
    private static final String BASE_PATH = "E:\\idea_workspace\\relation_extraction\\src\\main\\resource\\";
    private static final int CROSS_SENTENCE_VALUE = 2;
    private static final String SEN_SPL_TEXT_PATH = "data/own_sentenes_split/";
    private static final String ORIGIN_A1_A2_PATH = "data/origin/";
    private static final String RELATION_PATH = "data/cross_relation_"+CROSS_SENTENCE_VALUE+"/";
    private static final String RESULT_SUFFIX = ".ins";


    /**
     * 组建句子中的关系
     * @param env train/dev/test
     * @return
     */
    public boolean buildRelation(String env){

        if(StringUtils.isEmpty(env) ||
                (!"train".equals(env) && !"dev".equals(env))){
            LOGGER.error("输入的环境不正确，请检查环境. env={}", env);
            return false;
        }

        processDocuments(env);
        return false;
    }

    private void processDocuments(String env) {

        String path = makesureFileExist(env);
        Set<String> documentSet = getDocuments(env);
        for(String name : documentSet){
            String fileName = BASE_PATH + SEN_SPL_TEXT_PATH + env + "/" + name;
            try {
                processDocument(env, name);
            } catch (FileNotFoundException e) {

            } catch (UnsupportedEncodingException e) {

            } catch (IOException e) {

            }
        }



    }

    /**
     * 处理单个文档
     * @param fileName
     */
    private void processDocument(String env, String fileName) throws IOException {

        
        //1 读取a1文件，返回Entity列表， Bacteria  Habitat  Geographical
        List<Entity> entityList = getEntityListFromA1(env, fileName);
        //2 读取a2文件, 返回Relation列表， equiv
        List<Relation> relationList = getRelationFromA2(env, fileName);
        //3 读取txt分析
        processTxt(env, fileName, entityList, relationList);
    }

    private void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList) throws IOException {

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
                List<Entity> containsEntity = getEntityLIstFromSentence(line, entityList, offset, last);
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

    private boolean containsRelation(String first, String second, List<Relation> relationList) {

        for(Relation relation : relationList){
            if((first.equals(relation.getFirst().aliasName) && second.equals(relation.getSecond().aliasName)) ||
                    (second.equals(relation.getFirst().aliasName) && first.equals(relation.getSecond().aliasName))){
                return true;
            }
        }
        return false;
    }

    private boolean matchRelation(String firstEntity, String secondEntity) {

        if(firstEntity.equals(secondEntity) ||
                ("Geographical".equals(firstEntity) && "Habitat".equals(secondEntity)) ||
                ("Habitat".equals(firstEntity) && "Geographical".equals(secondEntity))){
            return false;
        }
        return true;
    }

    private List<Entity> getEntityLIstFromSentence(String line, List<Entity> entityList, int begin, int end) {

        List<Entity> result = Lists.newArrayList();
        for(Entity entity : entityList){
            List<Entity.Point>  points = entity.getIndexList();
            boolean isContain = true;
            for(Entity.Point point : points){
                if(!(point.begin>=begin && point.end<end)){
                    isContain = false;
                }
            }
            if(isContain){
                result.add(entity);
            }
        }
        return result;
    }

    private List<Relation> getRelationFromA2(String env, String fileName) throws IOException {

        List<Relation> result = Lists.newArrayList();
        String name = fileName.split("\\.")[0];
        String path = BASE_PATH+ORIGIN_A1_A2_PATH+env+"/" + name+".a2";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        String line = null;
        while((line=br.readLine())!=null){
            if(line.contains("Equiv")){
                //特殊处理
                continue;
            }
            String[] tokens = line.split("(\\s)+");
            if(tokens.length!=4){
                LOGGER.error("a2 数据不规范：{}", line);
            }
            Relation relation = new Relation();
            relation.setFirst(tokens[2]);
            relation.setSecond(tokens[3]);
            result.add(relation);
        }
        br.close();
        return result;
    }

    private List<Entity> getEntityListFromA1(String env, String fileName) throws IOException {

        List<Entity> result = Lists.newArrayList();
        String name = fileName.split("\\.")[0];
        String path = BASE_PATH+ORIGIN_A1_A2_PATH+env+"/" + name+".a1";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        String line = null;
        while((line=br.readLine())!=null){
            String[] tokens = line.split("\t");
            if(tokens.length!=3){
                LOGGER.error("a1 数据不规范：{}", line);
            }

            String[] subTokens = tokens[1].split("(\\s)+");
            if(subTokens.length<3){
                LOGGER.error("a1 数据不规范：{}", line);
            }

            if(!("Habitat".equals(subTokens[0]) ||
                    "Bacteria".equals(subTokens[0]) ||
                    "Geographical".equals(subTokens[0]))){
                continue;
            }

            Entity entity = new Entity();
            entity.setAliasName(tokens[0]);
            entity.setType(subTokens[0]);
            entity.setIndexList(tokens[1].replaceAll(subTokens[0],"").trim());
            entity.setName(tokens[2]);
            result.add(entity);
        }
        br.close();
        return result;
    }

    private Set<String> getDocuments(String env) {
        String sourcePath = BASE_PATH+SEN_SPL_TEXT_PATH+env;
        File file = new File(sourcePath);
        String[] fileNames = file.list();
        if(fileNames==null){
            return Sets.newHashSet();
        }
        return Sets.newHashSet(fileNames);
    }

    private String makesureFileExist(String env) {
        File relationFile = new File(BASE_PATH+RELATION_PATH);
        if(!relationFile.exists()){
            relationFile.mkdir();
        }

        String resultPath = BASE_PATH+RELATION_PATH+env;
        File resultFile = new File(resultPath);
        if(!resultFile.exists()){
            resultFile.mkdir();
        }

        return resultFile.getAbsolutePath();
    }

    public static void main(String[] args) {
        RelationEntityBaseCrossSentenceService service = new RelationEntityBaseCrossSentenceService();
        service.buildRelation("train");
    }
}
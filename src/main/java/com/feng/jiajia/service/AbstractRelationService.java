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

public abstract class AbstractRelationService implements RelationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRelationService.class);
    public static final String BASE_PATH;
    public static final String SEN_SPL_TEXT_PATH = "data/own_sentenes_split/";
    public static final String ORIGIN_A1_A2_PATH = "data/origin/";
    public static final String RESULT_SUFFIX = ".ins";

    static{
        BASE_PATH = System.getProperty("user.dir") + "/src/main/resource/";
    }

    abstract String getSavePath();
    /**
     * 处理匹配好实体
     * @param relationList
     * @param offset
     * @param line
     * @param firstEntity
     * @param secondEntity
     */
    protected String processRelation(List<Relation> relationList, int offset, String line, Entity firstEntity, Entity secondEntity) {
        if(firstEntity.getMaxPoint().begin > secondEntity.getMaxPoint().begin){
            Entity temp = firstEntity;
            firstEntity = secondEntity;
            secondEntity = temp;
        }

        if(firstEntity.getMaxPoint().end > secondEntity.getMaxPoint().begin){
            LOGGER.error("实体间存在交叉，不处理：{}=={}", firstEntity, secondEntity );
            return null;
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
                return "+1\t" + result;
            }else{
                return "-1\t" + result;
            }
        }else{
            LOGGER.error("获取的值不匹配{}=={}", firstEntity, secondEntity);
        }
        return null;
    }

    /**
     * 写结果到文件
     * @param bw
     * @param resultSet
     * @throws IOException
     */
    protected void writeFile(BufferedWriter bw, Set<String> resultSet) throws IOException {
        for(String key : resultSet){
            bw.write(key);
            bw.newLine();
        }
    }

    /**
     * 获取句子，这里可以自定义如何获取
     * @param br
     * @return
     * @throws IOException
     */
    protected List<String> getSentenceList(BufferedReader br) throws IOException {

        List<String> result = Lists.newArrayList();
        String line = null;
        while((line=br.readLine())!=null){
            result.add(line);
        }
        return result;
    }

    /**
     * 根据env环境构建关系数据
     * @param env
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

    /**
     * 处理多个语料文件
     * @param env
     */
    private void processDocuments(String env) {

        String path = makesureFileExist(env);
        Set<String> documentSet = getDocuments(env);
        for (String name : documentSet) {
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
     * 获取该环境下的所有文件名
     * @param env
     * @return
     */
    private Set<String> getDocuments(String env) {
        String sourcePath = BASE_PATH+SEN_SPL_TEXT_PATH+env;
        File file = new File(sourcePath);
        String[] fileNames = file.list();
        if(fileNames==null){
            return Sets.newHashSet();
        }
        return Sets.newHashSet(fileNames);
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

    /**
     * 从a1文件中获取符合条件的实体
     * Bacteria  Habitat  Geographical
     * @param env
     * @param fileName
     * @return
     * @throws IOException
     */
    public List<Entity> getEntityListFromA1(String env, String fileName) throws IOException {

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

    /**
     * 从a2文件中获取正例关系
     * 特殊处理Equiv行
     * @param env
     * @param fileName
     * @return
     * @throws IOException
     */
    public List<Relation> getRelationFromA2(String env, String fileName) throws IOException {

        List<Relation> result = Lists.newArrayList();
        String name = fileName.split("\\.")[0];
        String path = BASE_PATH+ORIGIN_A1_A2_PATH+env+"/" + name+".a2";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        String line = null;
        List<String> equivList = Lists.newArrayList();
        while((line=br.readLine())!=null){
            if(line.contains("Equiv")){
                //特殊处理
                String[] subTokens = line.split("(\\s)+");
                for(int i=2; i<subTokens.length; i++){
                    equivList.add(subTokens[i]);
                }
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
        updateRelation(result, equivList);
        br.close();
        return result;
    }

    /**
     * 特殊处理Equiv行
     * @param result
     * @param equivList
     */
    private void updateRelation(List<Relation> result, List<String> equivList) {

        List<Relation> origin = Lists.newArrayList(result);
        for(Relation relation : origin){
            String first = relation.getFirst().aliasName;
            if(equivList.contains(first)){
                for(String token : equivList){
                    if(token.equals(first)){
                        continue;
                    }
                    Relation temp = new Relation();
                    temp.setFirst(relation.getFirst().type+":"+token);
                    temp.setSecond(relation.getSecond().type+":"+relation.getSecond().aliasName);
                    result.add(temp);
                }
                continue;
            }
            String second = relation.getSecond().aliasName;
            if(equivList.contains(second)){
                for(String token : equivList){
                    if(token.equals(second)){
                        continue;
                    }
                    Relation temp = new Relation();
                    temp.setFirst(relation.getFirst().type+":"+relation.getFirst().aliasName);
                    temp.setSecond(relation.getSecond().type+":"+token);
                    result.add(temp);
                }
            }
        }
    }

    /**
     * 获取一句中的实体
     * @param line
     * @param entityList
     * @param begin
     * @param end
     * @return
     */
    public List<Entity> getEntityListFromSentence(String line, List<Entity> entityList, int begin, int end) {

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

    /**
     * 判断是否为正例关系
     * @param first
     * @param second
     * @param relationList
     * @return
     */
    public boolean containsRelation(String first, String second, List<Relation> relationList) {

        for(Relation relation : relationList){
            if((first.equals(relation.getFirst().aliasName) && second.equals(relation.getSecond().aliasName)) ||
                    (second.equals(relation.getFirst().aliasName) && first.equals(relation.getSecond().aliasName))){
                return true;
            }
        }
        return false;
    }

    /**
     * 实体构成关系规则
     * @param firstEntityType
     * @param secondEntityType
     * @return
     */
    public boolean matchRelation(String firstEntityType, String secondEntityType) {

        if(firstEntityType.equals(secondEntityType) ||
                ("Geographical".equals(firstEntityType) && "Habitat".equals(secondEntityType)) ||
                ("Habitat".equals(firstEntityType) && "Geographical".equals(secondEntityType))){
            return false;
        }
        return true;
    }

    /**
     * 取保使用的文件存在
     * @param env
     * @return
     */
    private String makesureFileExist(String env) {
        File relationFile = new File(BASE_PATH+getSavePath());
        if(!relationFile.exists()){
            relationFile.mkdir();
        }

        String resultPath = BASE_PATH+getSavePath()+env;
        File resultFile = new File(resultPath);
        if(!resultFile.exists()){
            resultFile.mkdir();
        }

        return resultFile.getAbsolutePath();
    }



}
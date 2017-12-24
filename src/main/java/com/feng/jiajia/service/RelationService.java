package com.feng.jiajia.service;

import com.feng.jiajia.model.Entity;
import com.feng.jiajia.model.Relation;

import java.io.IOException;
import java.util.List;

/**
 * 关系抽取接口
 */
public interface RelationService {

    /**
     * 根据指定环境构建关系数据
     * @param env
     * @return
     */
    boolean buildRelation(String env);
    /**
     * 从a1文件中筛选出符合要求的实体
     *Habitat  Bacteria  Geographical
     * @param env
     * @param fileName
     * @return
     * @throws IOException
     */
    List<Entity> getEntityListFromA1(String env, String fileName) throws IOException;
    /**
     * 从a2文件中生成正例关系
     * @param env
     * @param fileName
     * @return
     * @throws IOException
     */
    List<Relation> getRelationFromA2(String env, String fileName) throws IOException;
    /**
     * 解析文本txt文件
     * @param env
     * @param fileName
     * @param entityList
     * @param relationList
     */
    void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList)throws IOException;

    /**
     * 从句子中获取该句子中的实体，所有的实体存储在entityList中， begin，end为句子的开始和结尾，偏移量以文章头开始算
     * @param line
     * @param entityList
     * @param begin
     * @param end
     * @return
     */
    List<Entity> getEntityListFromSentence(String line, List<Entity> entityList, int begin, int end);

    /**
     * 检查first与second是否存在关系，存在关系取决于a2文件生成的relationList
     * @param first
     * @param second
     * @param relationList
     * @return
     */
    boolean containsRelation(String first, String second, List<Relation> relationList);

    /**
     * 判断first与second是否符合组成关系条件
     * @param first
     * @param second
     * @return
     */
    boolean matchRelation(String first, String second);

}
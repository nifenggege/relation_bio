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
 * 基于实体个数查找，个数不限制可以设置为MAX_DISTINCE_ENTITY=Integer.MAX_VALUE
 * 对应需求三、四
 */
public class RelationEntityBaseEntityNumberService extends AbstractRelationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationEntityBaseEntityNumberService.class);
    private static final int MAX_DISTINCE_ENTITY = 5;
    private static final String RELATION_PATH = "data/relation_base_entity_num" + MAX_DISTINCE_ENTITY+"/";

    public void processTxt(String env, String fileName, List<Entity> entityList, List<Relation> relationList) throws IOException {

        String path = BASE_PATH+SEN_SPL_TEXT_PATH+env+"/" + fileName;
        String name = fileName.split("\\.")[0];
        String savePath = BASE_PATH+RELATION_PATH+env+"/" + name +  RESULT_SUFFIX;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savePath), "utf-8"));
        String line = null;
        String content = "";
        while((line=br.readLine())!=null) {
            content += line + " ";
        }
        if(StringUtils.isEmpty(content)) {
           return;
        }
        content = content.substring(0, content.length() - 1);
        Set<String> resultSet = Sets.newHashSet();
        for(int i=0; i<entityList.size(); i++){
            Entity first = entityList.get(i);
            for(int j=i+1; j<entityList.size(); j++){
                Entity second = entityList.get(j);
                if(matchRelation(first.getType(), second.getType())){

                    if(first.getMaxPoint().end <= second.getMaxPoint().begin){
                        if(containsRelation(first.getAliasName(), second.getAliasName(), relationList)){
                            resultSet.add("+1\t" + (("Bacteria".equals(first.getType()))?"PROT_1":"PROT_2") +
                                    content.substring(first.getMaxPoint().end, second.getMaxPoint().begin) +
                                    (("Bacteria".equals(second.getType()))?"PROT_1":"PROT_2"));
                        }else{
                            resultSet.add("-1\t" + (("Bacteria".equals(first.getType()))?"PROT_1":"PROT_2") +
                                    content.substring(first.getMaxPoint().end, second.getMaxPoint().begin) +
                                    (("Bacteria".equals(second.getType()))?"PROT_1":"PROT_2"));
                        }
                    }else{
                        LOGGER.error("实体间存在交叉，不处理：{}#######{}", first, second);
                    }
                }
            }
        }
        writeFile(bw, resultSet);

        br.close();
        bw.close();
    }

    String getSavePath() {
        return RELATION_PATH;
    }

    public static void main(String[] args) {
        RelationEntityBaseEntityNumberService service = new RelationEntityBaseEntityNumberService();
        service.buildRelation("train");
    }
}
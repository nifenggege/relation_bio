package com.feng.jiajia.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Set;

/**
 * 分句服务
 * 主要是为了按照官方进行分句，又能保留原始语料中的offset
 */
public class SentenceSplitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentenceSplitService.class);

    public static final String BASE_PATH;

    static{
        BASE_PATH = System.getProperty("user.dir") + "/src/main/resource/";
    }

    public static void getOwnSentence(String env){

        String sentencePath = BASE_PATH + "data\\sentencesplit\\";
        String ownSentencePath = BASE_PATH + "data\\own_sentenes_split\\";
        String originPath = BASE_PATH + "data\\origin\\";
        File file = new File(sentencePath+env);
        File ownSentenceFile = new File(ownSentencePath+env);
        if(!ownSentenceFile.exists()){
            ownSentenceFile.mkdir();
        }
        for(String fileName : file.list()){
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sentencePath+env+"/"+fileName), "utf-8"));
                BufferedReader originBr = new BufferedReader(new InputStreamReader(new FileInputStream(originPath+env+"/"+fileName)));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ownSentencePath+env+"/"+fileName)));
                String line = null;
                List<String> list = Lists.newArrayList();
                while((line=br.readLine())!=null){
                    list.add(line);
                }
                br.close();

                while((line=originBr.readLine()) !=null){
                    int index = -1;
                    int offset = 0;
                    while((index=line.indexOf(".", index+1))!=-1){
                        String str = line.substring(offset, index+1);
                        if(str.replaceAll("(\\s)+","").equals(list.get(0).replaceAll("(\\s)+", ""))){
                            list.remove(0);
                            offset = index+2;
                            bw.write(str);
                            bw.newLine();
                        }
                    }
                }
                originBr.close();
                bw.close();
                br.close();
            } catch (UnsupportedEncodingException e) {

            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }

    }

    public static Set<String> getDeliterChar(){

        Set<String> set = Sets.newHashSet();
        File file = new File("E:\\idea_workspace\\relation_extraction\\src\\main\\resource\\data\\sentencesplit\\train");
        for(String fileName : file.list()){
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath()+"/"+fileName), "utf-8"));
                String line = null;
                while((line=br.readLine())!=null){
                    set.add(line.substring(line.length()-1));
                }
                br.close();
            } catch (UnsupportedEncodingException e) {

            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }
        return set;
    }

    public static void main(String[] args) {
        getOwnSentence("train");
    }
}
package com.feng.jiajia.model;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Entity {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entity.class);

    private String aliasName;
    private String name;
    private String type;
    private List<Point> indexList;

    public static class Point{
        public int begin;
        public int end;

        @Override
        public String toString() {
            return "Point{" +
                    "begin=" + begin +
                    ", end=" + end +
                    '}';
        }
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Point> getIndexList() {
        return indexList;
    }

    public void setIndexList(String str) {

        List<Point> indexList = Lists.newArrayList();
        if(str.contains(";")){
            //多个区间的情况
            String[] tokens = str.split(";");
            for(String token : tokens){
                Point point = buildPoint(token);
                if(point!=null) {
                    indexList.add(point);
                }
            }
            this.indexList = indexList;
            return;
        }

        Point point = buildPoint(str);
        if(point!=null) {
            indexList.add(point);
        }
        this.indexList = indexList;
    }

    private Point buildPoint(String token) {


        String[] tokens = token.split("(\\s)+");
        if(tokens.length!=2){
            LOGGER.error("实体区间错误：{}", token);
            return null;
        }
        Point point = new Point();
        point.begin = Integer.parseInt(tokens[0]);
        point.end = Integer.parseInt(tokens[1]);
        return point;
    }

    public Point getMaxPoint(){
        Point point = new Point();
        point.begin = Integer.MAX_VALUE;
        for(Point p : this.getIndexList()){
            if(p.begin<point.begin){
                point.begin = p.begin;
            }

            if(p.end > point.end){
                point.end = p.end;
            }
        }

        return point;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "aliasName='" + aliasName + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", indexList=" + indexList +
                '}';
    }
}
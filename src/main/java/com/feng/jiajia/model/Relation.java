package com.feng.jiajia.model;

public class Relation {

    private RelationEntity first;
    private RelationEntity second;

    public static class RelationEntity{
        public String type;
        public String aliasName;
    }

    public RelationEntity getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = buildRelationEntity(first);
    }

    private RelationEntity buildRelationEntity(String first) {

        RelationEntity entity = new RelationEntity();
        String[] tokens = first.split(":");
        entity.aliasName = tokens[1];
        entity.type = tokens[0];
        return entity;
    }

    public RelationEntity getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = buildRelationEntity(second);;
    }
}
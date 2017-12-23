package com.feng.jiajia.model;

public class Sentence {

    private String sentence;
    private int firstSentenceLength;

    public Sentence(String sentence, int firstSentenceLength) {
        this.sentence = sentence;
        this.firstSentenceLength = firstSentenceLength;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public int getFirstSentenceLength() {
        return firstSentenceLength;
    }

    public void setFirstSentenceLength(int firstSentenceLength) {
        this.firstSentenceLength = firstSentenceLength;
    }
}
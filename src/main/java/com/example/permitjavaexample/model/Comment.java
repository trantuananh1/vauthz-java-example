package com.example.permitjavaexample.model;

public class Comment {
    private final Integer id;
    private final String author;
    private String content;

    public Comment(Integer id, String author, String content) {
        this.id = id;
        this.author = author;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }
}

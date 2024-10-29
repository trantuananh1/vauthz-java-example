package com.example.permitjavaexample.model;

import java.util.ArrayList;
import java.util.List;

public class Blog {
    private final Integer id;
    private final String author;
    private String content;
    private final List<Comment> comments = new ArrayList<>();

    public Blog(Integer id, String author, String content) {
        this.id = id;
        this.author = author;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }
}

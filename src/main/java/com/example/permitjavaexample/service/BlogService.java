package com.example.permitjavaexample.service;

import com.example.permitjavaexample.exception.ResourceNotFoundException;
import com.example.permitjavaexample.model.Blog;
import com.example.permitjavaexample.model.Comment;
import io.permit.sdk.Permit;
import io.permit.sdk.api.PermitApiError;
import io.permit.sdk.api.PermitContextError;
import io.permit.sdk.enforcement.Resource;
import io.permit.sdk.enforcement.User;
import io.permit.sdk.openapi.models.RelationshipTupleCreate;
import io.permit.sdk.openapi.models.ResourceInstanceCreate;
import io.permit.sdk.openapi.models.RoleAssignmentCreate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BlogService {
    private final List<Blog> blogs = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final AtomicInteger blogIdCounter = new AtomicInteger();
    private final AtomicInteger commentIdCounter = new AtomicInteger();

    private final Resource.Builder blogResourceBuilder = new Resource.Builder("blog");
    private final Resource.Builder commentResourceBuilder = new Resource.Builder("comment");
    private final UserService userService;
    private final Permit permit;


    public BlogService(UserService userService, Permit permit) {
        this.userService = userService;
        this.permit = permit;
    }

    private void authorize(User user, String action, Resource resource) {
        userService.authorize(user, action, resource);
    }

    private void authorize(User user, String action, Blog blog) {
        var attributes = new HashMap<String, Object>();
        attributes.put("author", blog.getAuthor());
        userService.authorize(user, action, blogResourceBuilder.withKey(blog.getId().toString()).withAttributes(attributes).build());
    }

    private void authorize(User user, String action, Comment comment) {
        var attributes = new HashMap<String, Object>();
        attributes.put("author", comment.getAuthor());
        userService.authorize(user, action, commentResourceBuilder.withKey(comment.getId().toString()).withAttributes(attributes).build());
    }

    private Blog getBlogById(int id) {
        return blogs.stream().filter(blog -> blog.getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Blog with id " + id + " not found"));
    }

    public List<Blog> getAllBlogs(User user) {
        authorize(user, "read", blogResourceBuilder.build());
        return new ArrayList<>(blogs);
    }

    public Blog getBlog(User user, int id) {
        authorize(user, "read", blogResourceBuilder.build());
        return getBlogById(id);
    }

    public Blog addBlog(User user, String content) {
        authorize(user, "create", blogResourceBuilder.build());
        Blog blog = new Blog(blogIdCounter.incrementAndGet(), user.getKey(), content);

        try {
            permit.api.resourceInstances.create(new ResourceInstanceCreate(blog.getId().toString(), "blog").withTenant("default"));
            permit.api.roleAssignments.assign(new RoleAssignmentCreate("author", user.getKey()).withResourceInstance("blog:" + blog.getId()).withTenant("default"));
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException("Failed to create resource instance or role assignment: " + e.getMessage());
        }
        blogs.add(blog);
        return blog;
    }

    public Blog updateBlog(User user, int id, String content) {
        Blog blog = getBlogById(id);
        authorize(user, "update", blog);
        blog.setContent(content);
        return blog;
    }

    public void deleteBlog(User user, int id) {
        boolean isDeleted = blogs.removeIf(blog -> {
            if (blog.getId().equals(id)) {
                authorize(user, "delete", blog);
                return true;
            } else {
                return false;
            }
        });
        if (!isDeleted) {
            throw new ResourceNotFoundException("Blog with id " + id + " not found");
        }
        try {
            permit.api.resourceInstances.delete("blog:" + id);
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException(e);
        }
    }

    public Comment addComment(User user, int blogId, String content) {
        authorize(user, "create", commentResourceBuilder.build());
        Blog blog = getBlogById(blogId);
        Comment comment = new Comment(commentIdCounter.incrementAndGet(), user.getKey(), content);
        try {

            permit.api.resourceInstances.create(new ResourceInstanceCreate(comment.getId().toString(), "comment").withTenant("default"));
            permit.api.relationshipTuples.create(new RelationshipTupleCreate("blog:" + blogId, "parent", "comment:" + commentIdCounter));
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException(e);
        }
        blog.addComment(comment);
        return comment;
    }

    public Comment updateComment(User user, int blogId, int commentId, String content) {
        Blog blog = getBlogById(blogId);
        Comment comment = blog.getComments().stream().filter(c -> c.getId().equals(commentId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Comment with id " + commentId + " not found"));
        authorize(user, "update", comment);
        comment.setContent(content);
        return comment;
    }

    public void deleteComment(User user, int blogId, int commentId) {
        Blog blog = getBlogById(blogId);
        boolean isDeleted = blog.getComments().removeIf(comment -> {
            if (comment.getId().equals(commentId)) {
                authorize(user, "delete", comment);
                return true;
            } else {
                return false;
            }
        });
        if (!isDeleted) {
            throw new ResourceNotFoundException("Comment with id " + commentId + " not found");
        }
        try {
            permit.api.resourceInstances.delete("comment:" + commentId);
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException(e);
        }
    }
}

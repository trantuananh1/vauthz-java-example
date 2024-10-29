package com.example.permitjavaexample;

import com.example.permitjavaexample.model.Blog;
import com.example.permitjavaexample.service.BlogService;
import com.example.permitjavaexample.service.UserService;
import io.permit.sdk.Permit;
import io.permit.sdk.api.PermitApiError;
import io.permit.sdk.api.PermitContextError;
import io.permit.sdk.enforcement.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows non-static @BeforeAll
public class BlogControllerIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private BlogService blogService;
    @Autowired
    private UserService userService;
    @Autowired
    private Permit permit;

    private String baseUrl;
    private User viewer1;
    private User viewer2;
    private User editor1;
    private User editor2;
    private User admin;
    private Blog blog1;
    private Blog blog2;

    @BeforeAll
    void setUpAll() {
        cleanup();

        baseUrl = "http://localhost:" + port;

        viewer1 = userService.signup("viewer-user-1");
        userService.assignRole(viewer1, "viewer");

        viewer2 = userService.signup("viewer-user-2");
        userService.assignRole(viewer2, "viewer");

        editor1 = userService.signup("editor-user-1");
        userService.assignRole(editor1, "editor");

        editor2 = userService.signup("editor-user-2");
        userService.assignRole(editor2, "editor");

        admin = userService.signup("admin-user-1");
        userService.assignRole(admin, "admin");

        blog1 = blogService.addBlog(editor1, "First Blog Post");
        blog2 = blogService.addBlog(editor2, "Second Blog Post");
    }

    void cleanup() {
        try {
            var resources = permit.api.resourceInstances.list();
            System.out.println("Cleaning up " + resources.length + " resources");
            Arrays.stream(resources).parallel().forEach(resource -> {
                try {
                    permit.api.resourceInstances.delete(resource.id);
                } catch (PermitApiError | PermitContextError | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException | PermitApiError | PermitContextError e) {
            throw new RuntimeException(e);
        }
    }

    ResponseEntity<String> sendRequest(String url, HttpMethod method, User user, Object body) {
        if (user == null)
            return restTemplate.exchange(baseUrl + url, method, null, String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getKey());
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(baseUrl + url, method, entity, String.class);
    }

    ResponseEntity<String> sendRequest(String url, HttpMethod method, User user) {
        return sendRequest(url, method, user, null);
    }

    @Test
    void listBlogsUnauthenticated() {
        // unauthenticated user fails to get all blogs
        var response = sendRequest("/api/blogs", HttpMethod.GET, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listBlogsAsUnknown() {
        // unknown user fails to get all blogs
        User user = new User.Builder("someone").build();
        var response = sendRequest("/api/blogs", HttpMethod.GET, user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listBlogs() {
        // viewer1 gets all blogs
        var response = sendRequest("/api/blogs", HttpMethod.GET, viewer1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getBlogById() {
        // viewer1 gets blog1
        var response = sendRequest("/api/blogs/" + blog1.getId(), HttpMethod.GET, viewer1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getBlogByIdNotFound() {
        // viewer1 fails to get non-existent blog
        var response = sendRequest("/api/blogs/999", HttpMethod.GET, viewer1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBlog() {
        // editor1 creates a blog
        var response = sendRequest("/api/blogs", HttpMethod.POST, editor1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createBlogForbidden() {
        // viewer1 fails to create a blog
        var response = sendRequest("/api/blogs", HttpMethod.POST, viewer1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateOwnBlog() {
        // editor1 updates their own blog
        var response = sendRequest("/api/blogs/" + blog1.getId(), HttpMethod.PUT, editor1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateOthersBlogForbidden() {
        // editor1 fails to update editor2's blog
        var response = sendRequest("/api/blogs/" + blog2.getId(), HttpMethod.PUT, editor1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateBlogNotFound() {
        // editor1 fails to update non-existent blog
        var response = sendRequest("/api/blogs/999", HttpMethod.PUT, editor1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOwnBlog() {
        // editor1 deletes their own blog
        var blog = blogService.addBlog(editor1, "Test Content");
        var response = sendRequest("/api/blogs/" + blog.getId(), HttpMethod.DELETE, editor1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteOthersBlogForbidden() {
        // editor1 fails to delete editor2's blog
        var response = sendRequest("/api/blogs/" + blog2.getId(), HttpMethod.DELETE, editor1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteOthersBlogAsAdmin() {
        // admin deletes editor1's blog
        var blog = blogService.addBlog(editor1, "Test Content");
        var response = sendRequest("/api/blogs/" + blog.getId(), HttpMethod.DELETE, admin);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    void deleteBlogNotFound() {
        // editor1 fails to delete non-existent blog
        var response = sendRequest("/api/blogs/999", HttpMethod.DELETE, editor1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createComment() {
        // viewer1 comments on blog1
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment", HttpMethod.POST, viewer1, "Test Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateOwnComment() {
        // viewer1 updates their own comment on their own blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.PUT, viewer1, "Updated Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteOwnComment() {
        // viewer1 deletes their own comment on blog1
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.DELETE, viewer1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateOthersCommentForbidden() {
        // viewer2 fails to update viewer1's comment on viewer1's blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.PUT, viewer2, "Updated Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteOthersCommentOnOwnBlog() throws InterruptedException {
        // editor1 deletes viewer1's comment on editor1's blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        Thread.sleep(1000); // Wait for PDP to sync
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.DELETE, editor1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateOthersCommentOnOwnBlogForbidden() {
        // editor1 deletes viewer1's comment on editor1's blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.PUT, editor1, "Updated Content");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteOthersCommentOnOthersBlogsForbidden() {
        // editor2 fails to delete viewer1's comment on editor1's blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.DELETE, editor2);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteOthersCommentOnOthersBlogAsAdmin() {
        // admin deletes viewer1's comment on editor1's blog
        var comment = blogService.addComment(viewer1, blog1.getId(), "Test Content");
        var response = sendRequest("/api/blogs/" + blog1.getId() + "/comment/" + comment.getId(), HttpMethod.DELETE, admin);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

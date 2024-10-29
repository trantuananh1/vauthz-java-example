package com.example.permitjavaexample;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.permit.sdk.Permit;
import io.permit.sdk.openapi.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class StartupData implements CommandLineRunner {
    @Autowired
    Permit permit;

    @Override
    public void run(String... args) throws Exception {
        try {
            //resources
            //blog
            HashMap<String, ActionBlockEditable> actions = new HashMap<>();
            actions.put("create", new ActionBlockEditable());
            actions.put("read", new ActionBlockEditable());
            actions.put("update", new ActionBlockEditable());
            actions.put("delete", new ActionBlockEditable());

            HashMap<String, AttributeBlockEditable> attributes = new HashMap<>();
            attributes.put(
                    "author",
                    new AttributeBlockEditable().withType(AttributeType.STRING).withDescription("The user key who created the blog")
            );
            ResourceCreate blogCreate = ((
                    new ResourceCreate("blog", "Blogs", actions).withAttributes(attributes)
            ));
            ResourceRead blog = permit.api.resources.create(blogCreate);
//        ResourceRead blog = permit.api.resources.get("blog");
            //comment
            actions.clear();
            actions = new HashMap<>();
            actions.put("create", new ActionBlockEditable());
            actions.put("read", new ActionBlockEditable());
            actions.put("delete", new ActionBlockEditable());
            attributes.clear();
            attributes.put(
                    "author",
                    new AttributeBlockEditable().withType(AttributeType.STRING).withDescription("The user key who created the comment")
            );
            ResourceCreate commentCreate = ((
                    new ResourceCreate("comment", "Comments", actions).withAttributes(attributes)
            ));
            ResourceRead comment = permit.api.resources.create(commentCreate);
//        ResourceRead comment = permit.api.resources.get("comment");
            // resource set
            String conditionStr = """
                    {
                        "allOf" : [
                          {
                            "allOf" : [
                              { "resource.author" : { "equals" : { "ref" : "user.key" } } }
                            ]
                          }
                        ]
                    }
                    """;
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(conditionStr, HashMap.class);
            ConditionSetRead ownBlog = permit.api.conditionSets.create(
                    new ConditionSetCreate("own_blog", "Own Blogs").withResourceId(blog.id).withConditions(map).withType(ConditionSetType.RESOURCESET)
            );
//            ConditionSetRead ownBlog = permit.api.conditionSets.get("own_blog");
            ConditionSetRead ownComment = permit.api.conditionSets.create(
                    new ConditionSetCreate("own_comment", "Own Comments").withResourceId(comment.id).withConditions(map).withType(ConditionSetType.RESOURCESET)
            );
//            ConditionSetRead ownComment = permit.api.conditionSets.get("own_comment");

            //relation
            //blog_comment_relation
            RelationRead blogCommentRelation = permit.api.resourceRelations.create("comment", new RelationCreate("parent", "Blog parent of Comment", blog.key)).withObjectResource(comment.key);

            //resource role
            //blog_author
            ResourceRoleRead blogAuthor = permit.api.resourceRoles.create(blog.key, new ResourceRoleCreate("author", "author")
                    .withDescription("Update and delete own blogs")
                    .withPermissions(List.of("update", "delete")));
            //comment_moderator
            ResourceRoleRead commentModerator = permit.api.resourceRoles.create(comment.key, new ResourceRoleCreate("moderator", "moderator")
                    .withDescription("Delete comments on own blogs")
                    .withPermissions(List.of("delete")));

            //role derivation
            //blog_author_comment_moderator
            permit.api.resourceRoles.createRoleDerivation(comment.key, commentModerator.key,
                    new DerivedRoleRuleCreate(blogAuthor.key, blog.key, blogCommentRelation.key));

            //role
            RoleRead viewer = permit.api.roles.create(
                    new RoleCreate("viewer", "viewer")
                            .withDescription("Read and comment on all blogs")
                            .withPermissions(List.of("blog:read", "comment:create")));
//            RoleRead viewer = permit.api.roles.get("viewer");
            RoleRead editor = permit.api.roles.create(
                    new RoleCreate("editor", "editor")
                            .withDescription("Create blogs, update and delete them, and delete comments on them")
                            .withPermissions(List.of("blog:read", "blog:create")));
//            RoleRead editor = permit.api.roles.get("editor");
            RoleRead admin = permit.api.roles.create(
                    new RoleCreate("admin", "admin")
                            .withDescription("Delete any blog or comment")
                            .withPermissions(List.of("blog:delete", "comment:delete")));
//            RoleRead admin = permit.api.roles.get("admin");

            //condition set rule
            //allow_editors_to_update_own_blogs
            permit.api.conditionSetRules.create(
                    new ConditionSetRuleCreate(editor.key, "blog:update", ownBlog.key)
            );
            //allow_editors_to_delete_own_blogs
            permit.api.conditionSetRules.create(
                    new ConditionSetRuleCreate(editor.key, "blog:delete", ownBlog.key)
            );
            //allow_viewers_to_update_own_comments
            permit.api.conditionSetRules.create(
                    new ConditionSetRuleCreate(viewer.key, "comment:update", ownComment.key)
            );
            //allow_viewers_to_delete_own_comments
            permit.api.conditionSetRules.create(
                    new ConditionSetRuleCreate(viewer.key, "comment:delete", ownComment.key)
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            permit.api.resources.delete("blog");
//            permit.api.resources.delete("comment");
//            permit.api.conditionSets.delete("own_blog");
//            permit.api.conditionSets.delete("own_comment");
//            permit.api.roles.delete("viewer");
//            permit.api.roles.delete("admin");
//            permit.api.roles.delete("editor");
//            permit.api.conditionSetRules.delete(new ConditionSetRuleRemove("editor", "blog:update", "own_blog"));
//            permit.api.conditionSetRules.delete(new ConditionSetRuleRemove("editor", "blog:delete", "own_blog"));
//            permit.api.conditionSetRules.delete(new ConditionSetRuleRemove("viewer", "comment:update", "own_comment"));
//            permit.api.conditionSetRules.delete(new ConditionSetRuleRemove("viewer", "comment:delete", "own_comment"));
        }
    }
}

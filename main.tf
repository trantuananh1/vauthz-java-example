terraform {
  required_providers {
    permitio = {
      source  = "permitio/permit-io"
      version = "~> 0.0.1"
    }
  }
}

variable "permit_api_key" {
  type        = string
  description = "The API key for the Permit.io API"
}

provider "permitio" {
  api_url = "https://api.permit.io"
  api_key = "permit_key_pSlixRcGXVq6A8wmKLXFn44OiXaHZxjM2spkZ8NxbVz6e8JTP0yBNC5jR0VuDegSFjADmcsHRDecMBOcSnSbuB"
}

resource "permitio_resource" "blog" {
  key     = "blog"
  name    = "Blogs"
  actions = {
    "create" = { "name" = "create" }
    "read"   = { "name" = "read" }
    "update" = { "name" = "update" }
    "delete" = { "name" = "delete" }
  }
  attributes = {
    "author" = {
      "description" = "The user key who created the blog"
      "type"        = "string"
    }
  }
}

resource "permitio_resource_set" "own_blog" {
  key        = "own_blog"
  name       = "Own Blogs"
  resource   = permitio_resource.blog.key
  conditions = jsonencode({
    "allOf" : [
      {
        "allOf" : [
          { "resource.author" : { "equals" : { "ref" : "user.key" } } },
        ],
      },
    ],
  })
  depends_on = [
    permitio_resource.blog,
  ]
}

resource "permitio_resource" "comment" {
  key     = "comment"
  name    = "Comments"
  actions = {
    "create" = { "name" = "create" }
    "update" = { "name" = "update" }
    "delete" = { "name" = "delete" }
  }
  attributes = {
    "author" = {
      "description" = "The user key who created the comment"
      "type"        = "string"
    }
  }
}

resource "permitio_resource_set" "own_comment" {
  key        = "own_comment"
  name       = "Own Comments"
  resource   = permitio_resource.comment.key
  conditions = jsonencode({
    "allOf" : [
      {
        "allOf" : [
          { "resource.author" : { "equals" : { "ref" : "user.key" } } },
        ],
      },
    ],
  })
  depends_on = [
    permitio_resource.comment,
  ]
}

resource "permitio_relation" "blog_comment_relation" {
  key              = "parent"
  name             = "Blog parent of Comment"
  subject_resource = permitio_resource.blog.key
  object_resource  = permitio_resource.comment.key
  depends_on       = [
    permitio_resource.blog,
    permitio_resource.comment,
  ]
}

resource "permitio_role" "blog_author" {
  key         = "author"
  name        = "author"
  description = "Update and delete own blogs"
  resource    = permitio_resource.blog.key
  permissions = ["update", "delete"]
  depends_on  = [
    permitio_resource.blog,
  ]
}

resource "permitio_role" "comment_moderator" {
  key         = "moderator"
  name        = "moderator"
  description = "Delete comments on own blogs"
  resource    = permitio_resource.comment.key
  permissions = ["delete"]
  depends_on  = [
    permitio_resource.comment,
  ]
}

// Derive blog#author to comment#moderator for child comments
resource "permitio_role_derivation" "blog_author_comment_moderator" {
  role        = permitio_role.blog_author.key
  on_resource = permitio_resource.blog.key
  resource    = permitio_resource.comment.key
  to_role     = permitio_role.comment_moderator.key
  linked_by   = permitio_relation.blog_comment_relation.key
  depends_on  = [
    permitio_resource.blog,
    permitio_resource.comment,
    permitio_role.comment_moderator,
    permitio_role.blog_author,
    permitio_relation.blog_comment_relation,
  ]
}

resource "permitio_role" "viewer" {
  key         = "viewer"
  name        = "viewer"
  description = "Read and comment on all blogs"
  permissions = ["blog:read", "comment:create"]
  depends_on  = [
    permitio_resource.blog,
    permitio_resource.comment,
  ]
}

resource "permitio_role" "editor" {
  key         = "editor"
  name        = "editor"
  description = "Create blogs, update and delete them, and delete comments on them"
  permissions = ["blog:read", "blog:create"]
  depends_on  = [
    permitio_resource.blog,
  ]
}

resource "permitio_role" "admin" {
  key         = "admin"
  name        = "admin"
  description = "Delete any blog or comment"
  permissions = ["blog:delete", "comment:delete"]
  depends_on  = [
    permitio_resource.blog,
    permitio_resource.comment,
  ]
}

// Give 'editor' role permissions to 'own_blog:update'
resource "permitio_condition_set_rule" "allow_editors_to_update_own_blogs" {
  user_set     = permitio_role.editor.key
  resource_set = permitio_resource_set.own_blog.key
  permission   = "blog:update"
  depends_on   = [
    permitio_resource_set.own_blog,
  ]
}

// Give 'editor' role permissions to 'own_blog:delete'
resource "permitio_condition_set_rule" "allow_editors_to_delete_own_blogs" {
  user_set     = permitio_role.editor.key
  resource_set = permitio_resource_set.own_blog.key
  permission   = "blog:delete"
  depends_on   = [
    permitio_resource_set.own_blog,
  ]
}

// Give 'viewer' role permissions to 'own_comment:update'
resource "permitio_condition_set_rule" "allow_viewers_to_update_own_comments" {
  user_set     = permitio_role.viewer.key
  resource_set = permitio_resource_set.own_comment.key
  permission   = "comment:update"
  depends_on   = [
    permitio_resource_set.own_comment,
  ]
}

// Give 'viewer' role permissions to 'own_comment:delete'
resource "permitio_condition_set_rule" "allow_viewers_to_delete_own_comments" {
  user_set     = permitio_role.viewer.key
  resource_set = permitio_resource_set.own_comment.key
  permission   = "comment:delete"
  depends_on   = [
    permitio_resource_set.own_comment,
  ]
}

# Blog Application with Permit.io Authorization

This example project showcases a simple blog application that implements authorization using permit.io.

## Features

- **User Authentication**: Users can log in to the application using their credentials.
- **Blog Management**: Authenticated users can create new blog posts, read existing posts, update their posts, and
  delete them, subject to their authorization level.
- **Authorization with permit.io**: The application uses permit.io to manage user roles and permissions, ensuring that
  access to certain actions (like updating or deleting a blog post) is appropriately restricted.
- **Role-Based Access Control**: Defines different roles for users, such as viewer, editor, and admin, each with varying
  levels of access and permissions.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing
purposes.

### Prerequisites

Before you begin, ensure you have the following installed:

- Java JDK 21
- Gradle
- Docker (optional, for running Permit.io PDP locally)
- Terraform (optional, for setting up Permit.io resources)

### 1. Setting Up Permit.io using Terraform

Login to your Permit.io account and create a new project in your workspace and copy your API key.
Set the `PERMIT_API_KEY` environment variable to your API key:

```shell
export PERMIT_API_KEY=<YOUR_API_KEY>
```
Then run the following commands to create the resources in your Permit.io project:
```shell
terraform init
terraform plan
terraform apply
```

You should see under the Policy Editor a "Blog" and "Owned Blog" resources, and the roles `viewer`, `editor`, and `admin`.
![image](https://github.com/permitio/permit-java-example/assets/12188774/d1fd9a57-aae0-42c1-a3e5-81e9af912b91)

### 2. Starting your local PDP container

The PDP (Policy Decision Point) is the component of permit.io that evaluates access control decisions.
You can run a local PDP container using Docker:

```shell
docker run -it -p 7766:7000 --env PDP_DEBUG=True --env PDP_API_KEY=<YOUR_API_KEY> permitio/pdp-v2:latest
```

> You may use the Cloud PDP by modifying the `src/resources/application.yaml` config, setting the `permit.pdpUrl` property to https://cloudpdp.api.permit.io. 
> It is not recommended as it does not support ABAC policies used in this project.

### 3. Running the Application

You can run the application using the following Gradle command:

```shell
./gradlew bootRun
```

Then access the application Swagger at http://localhost:8080/swagger-ui/index.html.

## Usage

The application contains a makeshift user authentication. Create a new user using the `/api/users/signup` endpoint:
```shell
curl -X POST "http://localhost:8080/api/users/signup" -H "Content-Type: application/json" -d 'myuser'
```
You should be able to view the user in your Permit.io project, under Directory > All Tenants.

Initially the user has no roles, therefor it cannot do much. For example, trying to list the blogs will result in a 403 Forbidden response:
```shell
curl -X GET "http://localhost:8080/api/blogs" -H "Authorization: Bearer myuser"
# 403 Forbidden
```
Assign the user with a `viewer` role using:
```shell
curl -X POST "http://localhost:8080/api/users/assign-role" -H "Authorization: Bearer myuser" -d "viewer"
```

Alternatively, you can use the Swagger UI authorize with the username.

### Example Usage

The application contains simple APIs for creating, reading, updating, and deleting blog and comment resources.
Here is an example of how to use the application:

#### 1. Reading Blogs
With your 'viewer' role, you can read the blogs. List all blogs using:
```shell
curl -X GET "http://localhost:8080/api/blogs" -H "Authorization: Bearer my-user"
```
Initially, there are no blogs, so lets try to create one using:
```shell
curl -X POST "http://localhost:8080/api/blogs" -H "Authorization: Bearer my-user" -H "Content-Type: application/json" -d 'This is my blog'
# 403 Forbidden
```
Turns out viewers cannot create blogs. You can assign your user with an `editor` role using:
```shell
curl -X POST "http://localhost:8080/api/users/assign-role" -H "Authorization: Bearer my-user" -d "editor"
```
Then you can create a blog using:
```shell
curl -X POST "http://localhost:8080/api/blogs" -H "Authorization: Bearer my-user" -H "Content-Type: application/json" -d 'This is my blog'
```
Now you can read the blog using (assuming the blog ID is 1):
```shell
curl -X GET "http://localhost:8080/api/blogs/1" -H "Authorization: Bearer my-user"
```

#### 2. Commenting on blogs
Let's create another user and assign it with a `viewer` role:
```shell
curl -X POST "http://localhost:8080/api/users/signup" -H "Content-Type: application/json" -d 'other-user'
curl -X POST "http://localhost:8080/api/users/assign-role" -H "Authorization: Bearer myviewer" -d "viewer"
```
Now, the user can comment our the blog using:
```shell
curl -X POST "http://localhost:8080/api/blogs/1/comments" -H "Authorization: Bearer other-user" -H "Content-Type: application/json" -d 'This blog is not good!'
```
But it wasn't enough, so it wanted to update the comment:
```shell
curl -X POST "http://localhost:8080/api/blogs/1/comments" -H "Authorization: Bearer other-user" -H "Content-Type: application/json" -d 'This blog is awful!'
```
The negative comment by `other-user` was not well received by `my-user`, so it wanted to change it:
```shell
curl -X PUT "http://localhost:8080/api/blogs/1/comments/1" -H "Authorization: Bearer my-user" -H "Content-Type: application/json" -d 'This blog is great!'
# 403 Forbidden
```
Although being the author of the blog, `my-user` cannot update other user's comments, but it can delete it:
```shell
curl -X DELETE "http://localhost:8080/api/blogs/1/comments/1" -H "Authorization: Bearer my-user"
```
Feeling sorry, `other-user` left another comment:
```shell
curl -X POST "http://localhost:8080/api/blogs/1/comments" -H "Authorization: Bearer other-user" -H "Content-Type: application/json" -d 'This blog is great!'
```
But regretted it immediately and wanted to delete it:
```shell
curl -X DELETE "http://localhost:8080/api/blogs/1/comments/2" -H "Authorization: Bearer other-user"
```

#### 3. Modifying Blogs
With the negative comments on it's blog, `my-user` wanted to update the blog:
```shell
curl -X PUT "http://localhost:8080/api/blogs/1" -H "Authorization: Bearer my-user" -H "Content-Type: application/json" -d 'This is my updated blog'
```
But it wasn't enough, eventually it decided to delete the blog:
```shell
curl -X DELETE "http://localhost:8080/api/blogs/1" -H "Authorization: Bearer my-user"
```
Then, it created a new blog:
```shell
curl -X POST "http://localhost:8080/api/blogs" -H "Authorization: Bearer my-user" -H "Content-Type: application/json" -d 'I dont like @other-user comments...'
```

#### 4. Admin Access
Personal blogs are not allowed in the application, so an admin user is needed to delete them.
Create an admin user and assign it with an `admin` role:
```shell
curl -X POST "http://localhost:8080/api/users/signup" -H "Content-Type: application/json" -d 'admin-user'
curl -X POST "http://localhost:8080/api/users/assign-role" -H "Authorization: Bearer admin-user" -d "admin"
```
Now the admin can delete the blog using (assuming the blog ID is 2):
```shell
curl -X DELETE "http://localhost:8080/api/blogs/2" -H "Authorization: Bearer admin-user"
```

## Testing

This example project contains integration tests that demonstrate the authorization flow using permit.io. 

You can run all tests using the following Gradle command:
```shell
./gradlew test
```

First, the test suite create a viewer, editor and admin users, and assign them with the respective roles.
Then, it asserts:
* Unauthenticated users or unknown users cannot access the API.
* Viewer can read blogs, but cannot create, update, or delete them. (RBAC)
* Editor can read and create blogs, and can update or delete their own blogs. (ABAC)
* Editor cannot update or delete other user's blogs. 
* Viewer can comment on blogs, and can update or delete their own comments. (ABAC)
* Blog author (derives to be comment moderator) can delete comments on their own blogs. (ReBAC)
* Admin can delete other user's blogs and comments.

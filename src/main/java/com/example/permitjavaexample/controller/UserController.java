package com.example.permitjavaexample.controller;


import com.example.permitjavaexample.exception.UnauthorizedException;
import com.example.permitjavaexample.service.UserService;
import io.permit.sdk.enforcement.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public User signup(@RequestBody String key) {
        return userService.signup(key);
    }

    @PostMapping("/assign-role")
    public void assignRole(HttpServletRequest request, @RequestBody String role) {
        User currentUser = (User) request.getAttribute("user");
        if (currentUser == null) {
            throw new UnauthorizedException("Not logged in");
        }
        userService.assignRole(currentUser, role);
    }
}

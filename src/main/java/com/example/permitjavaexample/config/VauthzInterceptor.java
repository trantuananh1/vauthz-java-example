package com.example.permitjavaexample.config;

import com.example.permitjavaexample.service.UserService;
import io.permit.sdk.Permit;
import io.permit.sdk.enforcement.Resource;
import io.permit.sdk.enforcement.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class VauthzInterceptor implements HandlerInterceptor {

    private final Permit permit;


    public VauthzInterceptor(Permit permit) {
        this.permit = permit;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            VauthzCheck vauthzCheck = method.getAnnotation(VauthzCheck.class);
            if (vauthzCheck != null) {
                // Extract user key and other necessary information from the request
                User user = (User) request.getAttribute("user");
                Resource resource = Resource.fromString(vauthzCheck.resource());
                String action = vauthzCheck.action();

                // Perform the permission check
                boolean isAllowed = permit.check(user, action, resource);

                if (!isAllowed) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Access Denied");
                    return false;
                }
            }
        }
        return true;
    }
}
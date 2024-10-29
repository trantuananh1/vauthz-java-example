package com.example.permitjavaexample.service;

import com.example.permitjavaexample.exception.ResourceNotFoundException;
import com.example.permitjavaexample.model.Folder;
import io.permit.sdk.Permit;
import io.permit.sdk.api.PermitApiError;
import io.permit.sdk.api.PermitContextError;
import io.permit.sdk.enforcement.Resource;
import io.permit.sdk.enforcement.User;
import io.permit.sdk.openapi.models.ResourceInstanceCreate;
import io.permit.sdk.openapi.models.RoleAssignmentCreate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FolderService {
    private final List<Folder> folders = new ArrayList<>();
    private final AtomicInteger folderIdCounter = new AtomicInteger();

    private final UserService userService;
    private final Permit permit;

    public FolderService(UserService userService, Permit permit) {
        this.userService = userService;
        this.permit = permit;
    }
    
    public Folder createFolder(User user, String name) {
        Folder folder = new Folder(folderIdCounter.incrementAndGet(), user.getKey(), name);

        try {
            permit.api.resourceInstances.create(new ResourceInstanceCreate(folder.getId().toString(), "folder").withTenant("default"));
            permit.api.roleAssignments.assign(new RoleAssignmentCreate("author", user.getKey()).withResourceInstance("folder:" + folder.getId()).withTenant("default"));
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException("Failed to create resource instance or role assignment: " + e.getMessage());
        }
        folders.add(folder);
        return folder;
    }

    private Folder getFolderById(int id) {
        return folders.stream().filter(folder -> folder.getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Folder with id " + id + " not found"));
    }
    
    public List<Folder> getAllFolders(User user) {
        return new ArrayList<>(folders);
    }

    public Folder getFolder(User user, int id) {
        return getFolderById(id);
    }

    public Folder updateFolder(User user, int id, String name) {
        Folder folder = getFolderById(id);
        folder.setName(name);
        return folder;
    }

    public void deleteFolder(User user, int id) {
        boolean isDeleted = folders.removeIf(folder -> {
            if (folder.getId().equals(id)) {
                return true;
            } else {
                return false;
            }
        });
        if (!isDeleted) {
            throw new ResourceNotFoundException("Folder with id " + id + " not found");
        }
        try {
            permit.api.resourceInstances.delete("folder:" + id);
        } catch (IOException | PermitApiError | PermitContextError e) {
            // In production code you should consider action atomicity, and rollback the action.
            throw new RuntimeException(e);
        }
    }

    public void share(String folderId, String sharedUserId, String role) {
        try {
            permit.api.roleAssignments.assign(new RoleAssignmentCreate(role, sharedUserId).withResourceInstance("folder:" + folderId).withTenant("default"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
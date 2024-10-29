package com.example.permitjavaexample.controller;

import com.example.permitjavaexample.config.VauthzCheck;
import com.example.permitjavaexample.model.Folder;
import com.example.permitjavaexample.service.FolderService;
import io.permit.sdk.enforcement.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;

    @Autowired
    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public List<Folder> getAllFolders(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("user");
        return folderService.getAllFolders(currentUser);
    }

    @GetMapping("/{id}")
    public Folder getFolderById(HttpServletRequest request, @PathVariable("id") int id) {
        User currentUser = (User) request.getAttribute("user");
        return folderService.getFolder(currentUser, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Folder addFolder(HttpServletRequest request, @RequestBody String content) {
        User currentUser = (User) request.getAttribute("user");
        return folderService.createFolder(currentUser, content);
    }

    @PutMapping("/{id}")
    public Folder updateFolder(HttpServletRequest request, @PathVariable("id") int id, @RequestBody String content) {
        User currentUser = (User) request.getAttribute("user");
        return folderService.updateFolder(currentUser, id, content);
    }

    @DeleteMapping("/{id}")
    public String deleteFolder(HttpServletRequest request, @PathVariable("id") int id) {
        User currentUser = (User) request.getAttribute("user");
        folderService.deleteFolder(currentUser, id);
        return "Deleted Folder with id " + id;
    }
    
    @PostMapping("{id}/share/{sharedUserId}")
    public void share(@PathVariable String id, 
                      @PathVariable String sharedUserId,
                      @RequestBody String role) {
        folderService.share(id, sharedUserId, role);
    }
}

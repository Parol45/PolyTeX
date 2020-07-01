package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.restservice.MainApplication;
import ru.test.restservice.entity.Template;
import ru.test.restservice.entity.User;
import ru.test.restservice.service.AdminService;
import ru.test.restservice.service.ProjectService;
import ru.test.restservice.utils.Setting;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;
    private final ProjectService projectService;

    @GetMapping("/restart")
    public void restart() {
        MainApplication.restart();
    }

    @GetMapping("/update-latex")
    public void updatePackages() {
        // TODO:
        //TerminalUtils.executeTerminalCommand("tlmgr option repository ftp://tug.org/historic/systems/texlive/2019/tlnet-final", "/");
        //TerminalUtils.executeTerminalCommand("tlmgr update --force --all", "/");
    }

    @PostMapping("/register")
    public User register(@RequestParam("email") String email, @RequestParam("password") String password) {
        return adminService.register(email, password);
    }

    @GetMapping("/grant-admin")
    public void grantAdmin(@RequestParam UUID id) {
        adminService.grantAdmin(id);
    }

    @GetMapping("/revoke-admin")
    public void revokeAdmin(@RequestParam UUID id) {
        adminService.revokeAdmin(id);
    }

    @GetMapping("/block")
    public void blockUser(@RequestParam UUID id) {
        adminService.blockUser(id);
    }

    @GetMapping("/unblock")
    public void unblockUser(@RequestParam UUID id) {
        adminService.unblockUser(id);
    }

    @PostMapping("/update-settings")
    public void updateSettings(@RequestBody List<Setting> settings) throws IOException {
        adminService.setNewSettings(settings);
    }

    @PostMapping("/{projectId}/add-owner")
    public void addOwner(@PathVariable UUID projectId, @RequestParam String email, @RequestParam String owner) {
        projectService.addOwner(projectId, email, owner);
    }

    @DeleteMapping("/{projectId}/remove-owner")
    public void removeOwner(@PathVariable UUID projectId, @RequestParam String email, @RequestParam String owner) {
        projectService.removeOwner(projectId, email, owner);
    }

    @DeleteMapping("/{projectId}")
    public void deleteProject(@PathVariable UUID projectId, @RequestParam String owner) {
        projectService.deleteProject(projectId, owner);
    }

    @PostMapping(value = "/templates", headers = "content-type=multipart/*")
    public Template createTemplate(@RequestParam String templateName, @RequestParam("file") MultipartFile file, @RequestParam String templateDescription) throws IOException {
        return adminService.createTemplate(templateName, file, templateDescription);
    }

    @DeleteMapping("/templates")
    public void deleteTemplate(@RequestParam UUID templateId) {
        adminService.deleteTemplate(templateId);
    }

}

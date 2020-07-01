package ru.test.restservice.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(description="API админа")
public class AdminApiController {

    private final AdminService adminService;
    private final ProjectService projectService;

    @GetMapping("/restart")
    @ApiOperation("Перезапуск контекста сервера")
    public void restart() {
        MainApplication.restart();
    }

    @GetMapping("/update-latex")
    @ApiOperation("Обновление пакетов LaTeX-дистрибутива")
    public void updatePackages() {
        // TODO:
        //TerminalUtils.executeTerminalCommand("tlmgr option repository ftp://tug.org/historic/systems/texlive/2019/tlnet-final", "/");
        //TerminalUtils.executeTerminalCommand("tlmgr update --force --all", "/");
    }

    @PostMapping("/register")
    @ApiOperation("Добавление нового локального пользователя приложения")
    public User register(
            @ApiParam(value = "Имя нового пользователя", required=true)
            @RequestParam("email") String email,
            @ApiParam(value = "Пароль нового пользователя", required=true)
            @RequestParam("password") String password) {
        return adminService.register(email, password);
    }

    @GetMapping("/grant-admin")
    @ApiOperation("Добавление админских прав пользователю по его id")
    public void grantAdmin(
            @ApiParam(value = "Id пользователя", required=true)
            @RequestParam UUID id) {
        adminService.grantAdmin(id);
    }

    @GetMapping("/revoke-admin")
    @ApiOperation("Удаление админских прав пользователя по его id")
    public void revokeAdmin(
            @ApiParam(value = "Id пользователя", required=true)
            @RequestParam UUID id) {
        adminService.revokeAdmin(id);
    }

    @GetMapping("/block")
    @ApiOperation("Блокировка пользователя")
    public void blockUser(
            @ApiParam(value = "Id пользователя", required=true)
            @RequestParam UUID id) {
        adminService.blockUser(id);
    }

    @GetMapping("/unblock")
    @ApiOperation("Разблокировка пользователя")
    public void unblockUser(
            @ApiParam(value = "Id пользователя", required=true)
            @RequestParam UUID id) {
        adminService.unblockUser(id);
    }

    @PostMapping("/update-settings")
    @ApiOperation("Установка новых системных настроек")
    public void updateSettings(
            @ApiParam(value = "Id шаблона", required=true)
            @RequestBody List<Setting> settings) throws IOException {
        adminService.setNewSettings(settings);
    }

    @PostMapping("/{projectId}/add-owner")
    @ApiOperation("Добавление пользователя в любой LaTeX проект")
    public void addOwner(
            @PathVariable UUID projectId,
            @ApiParam(value = "Имя нового владельца проекта", required=true)
            @RequestParam String email,
            @ApiParam(value = "Имя создателя проекта", required=true)
            @RequestParam String owner) {
        projectService.addOwner(projectId, email, owner);
    }

    @DeleteMapping("/{projectId}/remove-owner")
    @ApiOperation("Удаление пользователя из любого LaTeX проекта")
    public void removeOwner(
            @PathVariable UUID projectId,
            @ApiParam(value = "Имя пользователя удяляемого из проекта", required=true)
            @RequestParam String email,
            @ApiParam(value = "Имя создателя проекта", required=true)
            @RequestParam String owner) {
        projectService.removeOwner(projectId, email, owner);
    }

    @DeleteMapping("/{projectId}")
    @ApiOperation("Удаление любого LaTeX проекта")
    public void deleteProject(
            @PathVariable UUID projectId,
            @ApiParam(value = "Имя создателя проекта", required=true)
            @RequestParam String owner) {
        projectService.deleteProject(projectId, owner);
    }

    @PostMapping(value = "/templates", headers = "content-type=multipart/*")
    @ApiOperation("Загрузка нового LaTeX проекта")
    public Template createTemplate(
            @ApiParam(value = "Имя создаваемого шаблона", required=true)
            @RequestParam String templateName,
            @ApiParam(value = "Zip-архив с файлами шаблона", required=true)
            @RequestParam("file") MultipartFile file,
            @ApiParam(value = "Описание создаваемого шаблона", required=true)
            @RequestParam String templateDescription) throws IOException {
        return adminService.createTemplate(templateName, file, templateDescription);
    }

    @DeleteMapping("/templates")
    @ApiOperation("Удаление шаблона LaTeX проекта")
    public void deleteTemplate(@RequestParam UUID templateId) {
        adminService.deleteTemplate(templateId);
    }

}

angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        let projectName = document.querySelector("#project-name");
        // Отвечает за отображение типа создания проекта: шаблон/архив (templ/upload)
        $scope.type = "templ";
        $scope.projects = projects;
        $scope.username = username;
        $scope.isAdmin = admin;
        $scope.templates = templates;

        $scope.createNewProject = function () {
            let selectedTemplate = document.querySelector("#template-select");
            if (projectName.value.length <= 50) {
                if (projectName.value.length > 0) {
                    $http.post(`/api/projects/?projectName=${projectName.value}&templateId=${selectedTemplate.value}`).then(
                        (response) => {
                            $scope.projects.push(response.data);
                            projectName.value = "";
                        }, (response) => {
                            $scope.showError(response);
                        });
                } else {
                    $scope.showError("Пустое имя проекта");
                }
            } else {
                $scope.showError("Имя проекта слишком длинное");
            }
        };

        $scope.uploadProject = function () {
            let uploadFile = document.querySelector("#upload-file");
            if (projectName.value.length > 0 && uploadFile.files[0]) {
                let fd = new FormData();
                fd.append("projectName", projectName.value);
                fd.append("file", uploadFile.files[0]);
                $http.post("/api/projects/upload-project", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then((response) => {
                        $scope.projects.push(response.data);
                        projectName.value = "";
                    },
                    (response) => {
                        $scope.showError(response);
                    }
                );
            } else {
                $scope.showError("Заполните все поля");
            }
        };

        $scope.addOwner = function (projectId) {
            let email = document.querySelector("#add-" + projectId);
            if (email.value.length <= 50) {
                if (!$scope.projects.find(p => p.id === projectId).owners.find(o => o === email.value)) {
                    if (email.value.length > 0) {
                        $http.post(`/api/projects/${projectId}/add-owner?email=${email.value}`).then(
                            () => {
                                $scope.projects.find(p => p.id === projectId).owners.push(email.value);
                                email.value = "";
                            }, (response) => {
                                $scope.showError(response);
                            });
                    } else {
                        $scope.showError("Введите имя пользователя");
                    }
                } else {
                    $scope.showError("Пользователь уже добавлен");
                }
            } else {
                $scope.showError("Имя пользователя слишком длинное");
            }
        };

        $scope.deleteOwner = function (projectId) {
            let email = document.querySelector("#del-" + projectId);
            if (email.value) {
                $http.post(`/api/projects/${projectId}/remove-owner?email=${email.value}`).then(
                    () => {
                        let projectUsers = $scope.projects.find(p => p.id === projectId).owners;
                        projectUsers.splice(projectUsers.indexOf(projectUsers.find(u => u === email.value)), 1);
                        if (projectUsers.length === 0) {
                            $scope.projects.splice($scope.projects.indexOf($scope.projects.find(p => p.id === projectId)), 1);
                        }
                    }, (response) => {
                        $scope.showError(response);
                    });
            } else {
                $scope.showError("Выберите пользователя для удаления");
            }
        };

        $scope.deleteProject = function (projectId) {
            if (window.confirm("Вы уверены?")) {
                $http.delete(`/api/projects/${projectId}/`).then(
                    () => {
                        $scope.projects.splice($scope.projects.indexOf($scope.projects.find(p => p.id === projectId)), 1);
                    }, (response) => {
                        $scope.showError(response);
                    });
            }
        };

        $scope.leaveProject = function (projectId) {
            $http.post(`/api/projects/${projectId}/remove-yourself/`).then(
                () => {
                    $scope.projects.splice($scope.projects.indexOf($scope.projects.find(p => p.id === projectId)), 1);
                }, () => {
                    $scope.showError();
                });
        };

        $scope.showError = function (message = "Что-то пошло не так") {
            if (typeof message === "string") {
                alert(message);
            } else {
                if (message.data.status === 500) {
                    alert(message.data.message);
                } else if (message.data.status === 404) {
                    alert("Не найден");
                }
                console.log(message);
            }
        };
    }]);
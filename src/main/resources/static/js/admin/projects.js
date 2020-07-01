angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {
        $scope.projects = projects;
        $scope.searchOwner = "";

        $scope.ownerFilter = function (project) {
            let result = false;
            project.owners.forEach(p => p.match($scope.searchOwner) ? result = true : result);
            return result;
        };

        $scope.addOwner = function (projectId, creatorName) {
            let email = document.querySelector("#add-" + projectId);
            if (email.value.length > 0) {
                $http.post(`/admin/api/${projectId}/add-owner?email=${email.value}&owner=${creatorName}`).then(
                    () => {
                        $scope.projects.find(p => p.id === projectId).owners.push(email.value);
                        email.value = "";
                    }, () => {
                        $scope.showError();
                    });
            } else {
                $scope.showError("Пустое поле ввода имени пользователя");
            }
        };

        $scope.deleteOwner = function (projectId, creatorName) {
            let email = document.querySelector("#del-" + projectId);
            if (email.value) {
                $http.delete(`/admin/api/${projectId}/remove-owner?email=${email.value}&owner=${creatorName}`).then(
                    () => {
                        let projectUsers = $scope.projects.find(p => p.id === projectId).owners;
                        projectUsers.splice(projectUsers.indexOf(projectUsers.find(u => u === email.value)), 1);
                        if (projectUsers.length === 0) {
                            $scope.projects.splice($scope.projects.indexOf($scope.projects.find(p => p.id === projectId)), 1);
                        }
                    }, () => {
                        $scope.showError();
                    });
            } else {
                $scope.showError("Выберите пользователя для удаления из проекта");
            }
        };

        $scope.deleteProject = function (projectId, creatorName) {
            if (window.confirm("Вы уверены?")) {
                $http.delete(`/admin/api/${projectId}?owner=${creatorName}`).then(
                    () => {
                        $scope.projects.splice($scope.projects.indexOf($scope.projects.find(p => p.id === projectId)), 1);
                    }, () => {
                        $scope.showError();
                    });
            }
        };

        $scope.showError = function (message = "Что-то пошло не так") {
            if (typeof message === "string") {
                alert(message);
            } else {
                if (message.data.status === 409) {
                    window.location.reload(true);
                } else if (message.data.status === 500) {
                    alert(message.data.message);
                } else if (message.data.status === 404) {
                    alert("Не найден");
                }
                console.log(message);
            }
        };
    }]);
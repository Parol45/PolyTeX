angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        let projectName = document.querySelector("#projectName");
        $scope.projects = projects;

        $scope.createNewProject = function () {
            if (projectName.value.length > 0) {
                $http.post("/api/projects/?projectName=" + projectName.value).then(
                    (response) => {
                        $scope.projects.push(response.data);
                        projectName.value = "";
                    }, () => {
                        $scope.showError();
                    });
            } else {
                $scope.showError("Empty project name");
            }
        };

        $scope.addOwner = function (projectId) {
            let email = document.querySelector("#add-" + projectId);
            if (email.value.length > 0) {
                $http.post("/api/projects/" + projectId +"/add-owner?email=" + email.value).then(
                    () => {
                        let projectUsers = $scope.projects.find(p => p.id === projectId).owners;
                        projectUsers.push(email.value);
                        email.value = "";
                    }, () => {
                        $scope.showError();
                    });
            } else {
                $scope.showError("Empty user email");
            }
        };

        $scope.deleteOwner = function (projectId) {
            let email = document.querySelector("#del-" + projectId);
            $http.post("/api/projects/" + projectId +"/remove-owner?email=" + email.value).then(
                () => {
                    let projectUsers = $scope.projects.find(p => p.id === projectId).owners;
                    projectUsers.splice(projectUsers.indexOf(projectUsers.find(u => u === email)));
                }, () => {
                    $scope.showError();
                });
        };

        $scope.deleteProject = function (projectId) {
            $http.delete("/api/projects/" + projectId).then(() => {
                let deletedProj = $scope.projects.find(p => p.id === projectId);
                $scope.projects.splice($scope.projects.indexOf(deletedProj), 1);
            }, () => {
                $scope.showError();
            });
        };

        $scope.showError = function (message = "Something went wrong") {
            // TODO: поменять на что-нибудь вразумительное
            alert(message);
        };
    }]);
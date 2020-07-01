angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        // Сразу ищу элементы, чтобы удобнее было к ним обращаться
        let templateName = document.querySelector("#template-name");
        let templateDescription = document.querySelector("#template-description");
        let uploadFile = document.querySelector("#upload-file");
        $scope.templates = templates;

        $scope.createNewTemplate = function () {
            if (templateName.value.length > 0 && uploadFile.files[0]) {
                let fd = new FormData();
                fd.append("templateName", templateName.value);
                fd.append("file", uploadFile.files[0]);
                fd.append("templateDescription", templateDescription.value);
                $http.post("/admin/api/templates", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then((response) => {
                        $scope.templates.push(response.data);
                        templateName.value = "";
                        templateDescription.value = "";
                    },
                    (response) => {
                        $scope.showError(response);
                    }
                );
            } else {
                $scope.showError("Заполните все поля");
            }
        };

        $scope.deleteTemplate = function (id) {
            $http.delete(`/admin/api/templates?templateId=${id}`).then((response) => {
                $scope.templates.splice($scope.templates.indexOf($scope.templates.find(t => t.id === id)), 1);
            }, (response) => {
                $scope.showError(response);
            });
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
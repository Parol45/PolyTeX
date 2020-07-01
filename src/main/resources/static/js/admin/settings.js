angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {
        $scope.settings = {...settings};

        $scope.sendSettings = function () {
            let sendSettings = [];
            let validProperties = true, reloaded = false;
            angular.forEach($scope.settings, (value, key) => {
                if ($scope.settings[key] !== settings[key]) {
                    if (value.match(/[^\d]+/) !== null) {
                        validProperties = false;
                    }
                    let obj = {};
                    obj["property"] = key;
                    obj["value"] = value;
                    sendSettings.push(obj);
                }
            });
            if (validProperties) {
                let includeUploadSize = sendSettings.map(s => s.property).includes("maxUploadFileSize");
                if (includeUploadSize) {
                    reloaded = window.confirm("Для изменений настроек необходима перезагрузка сервера. Вы уверены?");
                }
                if (sendSettings.length !== 0 && (includeUploadSize === reloaded)) {
                    $http.post("/admin/api/update-settings", sendSettings).then(() => {
                            settings = {...$scope.settings};
                            alert("Настройки обновлены");
                            if (reloaded) {
                                window.location.reload(true);
                            }
                        },
                        (response) => {
                            $scope.showError(response);
                        });
                } else {
                    $scope.showError("Настройки не были изменены");
                }
            } else {
                $scope.showError("Проверьте правильность введённых настроек");
            }
        };

        $scope.updatePackages = function () {
            if (window.confirm("Это займёт много времени. Вы уверены?")) {
                $http.get("/admin/api/update-latex").then(() => {
                        $scope.alert("Пакеты обновлены");
                    },
                    (response) => {
                        $scope.showError(response);
                    });
            }
        };

        $scope.restart = function () {
            if (window.confirm("Подтвердите перезапуск сервера")) {
                $http.get("/admin/api/restart").then(() => {
                        window.location.reload(true);
                    },
                    (response) => {
                        $scope.showError(response);
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
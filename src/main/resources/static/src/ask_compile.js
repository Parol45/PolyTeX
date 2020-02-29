angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        $scope.docs = [];

        //При загрузке обращение к серверу и получение списка существующих в рабочей папке файлов
        $http.get("/api/filelist/").then(
            (response) => {
                angular.forEach(response.data.list, function (value, key) {
                    $scope.docs.push({name: value})
                });
            },
            () => {
                document.querySelector("#result-wrap").innerText = "Error in data transfer";
            });

        // Пока что отправка текста из div на сервер для компиляции только его и получение пути к пдфнику
        // Чтение напрямую с сервера стоит убрать, я думаю
        $scope.sendForCompilation = function () {
            $http.post("/api/compile/", document.querySelector("#source-wrap").innerText).then(
                (response) => {
                    if (response.data.message === "ok")
                        document.querySelector("#result-wrap").innerHTML = "<embed class='document' src=\"test/test.pdf\"/>";
                    else
                        document.querySelector("#result-wrap").innerText = response.data.message;
                },
                () => {
                    document.querySelector("#result-wrap").innerText = "Error in data transfer";
                });
        };

        $scope.uploadFile = function (file) {
            if (typeof file !== 'undefined' && !document.getElementById("li-" + file.name)) {
                let fd = new FormData();
                fd.append("file", file);
                $http.post("/api/upload/", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then(() => $scope.docs.push({name: file.name}),
                    () => alert("Something went wrong")
                );
            } else
                alert("There are already file with this name");
        };

    }]);
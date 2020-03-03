angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        // Тут храню инфу о полученных файлах при загрузке страницы
        $scope.files = [];
        $scope.fileTypes = [];
        $scope.fileContents = [];
        // Файл, содержимое которого отображается в данный момент
        $scope.openedFile = "";

        //При загрузке страницы обращение к серверу и получение списка существующих в рабочей папке файлов
        $http.get("/api/filelist/").then(
            (response) => {
                // Цикл с добавлением списка файлов в список слева и отображение первого текстового
                angular.forEach(response.data, function (value, key) {
                    $scope.files.push({name: value.fileName});
                    $scope.fileTypes.push(value.fileType);
                    $scope.fileContents.push(value.content);
                    if ($scope.openedFile === "" && $scope.fileTypes[key] === "txt") {
                        $scope.openedFile = $scope.files[key];
                        $scope.printFile(key);
                    }
                });
            },
            () => {
                document.querySelector("#result-wrap").innerText = "Error in data transfer";
            });

        // Пока что отправка текста из div на сервер для компиляции только его и получение пути к пдфнику
        $scope.sendForCompilation = function () {
            $http.post("/api/compile/", document.querySelector("#source-wrap").innerText).then(
                (response) => {
                    if (response.data.pathToPdf !== "") {
                        document.querySelector("#result-wrap").innerHTML =
                            "<embed class='document' src=" + response.data.pathToPdf + "/>";
                    } else {
                        document.querySelector("#result-wrap").innerText = response.data.compilerMessage;
                    }
                },
                (response) => {
                    document.querySelector("#result-wrap").innerText = response.data.compilerMessage;
                });
        };

        // Функция загрузки файла с допустимым содержимым на сервер (ещё проверяю есть ли с таким же именем
        // и выбран ли он вообще, потому что при нажатии отмены при втором выборе файла срабатывает onchange)
        $scope.uploadFile = function (file) {
            let reg = /.(tex|bib|png|jpg|svg)$/i;
            if (typeof file !== 'undefined' && !document.getElementById("li-" + file.name)
                && reg.test(file.name)) {
                let fd = new FormData();
                fd.append("file", file);
                // Этот пост запрос стырил с гитхаба
                $http.post("/api/upload/", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then((response) => {
                        $scope.files.push({name: response.data.fileName});
                        $scope.fileTypes.push(response.data.fileType);
                        $scope.fileContents.push(response.data.content);
                    },
                    () => alert("Something went wrong")
                );
            } else
                alert("Bad file");
        };

        // Обработка события выбора файла из левого списка
        $scope.showFile = function (link) {
            // id элемента списка формируется как "li-" + его имя
            let file = link.id.substr(3);
            // Нахождение в списке по значению слишком сложное, так что обхожу все
            angular.forEach($scope.files, (value, key) => {
                if (value.name === file) {
                    document.querySelector("#source-wrap").innerHTML = "";
                    $scope.openedFile = file;
                    $scope.printFile(key);
                }
            })
        };

        // Заполнение области для редактирования содержимым файла из переменной
        $scope.printFile = function (key) {
            angular.forEach($scope.fileContents[key], function (value, k) {
                let newLine = document.createElement('div');
                newLine.innerHTML = value === "" ? "<br>" : value;
                document.querySelector("#source-wrap").appendChild(newLine);
            });
        };

        $scope.saveDoc = function () {

        };
    }]);
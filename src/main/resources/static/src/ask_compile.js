angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        // TODO: событие с отсылкой изменений при закрытии страницы
        let sourceArea = document.querySelector("#source-wrap");
        // Тут храню инфу о полученных файлах при загрузке страницы
        $scope.files = [];
        // Индекс файла, содержимое которого отображается в данный момент
        $scope.openedFileIndex = -1;
        $scope.pathToPdf = "";

        //При загрузке страницы обращение к серверу и получение списка существующих в рабочей папке файлов
        $http.get("/api/files/").then(
            (response) => {
                // Цикл с добавлением списка файлов в список слева и отображение первого текстового
                angular.forEach(response.data, function (value, key) {
                    $scope.files.push({name: value.name, type: value.type, content: value.content});
                    if ($scope.openedFileIndex === -1 && $scope.files[key].type === "txt") {
                        $scope.openedFileIndex = key;
                        $scope.printFile(key);
                    }
                });
            },
            () => {
                $scope.showError();
            });

        // Пока что отправка текста из div на сервер для компиляции только его и получение пути к пдфнику
        $scope.sendForCompilation = function () {
            $http.post("/api/compile/", sourceArea.innerText.replace(/\n\n/g, "\n")).then(
                (response) => {
                    if (response.data.pathToPdf !== "") {
                        document.querySelector("#result-wrap").innerHTML = "<embed class='document' src=" + response.data.pathToPdf + "/>";
                        // Для этого продвинутого метода надо добавить вкладку с инфой об ошибках,
                        // потому что пока что ошибки заменяют собой тэг embed и выводятся вместо него
                        // TODO: заменить на:
                        // $scope.pathToPdf = response.data.pathToPdf;
                    } else {
                        document.querySelector("#result-wrap").innerText = response.data.compilerMessage;
                    }
                },
                () => {
                    $scope.showError();
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
                $http.post("/api/upload/", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then((response) => {
                        $scope.files.push({
                            name: response.data.name,
                            type: response.data.type,
                            content: response.data.content
                        });
                    },
                    () => {
                        $scope.showError();
                    }
                );
            } else
                $scope.showError("Bad file");
        };

        // Обработка события выбора файла из левого списка
        $scope.showFile = function (link) {
            // id элемента списка формируется как "li-" + его имя
            let name = link.id.substr(3);
            let key = $scope.files.find(f => f.name === name);
            $scope.saveOpenedDocLocally();
            $scope.openedFileIndex = key;
            $scope.printFile(key);
        };

        // Заполнение области для редактирования содержимым файла из переменной
        $scope.printFile = function (key) {
            sourceArea.innerHTML = "";
            angular.forEach($scope.files[key].content, function (value, k) {
                let newLine = document.createElement('div');
                newLine.innerHTML = value === "" ? "<br>" : value;
                sourceArea.appendChild(newLine);
            });
        };

        // Формирую список dto объектов и отправляю на сервер
        $scope.saveDocs = function () {
            let files = $scope.listTxtDocs();
            $http.put("/api/files/", files).then(() => {
                alert("Saved!");
            }, () => {
                $scope.showError();
            });
        };

        // Формирование списка FileItemDTO
        $scope.listTxtDocs = function () {
            let files = [];
            angular.forEach($scope.files, (value, key) => {
                if (value.type === "txt") {
                    files.push(value);
                }
            });
            return files;
        };

        // При потере фокуса или переключении документов
        $scope.saveOpenedDocLocally = function () {
            if ($scope.openedFileIndex !== -1 && $scope.files[$scope.openedFileIndex].type === "txt") {
                let sourceCode = sourceArea.innerText.replace(/\n\n/g, "\n");
                $scope.files[$scope.openedFileIndex].content = sourceCode.split("\n");
            }
        };

        // Обработка нажатия на крестик у файла в списке
        $scope.deleteFile = function (link) {
            let name = link.id.substr(4);
            $http.delete("/api/files/?path=" + name).then(() => {
                // удаляю элемент левого списка
                let deleteIndex = $scope.files.find(f => f.name === name);
                if (deleteIndex < $scope.openedFileIndex) {
                    $scope.openedFileIndex--;
                } else if (deleteIndex === $scope.openedFileIndex) {
                    sourceArea.innerHTML = "";
                    $scope.openedFileIndex = -1;
                }
                $scope.files.splice(deleteIndex, 1);
                if ($scope.files.length === 0) {
                    $scope.openedFileIndex = -1;
                }
            }, () => {
                $scope.showError();
            });
        };

        $scope.showError = function (message = "Something went wrong") {
            // TODO: поменять на что-нибудь вразумительное
            alert(message);
        }

    }]);

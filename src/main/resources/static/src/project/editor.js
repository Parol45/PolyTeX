angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        // TODO: событие с отсылкой изменений при закрытии страницы
        // TODO: копипаст вставляется как обычный текст
        let sourceArea = document.querySelector("#source-wrap");
        let resultArea = document.querySelector("#result-wrap");
        let newFileName = document.querySelector("#new-file");
        let newFolderName = document.querySelector("#new-folder");
        let fileList = document.querySelector("#file-list-wrap");
        // Тут храню инфу о полученных файлах при загрузке страницы
        $scope.files = [];
        // Файл, содержимое которого отображается в данный момент
        $scope.openedFile = null;
        $scope.openedDirs = [];
        $scope.pathToPdf = "";

        $scope.closeCurrentFile = function () {
            sourceArea.innerHTML = "";
            $scope.openedFile = null;
            sourceArea.contentEditable = false;
            sourceArea.innerText = "No file chosen";
        };

        $scope.closeCurrentFile();

        //При загрузке страницы обращение к серверу и получение списка существующих в рабочей папке файлов
        $http.get("/api/projects/" + projectId + "/files/").then(
            (response) => {
                console.log(response.data);
                $scope.organizeFiles(response.data);
            }, () => {
                $scope.showError();
            });

        // Цикл с добавлением списка файлов в список слева и отображение первого текстового
        $scope.organizeFiles = function (files) {
            angular.forEach(files, function (value) {
                let parent;
                let reg = value.path.match(/^(.*)\/[^\/]+$/);
                if (reg === null) {
                    parent = "";
                } else {
                    parent = reg[1];
                }
                let file = {
                    name: value.name,
                    path: value.path,
                    type: value.type,
                    content: value.content,
                    parent: parent
                };
                $scope.files.push(file);
            });
            $scope.files.sort((f1, f2) => f1.type.localeCompare(f2.type));
            // Добавление только файлов из корневого каталога проекта
            fileList.appendChild($scope.makeFileList($scope.files.filter(f => f.parent === "")));
            console.log($scope.files);
        };

        // Функция отправляющая внесённые изменения на сервер и запускающая компиляцию выбранного документа
        $scope.sendForCompilation = function () {
            $scope.saveOpenedDocLocally();
            if ($scope.openedFile !== null) {
                $http.post("/api/projects/" + projectId + "/compile/?targetFilepath=" + $scope.openedFile.path, $scope.files.filter(f => f.type === "txt")).then(
                    (response) => {
                        if (response.data.pathToPdf !== "") {
                            resultArea.innerHTML = `<embed class='document' src='/${response.data.pathToPdf}'/>`;
                            // TODO: добавить вторую вкладку и заменить на:
                            // $scope.pathToPdf = response.data.pathToPdf;
                            console.log(response.data.latexMessage + "\n" + response.data.biberMessage);
                        } else {
                            resultArea.innerText = response.data.latexMessage + "\n" + response.data.biberMessage;
                        }
                    },
                    () => {
                        $scope.showError();
                    });
            }
        };

        // Функция загрузки файла с допустимым содержимым на сервер (ещё проверяю есть ли с таким же именем
        // и выбран ли он вообще, потому что при нажатии отмены при втором выборе файла срабатывает onchange)
        $scope.uploadFile = function (file) {
            let reg = /.(tex|bib|png|jpg|svg)$/i;
            let targetList;
            let parent, path;
            // Если есть открытые папки, то загружаю в последнюю
            if ($scope.openedDirs.length === 0) {
                targetList = fileList.children[0];
                parent = "";
            } else {
                parent = $scope.openedDirs[$scope.openedDirs.length - 1].path;
                targetList = document.getElementById("li-" + parent).parentElement.lastChild;
            }
            path = parent + "/" + file.name;
            if (typeof file !== 'undefined' && !document.getElementById("li-" + path) && reg.test(file.name)) {
                let fd = new FormData();
                fd.append("file", file);
                fd.append("path", path);
                $http.post("/api/projects/" + projectId + "/upload/", fd, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined}
                }).then((response) => {
                        let file = {
                            name: response.data.name,
                            type: response.data.type,
                            path: path,
                            content: response.data.content,
                            parent: parent
                        };
                        $scope.files.push(file);
                        // Добавляю в список слева при успешной загрузке
                        let newFile = $scope.newFileItem(file);
                        targetList.appendChild(newFile.del);
                        targetList.appendChild(newFile.li);
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
            let selected = link.parentElement;
            // id элемента списка формируется как "li-" + путь до него
            let path = link.id.substr(3);
            let file = $scope.files.find(f => f.path === path);
            // Папки раскрывают вложенный список файлов внутри себя
            if (file.type === "dir") {
                $scope.showSublist(selected, path, file);
            } else {
                // Файлы отображают содержимое и запоминаются
                if ($scope.openedFile !== file) {
                    selected.style.backgroundColor = "#ce897e";
                    if ($scope.openedFile !== null && document.getElementById("li-" + $scope.openedFile.path) !== null) {
                        let opened = document.getElementById("li-" + $scope.openedFile.path).parentElement;
                        opened.style.backgroundColor = null;
                    }
                    $scope.saveOpenedDocLocally();
                    $scope.printFile(file);
                    $scope.openedFile = file;
                } else {
                    // Повторный клик на открытый файл закрывает его
                    selected.style.backgroundColor = null;
                    $scope.closeCurrentFile();
                }
            }
        };

        // Формирование на основе поля parent и отображения списка файлов открытой папки
        $scope.showSublist = function (selected, path, file) {
            // Проверка открыта ли уже эта папка
            if ($scope.openedDirs.indexOf(file) === -1) {
                let files = $scope.files.filter(f => f.parent === path);
                selected.style.backgroundColor = "#cecebb";
                $scope.openedDirs.push(file);
                selected.appendChild($scope.makeFileList(files));
            } else {
                // Если открыта, то закрываю
                selected.style.backgroundColor = null;
                selected.lastChild.remove();
                $scope.openedDirs.splice($scope.openedDirs.indexOf(file), 1);
                // Вложенные папки тоже исключаю из списка уже открытых
                for (let i = 0; i < $scope.openedDirs.length;) {
                    let value = $scope.openedDirs[i];
                    if (value.parent.startsWith(file.path)) {
                        $scope.openedDirs.splice($scope.openedDirs.indexOf(value), 1);
                    } else {
                        i++;
                    }
                }
            }
        };

        // Формирование немаркированного списка файлов
        $scope.makeFileList = function (files) {
            let subDir = document.createElement("ul");
            subDir.setAttribute("class", "file-list");
            angular.forEach(files, (value) => {
                let fileItem = $scope.newFileItem(value);
                subDir.appendChild(fileItem.del);
                subDir.appendChild(fileItem.li);
            });
            return subDir;
        };

        // Создание элемента списка вида из button и li
        $scope.newFileItem = function (file) {
            let del = document.createElement("button");
            let link = document.createElement("p");
            let li = document.createElement("li");
            del.setAttribute("onclick", "angular.element(this).scope().deleteFile(this)");
            del.innerText = "X";
            del.setAttribute("class", "delete-butt");
            del.setAttribute("id", "del-" + file.path);
            link.setAttribute("onclick", "angular.element(this).scope().showFile(this)");
            link.setAttribute("id", "li-" + file.path);
            link.appendChild(document.createTextNode(file.name));
            li.appendChild(link);
            // Если среди файлов в списке оказывается открытый, то помечаю его
            if (file === $scope.openedFile) {
                li.style.backgroundColor = "#ce897e";
            }
            return {del: del, li: li};
        };

        // Заполнение области для редактирования содержимым файла из переменной
        $scope.printFile = function (file) {
            sourceArea.innerHTML = "";
            // Выводится текст или тэг с изображением
            if (file.type === "txt") {
                angular.forEach(file.content, function (value) {
                    let newLine = document.createElement('div');
                    newLine.innerHTML = value === "" ? "<br>" : value;
                    sourceArea.appendChild(newLine);
                });
                sourceArea.contentEditable = true;
            } else if (file.type === "pic") {
                sourceArea.contentEditable = false;
                sourceArea.innerHTML = `<div><img src='/${file.content[0]}' alt='${file.content[0]}'></div>`;
            } else {
                console.log("Wtf? Item type is: " + file.type)
            }
        };

        // Формирую список dto текстовых файлов и отправляю на сервер
        $scope.saveDocs = function () {
            $http.put("/api/projects/" + projectId + "/files/", $scope.files.filter(f => f.type === "txt")).then(() => {
                alert("Saved!");
            }, () => {
                $scope.showError();
            });
        };

        // При потере фокуса или переключении документов
        $scope.saveOpenedDocLocally = function () {
            if ($scope.openedFile !== null && $scope.openedFile.type === "txt") {
                // innerText отображает <br> как два переноса строки. Работает - не трогай
                let sourceCode = sourceArea.innerText.replace(/\n\n/g, "\n");
                $scope.openedFile.content = sourceCode.split("\n");
            }
        };

        // Обработка нажатия на крестик у файла в списке
        $scope.deleteFile = function (link) {
            if (window.confirm("Are you sure?")) {
                let path = link.id.substr(4);
                $http.delete("/api/projects/" + projectId + "/files/?path=" + path).then(() => {
                    // Если удаляемый элемент - папка, то удаляю вложенные элементы
                    let files = $scope.files.filter(f => f.path === path || f.parent === path);
                    for (let i = 0; i < files.length;) {
                        if ($scope.files.find(f => f === files[i])) {
                            if ($scope.openedFile === files[i]) {
                                $scope.closeCurrentFile();
                            }
                            $scope.files.splice($scope.files.indexOf(files[i]), 1);
                        } else {
                            // И убираю удаляемые папки из списка открытых
                            if ($scope.openedDirs.find(f => f === files[i])) {
                                let file = $scope.openedDirs.find(f => f === files[i]);
                                $scope.openedDirs.splice($scope.openedDirs.indexOf(file), 1);
                            } else {
                                i++;
                            }
                        }
                    }
                    // Удаляю элемент левого списка
                    document.getElementById("li-" + path).parentElement.remove();
                    document.getElementById("del-" + path).remove();
                }, () => {
                    $scope.showError();
                });
            }
        };

        // Создание пустого файла или папки
        $scope.createItem = function (type) {
            let itemName, isOkay = true;
            if (type === "dir") {
                itemName = newFolderName.value;
            } else {
                itemName = newFileName.value;
                let reg = /.(tex|bib)$/i;
                isOkay = reg.test(itemName);
            }
            // Если поле ввода имени не пустое, то ищу последнюю открытую папку и создаю файл там
            if (itemName.length > 0) {
                let targetList;
                let parent;
                if ($scope.openedDirs.length === 0) {
                    parent = "";
                    targetList = fileList.children[0];
                } else {
                    parent = $scope.openedDirs[$scope.openedDirs.length - 1].path;
                    targetList = document.getElementById("li-" + parent).parentElement.lastChild;
                }
                let path = parent + "/" + itemName;
                // Проверяю существует ли уже такой файл/папка и допустимое ли расширение
                if (!$scope.files.find(f => f.path === path) && isOkay) {
                    let newItem = {
                        name: itemName,
                        type: type === "dir" ? "dir" : "txt",
                        path: path,
                        content: [],
                        parent: parent
                    };
                    $http.put("/api/projects/" + projectId + "/files", [newItem]).then(() => {
                        $scope.files.push(newItem);
                        let newFile = $scope.newFileItem(newItem);
                        targetList.appendChild(newFile.del);
                        targetList.appendChild(newFile.li);
                    }, () => {
                        $scope.showError();
                    });
                } else {
                    $scope.showError("Bad file");
                }
            }
        };

        $scope.clearAux = function() {
            // TODO: написать
        };

        $scope.showError = function (message = "Something went wrong") {
            // TODO: поменять на что-нибудь вразумительное
            alert(message);
        };

    }]);

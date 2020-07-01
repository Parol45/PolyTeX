angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        $scope.allFiles = [];
        $scope.projectId = projectId;
        $scope.firstVersion = commits;
        $scope.secondVersion = [];
        $scope.selectedFile = null;
        $scope.firstVersionFiles = [];
        $scope.secondVersionFiles = [];

        let currentFileArea = document.querySelector("#current-doc");
        let firstVersion = document.querySelector("#first-version");
        let secondVersion = document.querySelector("#second-version");
        let fileSelect = document.querySelector("#file-select");
        let fileUnselect = document.querySelector("#null-file");
        let rollbackButton = document.querySelector("#rollback-button");

        $scope.formatDate = function (date) {
            return date.replace(/(\d)(T)(\d)/g, "$1 $3");
        };

        $scope.selectFirstVersion = function (commitId) {
            rollbackButton.hidden = true;
            secondVersion.disabled = false;
            commitId = commitId.replace("string:", "");
            if (commitId !== "cur") {
                let selected = commits.find(c => c.commitId === commitId);
                $scope.firstVersionFiles = selected.files;
                $scope.secondVersion = commits.filter(c => commits.indexOf(c) > commits.indexOf(selected));
            } else {
                $scope.firstVersionFiles = curFiles;
                $scope.secondVersion = commits;
            }
            $scope.selectedFile = null;
            fileUnselect.selected = true;
            currentFileArea.innerHTML = "";
        };

        $scope.selectSecondVersion = function (commitId) {
            rollbackButton.hidden = true;
            commitId = commitId.replace("string:", "");
            let selected = commits.find(c => c.commitId === commitId);
            $scope.secondVersionFiles = selected.files;
            $scope.allFiles = $scope.firstVersionFiles.map(file => file.path)
                .concat($scope.secondVersionFiles.map(file => file.path))
                .filter((file, i, files) => files.indexOf(file) === i);
            $scope.$apply();
            $scope.selectedFile = null;
            fileUnselect.selected = true;
            currentFileArea.innerHTML = "";
        };

        $scope.firstDownload = function(filepath) {
            rollbackButton.hidden = firstVersion.value !== "cur";
            let file = $scope.firstVersionFiles.find(f => f.path === filepath);
            currentFileArea.innerHTML = "";
            if (file && !file.content) {
                if (firstVersion.value === "cur") {
                    $http.get(`/api/projects/${projectId}/get-file?filepath=${file.path}`).then((response) => {
                        file.content = response.data.join("\n");
                        $scope.secondDownload(file.content, filepath);
                    }, (response) => {
                        $scope.showError(response);
                    });
                } else {
                    $http.get(`/api/projects/${projectId}/commit-file?fileId=${file.id}`).then((response) => {
                        file.content = response.data.path;
                        $scope.secondDownload(file.content, filepath);
                    }, (response) => {
                        $scope.showError(response);
                    });
                }
            } else {
                if (file) {
                    $scope.secondDownload(file.content, filepath);
                } else {
                    $scope.secondDownload("", filepath);
                }
            }
        };

        $scope.secondDownload = function (firstText, filepath) {
            let oldFile = $scope.secondVersionFiles.find(file => file.path === filepath),
                secondText = "";
            if (oldFile && !oldFile.content) {
                $http.get(`/api/projects/${projectId}/commit-file?fileId=${oldFile.id}`).then((response) => {
                    oldFile.content = response.data.path;
                    $scope.selectedFile = oldFile;
                    firstText = difflib.stringAsLines(firstText);
                    secondText = difflib.stringAsLines(oldFile.content);
                    $scope.formHtmlDiff(secondText, firstText);
                }, (response) => {
                    $scope.showError(response);
                });
            } else {
                $scope.selectedFile = oldFile;
                firstText = difflib.stringAsLines(firstText);
                if (!oldFile) {
                    secondText = "";
                } else {
                    secondText = difflib.stringAsLines(oldFile.content);
                }
                $scope.formHtmlDiff(secondText, firstText);
            }
        };

        $scope.formHtmlDiff = function (secondText, firstText) {
            let sm = new difflib.SequenceMatcher(secondText, firstText);
            currentFileArea.appendChild(diffview.buildView({
                baseTextLines: secondText,
                newTextLines: firstText,
                opcodes: sm.get_opcodes(),
                baseTextName: firstVersion[firstVersion.selectedIndex].text,
                newTextName: secondVersion[secondVersion.selectedIndex].text,
                contextSize: null,
                viewType: 1
            }));
        };

        $scope.rollback = function () {
            if ($scope.selectedFile) {
                let rollbackTime = commits.find(c => c.commitId === secondVersion.value.replace("string:", "")).commitTime;
                $http.post(`/api/projects/${projectId}/rollback?commitDate=${rollbackTime}`, {
                    id: $scope.selectedFile.id,
                    path: $scope.selectedFile.path
                }).then(() => {
                    document.location.reload(true);
                }, (response) => {
                    $scope.showError(response);
                });
            } else {
                $http.delete(`/api/projects/${projectId}/files/?path=${fileSelect.value}`).then(() => {
                    document.location.reload(true);
                }, (response) => {
                    $scope.showError(response);
                });
            }
        };

        $scope.commit = function () {
            $http.post(`/api/projects/${projectId}/commit`).then(() => {
                document.location.reload(true);
            }, (response) => {
                $scope.showError(response);
            });
        };

        $scope.showError = function (message = "Something went wrong") {
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
angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {

        $scope.allFiles = [];
        $scope.commitFiles = [];
        $scope.selectedLink = null;
        $scope.selectedFile = null;

        let currentFileArea = document.querySelector("#current-doc");

        $scope.showFilesDiff = function (filepath) {
            currentFileArea.innerHTML = "";
            let curFile = curFiles.find(file => file.path === filepath),
                commFile = $scope.commitFiles.find(file => file.name === filepath),
                curText = "", commText = "";
            $scope.selectedFile = commFile;
            if (curFile) {
                curText = curFile.content;
            }
            if (commFile) {
                commText = difflib.stringAsLines(commFile.content);
                commText.pop();
            }

            let sm = new difflib.SequenceMatcher(commText, curText);
            currentFileArea.appendChild(diffview.buildView({
                baseTextLines: commText,
                newTextLines: curText,
                opcodes: sm.get_opcodes(),
                baseTextName: "Commit version",
                newTextName: "current file",
                contextSize: null,
                viewType: 1
            }));

        };

        $scope.loadCommitFiles = function (link) {
            if ($scope.selectedLink) {
                $scope.selectedLink.parentElement.style.background = null;
            }
            $scope.selectedLink = link;
            $scope.selectedLink.parentElement.style.background = "#999999";

            document.querySelector("#null-file").selected = true;
            $scope.selectedFile = null;
            $scope.allFiles = [];

            currentFileArea.innerHTML = "";

            $scope.commitFiles = commits.find(c => c.commitId === link.id).files;
            let ids = $scope.commitFiles.map(f => f.id);
            $http.post(`/api/projects/${projectId}/commit-files/`, ids).then((response) => {
                angular.forEach(ids, (id, i) => {
                    $scope.commitFiles[i].content = response.data[i];
                });
            }, () => {
                $scope.showError();
            });
            $scope.allFiles = curFiles.map(file => file.path)
                .concat($scope.commitFiles.map(file => file.name))
                .filter((file, i, files) => files.indexOf(file) === i);
            $scope.$apply();
        };

        $scope.rollback = function () {
            if ($scope.selectedLink) {
                if ($scope.selectedFile) {
                    $http.post(`/api/projects/${projectId}/rollback/?commitDate=${$scope.selectedLink.text}`, {
                        id: $scope.selectedFile.id,
                        name: $scope.selectedFile.name
                    }).then(() => {
                        document.location.reload(true);
                    }, () => {
                        $scope.showError();
                    });
                } else {
                    $scope.showError("No such file in selected commit");
                }
            } else {
                $scope.showError("No commit chosen");
            }
        };

        $scope.showError = function (message = "Something went wrong") {
            alert(message);
        };

    }]);
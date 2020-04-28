angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {
        $scope.commitFiles = [];
        $scope.selectedLink = null;
        $scope.selectedFile = null;

        // TODO: прикрутить библиотеку diffs

        let currentFileArea = document.querySelector("#current-doc");
        let commitFileArea = document.querySelector("#commit-doc");


        $scope.showCurrentFile = function (filepath) {
            currentFileArea.innerHTML = "";
            let text = "";
            angular.forEach(curFiles.find(f => f.path === filepath).content, line => {
                text += line + "\n";
            });
            currentFileArea.innerText = text;
            return text;
        };

        $scope.showCommitFile = function (filepath) {
            let file = $scope.commitFiles.find(f => f.name === filepath);
            commitFileArea.innerText = file.content;
            $scope.selectedFile = file;
            return file.content;
        };

        $scope.loadCommitFiles = function (link) {
            if ($scope.selectedLink !== null) {
                $scope.selectedLink.parentElement.style.background = null;
            }
            $scope.selectedLink = link;
            $scope.selectedLink.parentElement.style.background = "#999999";
            commitFileArea.innerHTML = "";
            document.querySelector("#commit-null").selected = true;
            $scope.selectedFile = null;

            $scope.commitFiles = commits.find(c => c.commitId === link.id).files;
            $scope.$apply();

            let ids = $scope.commitFiles.map(f => f.id);
            $http.post("/api/projects/" + projectId + "/commit-files/", ids).then((response) => {
                angular.forEach(ids, (id, i) => {
                    let file = $scope.commitFiles[i];
                    file.content = response.data[i];
                });
            }, () => {
                $scope.showError();
            });
        };

        $scope.rollback = function() {
            if ($scope.selectedFile !== null) {
                $http.post("/api/projects/" + projectId + "/rollback/", {id: $scope.selectedFile.id, name: $scope.selectedFile.name}).then((response) => {
                    console.log(response);
                    document.location.reload(true);
                }, () => {
                    $scope.showError();
                });
            } else {
                $scope.showError("No file or commit chosen");
            }
        };

        $scope.showError = function (message = "Something went wrong") {
            alert(message);
        };

    }]);
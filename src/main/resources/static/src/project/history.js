angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {
        $scope.commitFiles = [];
        $scope.selectedLink = null;

        let currentFileArea = document.querySelector("#current-doc");
        let commitFileArea = document.querySelector("#commit-doc");


        $scope.showCurrentFile = function (filepath) {
            currentFileArea.innerHTML = "";
            angular.forEach(curFiles.find(f => f.path === filepath).content, line => {
                let newLine = document.createElement('div');
                newLine.innerHTML = line === "" ? "<br>" : line;
                currentFileArea.appendChild(newLine);
            });
        };

        $scope.showCommitFile = function (filepath) {
            let file = $scope.commitFiles.find(f => f.name = filepath);
            commitFileArea.innerText = file.content;

        };

        $scope.loadCommitFiles = function (link) {
            if ($scope.selectedLink !== null) {
                $scope.selectedLink.parentElement.style.background = null;
            }
            $scope.selectedLink = link;
            $scope.selectedLink.parentElement.style.background = "#999999";
            commitFileArea.innerHTML = "";
            document.querySelector("#commit-null").selected = true;

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


        $scope.showError = function (message = "Something went wrong") {
            // TODO: поменять на что-нибудь вразумительное
            alert(message);
        };

    }]);
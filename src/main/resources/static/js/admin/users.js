angular
    .module("app", [])
    .controller("ctrl", ['$scope', '$http', function ($scope, $http) {
        $scope.users = users;
        let email = document.querySelector("#email-field"),
            password = document.querySelector("#password-field");

        $scope.grantAdmin = function (id) {
            $http.get(`/admin/api/grant-admin?id=${id}`).then(() => {
                $scope.users.find(u => u.id === id).role = "ROLE_ADMIN";
            }, (response) => {
                $scope.showError(response);
            });
        };

        $scope.revokeAdmin = function (id) {
            $http.get(`/admin/api/revoke-admin?id=${id}`).then(() => {
                $scope.users.find(u => u.id === id).role = "ROLE_USER";
            }, (response) => {
                $scope.showError(response);
            });
        };

        $scope.block = function (id) {
            $http.get(`/admin/api/block?id=${id}`).then(() => {
                $scope.users.find(u => u.id === id).banned = true;
            }, (response) => {
                $scope.showError(response);
            });
        };

        $scope.unblock = function (id) {
            $http.get(`/admin/api/unblock?id=${id}`).then(() => {
                $scope.users.find(u => u.id === id).banned = false;
            }, (response) => {
                $scope.showError(response);
            });
        };

        $scope.register = function () {
            if (email.value.length > 0 && password.value.length > 0)
                $http.post(`/admin/api/register?email=${email.value}&password=${password.value}`).then(
                    (response) => {
                        $scope.users.push(response.data);
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
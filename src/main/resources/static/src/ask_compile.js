var app = angular.module("app", []);

app.controller("ctrl", ['$scope', '$http', function($scope, $http) {
    $scope.sendForCompilation = function () {
        $http.post("/api/compile/", formatText(document.querySelector(".source-wrap").innerHTML)).then(
            (response) => {returnDoc(response)},
            (response) => {document.querySelector(".result-wrap").innerHTML = "Error in data transfer: " + response});
    };

    function formatText(text){
        ftext = text;
        ftext = ftext.replace(/(^\s+$)/gm, "");
        ftext = ftext.replace(/(<div><br><\/div>)|(<\/div>)/g, "\n");
        ftext = ftext.replace(/<div>/gm, "");
        return ftext;
    }

    function returnDoc(mess){
        if (mess.data.message === "ok")
            document.querySelector(".result-wrap").innerHTML = "<embed class='document' src=\"test/test.pdf\"/>";
        else
            document.querySelector(".result-wrap").innerHTML = mess.data.message;
    }
}]);

/* Для сравнения первой функции с функцией в JQuery
$(document).ready(function () {
  $('.compile-butt').click(function () {
    $.ajax({
      type:'POST',
      contentType: "application/json",
      url: window.location + "api/compile",
      data: FormatText($('.source-wrap').text()),
      success: function(result) {
        $(".result-wrap").html("<embed class='document' src=\"test/test.pdf\" />");
      },
      error: function(e) {
        alert("Error!")
    }
    });
  });
});

*/
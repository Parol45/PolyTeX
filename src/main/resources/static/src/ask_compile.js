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
function FormatText(text){
  var re = /^\s+/gm;
  text = text.replace("<br>", "\n");
  text = text.replace(re, "");
  return text;
}
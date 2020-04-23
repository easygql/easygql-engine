var loctarget=window.location;
function authorizedget(url,datasent,datatype,contenttype,okfun,errorfun,currenturl){
    if(usertoken=="") {
        window.location.href="/?orginurl="+UrlEncode(currenturl);
    } else {
       // var usertoken=localStorage.getItem("usertoken");
        var usertoken="test";
        // var moduleurl="http://39.106.186.45:8711/zuksynctest";
        var modulerurl=loctarget.protocol+"//"+loctarget.host;
        url=modulerurl+url;
        $.ajax(
            {
                type: "GET",
                url: url,
                headers:{
                    "Authorization":usertoken,
                    "User-id":"5c66afa4f074e13a1a124970"
                },
                dataType: datatype,
                contentType:contenttype,
                success: okfun ,
                error:errorfun
            }
        );
    }

};
function authorizedpost(url,datasent,datatype,contenttype,okfun,errorfun,currenturl){
   // var usertoken=localStorage.getItem("usertoken");
    var usertoken="test";
    if(usertoken=="") {
        window.location.href="/?orginurl="+encodeURI(encodeURI(currenturl));
    } else {
        // var moduleurl="http://39.106.186.45:8711/zuksynctest";
        var modulerurl=loctarget.protocol+"//"+loctarget.host;
        url=modulerurl+url;
        $.ajax(
            {
                type: "POST",
                url: url,
                headers:{"Authorization":usertoken,
                    "User-id":"5c66afa4f074e13a1a124970"},
                dataType: datatype,
                data:datasent,
                contentType:contenttype,
                success: okfun ,
                error:errorfun
            }
        );
    }
};
function login(){
    var username = $("#inputusername").val();
    var userpasswd=$("#inputpassword").val();
    var orginurl=$("#orginurl").val();
    if (username.trim() == "" || userpasswd.trim()=="") {
        alert('please enter you username or password.');
        return false;
    }  else {
       if(username=="admin"&&userpasswd=="admin"){
           window.location.href=orginurl;
       }
    }
};


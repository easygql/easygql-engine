var ws;
var roomid;
var username;

function connect() {
    username = document.getElementById("username").value;
    roomid=document.getElementById("roomid").value;
    ws = new WebSocket("ws://localhost:9978/graphqlws/chatroom");

    ws.onmessage = function(event) {
        var log = document.getElementById("log");
        console.log(event.data);
        var message = JSON.parse(event.data);
        log.innerHTML += message.from + " : " + message.content + "\n";
    };

    ws.onopen=function (ev) {
        var substr="subscription{sub_chatmessage(where:{col:{roomid:{match:\"roomtest\"}}}){mutationtype,new_val{content}}}";
        var json = JSON.stringify({"query":substr});
        ws.send(json);
    }
}

function send() {
    var content = document.getElementById("msg").value;
    var contentstr="mutation{insert_chatmessage(objects:[{roomid:\""+roomid+"\",from:\""+username+"\",content:\""+content+"\"}]){affected_rows}}";
    var json = JSON.stringify({"query":contentstr})
    ws.send(json);
}
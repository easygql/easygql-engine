var timeinterverinfo = 2000;
var isdraw = false;
var identity = "";
var zuksynclient=null;

function joinroom() {	
	var roomidtmp=$('#roomid').val();
	passwd=$('#secret').val();
	passwd=md5(passwd);
	zuksynclient=new ZukSync();
	zuksynclient.joinroom(roomidtmp,passwd);
}
function exitroom() {
//	if(ws!=null){
//		
//		
//	}
}
function autosendmessage() {
//		var strcontent=getnewString();
//		var idinfo=guid();
//		var createdtimeinfo=new Date().getTime();
//		stompClient.send("/app/sync/"+roomnoinfo, {}, JSON.stringify({id:idinfo,content:strcontent,createdtime:createdtimeinfo}));
//		setTimeout(autosendmessage, timeinterverinfo);
}

//function getnewString() {
//	var char1 = 'rethink';
//	var pwd = char1;
//	var len = 2 + Math.floor(Math.random() * 2);
//	for (i = 0; i < len; i++) {
//		pwd = pwd + pwd;
//	}
//	return pwd;
//}
function onNewMessage(result) {
    var message = JSON.parse(result.body);
    for(var i = 0 ; i<message.length; i ++){
    	var datacontent=message[i];
    	var timenow = new Date().getTime();
    	var passedinfo=timenow-datacontent.createdtime;
    	var lengthinfotmp=datacontent.content.length;
    	$('#pocinfo').append('<tr><td>receive</td><td>' + datacontent.id + '</td><td>' + passedinfo+ '</td><td>' + lengthinfotmp + '</td></tr>');
    }
    
}



function guid() {
	function S4() {
		return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
	}
	return (S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4());
}


function addsub(){
	var subpathinfo=$('#subpath').val().trim();
	if(!subpathinfo.startsWith("/")){
		subpathinfo="/"+subpathinfo;
	}	
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo!="") {
		conditioninfo=JSON.parse(conditioninfo);
	}	
	var fieldsinfo=$('#fields').val().trim();
	if(fieldsinfo!=""){
		fieldsinfo=JSON.parse(fieldsinfo);
	}
	zuksynclient.addsub(subpathinfo,conditioninfo,fieldsinfo,receivenewmessage);
}
function receivenewmessage(result){
	var message = JSON.parse(result.body);
	$('#pocinfo').append('<tr><td>'+message.type+'</td><td>' +result.body + '</td><td>' + '</td></tr>');
}
function insertval(){
	var valinfo=$('#new_val').val().trim();
	if(valinfo==""){
		return;
	}
	valinfo=JSON.parse(valinfo);
	zuksynclient.insertobj(valinfo);
}
function queryrest(){
	var messagetmp = {};
	messagetmp["type"]="queryrest";
	var msgcontent={};
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo!="") {
		conditioninfo=JSON.parse(conditioninfo);
	}	
	var fieldsinfo=$('#fields').val().trim();
	if(fieldsinfo!=""){
		fieldsinfo=JSON.parse(fieldsinfo);
	}
	var data1=zuksynclient.query_rest(conditioninfo,fieldsinfo);
	if(data1!=undefined||data1!=""){
		$('#pocinfo').append('<tr><td>query result</td><td>' +JSON.stringify(data1) + '</td><td></td></tr>');
	}
}
function insertrest(){
	var valinfo=$('#new_val').val().trim();
	if(valinfo==""){
		return;
	}
	valinfo=JSON.parse(valinfo);
	var data1=zuksynclient.insertobj_rest(valinfo);	
	if(data1!=undefined||data1!=""){
		$('#pocinfo').append('<tr><td>insert result</td><td>' +JSON.stringify(data1) + '</td><td></td></tr>');
	}
}
function updaterest(){
	var valinfo=$('#new_val').val().trim();
	if(valinfo==""){
		return;
	}
	valinfo = JSON.parse(valinfo);
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo==""){
		return;
	}
	conditioninfo=JSON.parse(conditioninfo);
	var data1=zuksynclient.updateobj_rest(valinfo,conditioninfo);	
	if(data1!=undefined||data1!=""){
		$('#pocinfo').append('<tr><td>update result</td><td>' +JSON.stringify(data1) + '</td><td></td></tr>');
	}
}
function deleterest(){
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo==""){
		return;
	}
	conditioninfo=JSON.parse(conditioninfo);
	var data1=zuksynclient.delobj_rest(conditioninfo);	
	if(data1!=undefined||data1!=""){
		$('#pocinfo').append('<tr><td>delete result</td><td>' +JSON.stringify(data1) + '</td><td></td></tr>');
	}
}
function updateval(){
	var valinfo=$('#new_val').val().trim();
	if(valinfo==""){
		return;
	}
	valinfo = JSON.parse(valinfo);
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo==""){
		return;
	}
	conditioninfo=JSON.parse(conditioninfo);
	zuksynclient.updateobj(valinfo,conditioninfo);
}
function deleteval(){	
	var conditioninfo=$('#condition').val().trim();
	if(conditioninfo==""){
		return;
	}
	conditioninfo=JSON.parse(conditioninfo);
	zuksynclient.delobj(conditioninfo);
}
function deletesub(){	
	var subpathinfo=$('#subpath').val().trim();
	zuksynclient.delsub(subpathinfo);
}


function newroom() {
	passwd = $('#secret').val();
	if(passwd==""){
		alert("创建房间前，输入预设密码!");
		return;
	}
	passwd=md5(passwd);
	var roomidtmp=guid();
	$('#roomid').val(roomidtmp);
	
	zuksynclient=new ZukSync(); 
	zuksynclient.createroom(roomidtmp,passwd);
}


var app=angular.module("chatRoom",[]);
var lastMsgId = null;
var threadIds = {};
/*app.factory('socket', function($rootScope) {
    var socket = io(); //默认连接部署网站的服务器
    return {
        on: function(eventName, callback) {
            socket.on(eventName, function() {
                var args = arguments;
                $rootScope.$apply(function() {   //手动执行脏检查
                    callback.apply(socket, args);
                });
            });
        },
        emit: function(eventName, data, callback) {
            socket.emit(eventName, data, function() {
                var args = arguments;
                $rootScope.$apply(function() {
                    if(callback) {
                        callback.apply(socket, args);
                    }
                });
            });
        }
    };
});*/

app.service('messageService', function ($rootScope, $http) {
    var self = this;
    var nickname;
    this.startConnect = function (userName, msgRecCallback) {
        nickname = userName;
        $http({
            url: "/ms/connect?userName=" + userName,           //请求的url路径
            method: 'GET'
        }).success(function (response, status, header, config, statusText) {
            console.log(response);
            if (response.indexOf('timing') > -1) {
                console.log("connection timing out");
            } else {
                //处理消息
                console.log("notified!");
                self.onReceiveMessage(msgRecCallback);
            }
            self.startConnect(userName,msgRecCallback);

        }).error(function (data, header, config, status) {
            //错误处理
            console.log(data);
            //self.startConnect(userName);
        });
    }

    this.onReceiveMessage = function (callback) {
        console.log("nickname:" + nickname);
        var url = "/ms/msg?userName=" + nickname;
        if (lastMsgId != null) {
            url = url + "&uuid=" + lastMsgId;
        }
        $http({
            url: url,           //请求的url路径
            method: 'GET'
        }).success(function (data, status, header, config, statusText) {
            console.log(data);
            if (data.length > 0) {
                lastMsgId = data[0].time;
                if(callback) {
                    callback(data);
                }
            }
        }).error(function (data, header, config, status) {
            console.log(data);
        });
    }
    
    this.sendMsg = function (msg) {
        console.log("will send message");
        $http({
            url: "/ms/thread/msg",           //请求的url路径
            method: 'POST',
            data: msg
        }).success(function (data, status, header, config, statusText) {
            console.log(data);
        }).error(function (data, header, config, status) {
            console.log(data);
        });
    }
});

app.factory('randomColor', function($rootScope) {
    return {
        newColor: function() {
            return '#'+('00000'+(Math.random()*0x1000000<<0).toString(16)).slice(-6);
        }
    };
});

app.factory('userService', function($rootScope) {
    return {
        get: function(users,nickname) {
            if(users instanceof Array){
                for(var i=0;i<users.length;i++){
                    if(users[i].nickname===nickname){
                        return users[i];
                    }
                }
            }else{
                return null;
            }
        },

        getOnlineUsers: function (callback) {

        }
    };
});

app.factory('threadService', function($rootScope) {
    return {
        get: function(threads,threadId) {
            if(threads instanceof Array){
                for(var i=0;i<threads.length;i++){
                    if(threads[i].threadId===threadId){
                        return threads[i];
                    }
                }
            }else{
                return null;
            }
        },

        getOnlineUsers: function (callback) {

        }
    };
});

app.controller("chatCtrl",['$scope','$http', '$timeout',
    '$interval', 'randomColor','userService', 'messageService', 'threadService',
    function($scope,$http,$timeout,$interval,randomColor,userService, messageService, threadService){
    var messageWrapper= $('.message-wrapper');
    $scope.hasLogined=false;
    $scope.receiver="";//默认是群聊
    $scope.publicMessages=[];//群聊消息
    $scope.privateMessages={};//私信消息
    $scope.messages=$scope.privateMessages;
    $scope.lastMsgId = null;
    //$scope.messages=$scope.publicMessages;//默认显示群聊
    $scope.users=[];
    $scope.userNames = {};
    //获取threads
    $scope.threads =[];
    $scope.color=randomColor.newColor();//当前用户头像颜色
    $scope.login=function(){   //登录进入聊天室
        //socket.emit("addUser",{nickname:$scope.nickname,color:$scope.color});
        $scope.userExisted=false;
        $scope.hasLogined=true;

        messageService.startConnect($scope.nickname, function (data) {
            //收到消息，对消息处理函数, 最新的消息在列表的最前面

            for (var i = data.length - 1; i >= 0; i--) {
                //this.push(key + ': ' + value);
                var value = data[i];
                console.log(value);
                if (threadIds[value.threadId] == undefined) {
                    //需要根据threadId查询thread信息
                    $http({
                        url:"/ms/thread/"+value.threadId,           //请求的url路径
                        method:'GET'
                    }).success(function(data, status, header, config, statusText){
                        console.log(data);
                        if (data != null) {
                            $scope.threads.push(data);
                        }
                    }).error(function(data,header,config,status){
                        console.error(data);
                    });
                }

                value.type = "normal";
                if(!$scope.privateMessages[value.threadId]){
                    $scope.privateMessages[value.threadId]=[];

                }
                $scope.privateMessages[value.threadId].push(value);

                var msgThread = threadService.get($scope.threads, value.threadId);
                msgThread.hasNewMessage = true;
                //var fromUser=userService.get($scope.users,value.from);
                //var toUser=userService.get($scope.users,value.to);
                //fromUser.hasNewMessage = true;//私信
                //threadIds[value.threadId].hasNewMessage = true;
            }

           // $scope.messages=$scope.privateMessages;
        });

        //获取在线用户信息
        $interval(function () {
            //get online users;
            $http({
                url:"/ms/online-users",           //请求的url路径
                method:'GET'
            }).success(function(response, status, header, config, statusText){
                console.log(response);
                if (response != null) {
                    $scope.users = [];
                    for(var i=0;i<response.length;i++){
                        if ($scope.nickname == response[i]) continue;
                        var u = {};
                        u.nickname = response[i];
                        console.log($scope.userNames);
                        $scope.users.push(u);
                        //if ($scope.userNames[u.nickname] == null) {
                          //  $scope.users.push(u);
                        //}
                        $scope.userNames[u.nickname] = u.nickname;
                    }
                }

            }).error(function(data,header,config,status){
                //错误处理
                console.error(data);
            });
        },5000);

        //获取thread信息
        $timeout(function () {
            //get online users;
            $http({
                url:"/ms/thread?userName="+ $scope.nickname,           //请求的url路径
                method:'GET'
            }).success(function(data, status, header, config, statusText){
                console.log(data);
                if (data != null) {
                    $scope.threads = [];
                    for(var i=0;i<data.length;i++){
                        $scope.threads.push(data[i]);
                        threadIds[data[i].threadId] = data[i].threadId;
                    }
                }

            }).error(function(data,header,config,status){
                //错误处理
                console.error(data);
            });
        },500);


    }//end login

    $scope.scrollToBottom=function(){
        messageWrapper.scrollTop(messageWrapper[0].scrollHeight);
    }

    $scope.postMessage=function(){
        var msg={msg:$scope.words,type:"normal",color:$scope.color,from:$scope.nickname,to:$scope.receiver, threadId:$scope.receiver};
        var rec=$scope.receiver;
        if(rec){  //私信
           if(!$scope.privateMessages[rec]){
               $scope.privateMessages[rec]=[];
           }
            $scope.privateMessages[rec].push(msg);
        }else{ //群聊
            $scope.publicMessages.push(msg);
        }
        $scope.words="";
        if(rec!==$scope.nickname) { //排除给自己发的情况
            //socket.emit("addMessage", msg);
            messageService.sendMsg(msg);
        }
    }
    $scope.setReceiver=function(threadId){
        $scope.receiver=threadId;
        if(threadId){ //私信用户
            if(!$scope.privateMessages[threadId]){
                $scope.privateMessages[threadId]=[];
            }
            $scope.messages=$scope.privateMessages[threadId];
        }else{//广播
            $scope.messages=$scope.publicMessages;
        }
        var thrd=threadService.get($scope.threads,threadId);
        if(thrd){
            thrd.hasNewMessage=false;
        }
    }

    /*
    //收到登录结果
    socket.on('userAddingResult',function(data){
        if(data.result){
            $scope.userExisted=false;
            $scope.hasLogined=true;
        }else{//昵称被占用
            $scope.userExisted=true;
        }
    });

    //接收到欢迎新用户消息
    socket.on('userAdded', function(data) {
        if(!$scope.hasLogined) return;
        $scope.publicMessages.push({text:data.nickname,type:"welcome"});
        $scope.users.push(data);
    });

    //接收到在线用户消息
    socket.on('allUser', function(data) {
        if(!$scope.hasLogined) return;
        $scope.users=data;
    });

    //接收到用户退出消息
    socket.on('userRemoved', function(data) {
        if(!$scope.hasLogined) return;
        $scope.publicMessages.push({text:data.nickname,type:"bye"});
        for(var i=0;i<$scope.users.length;i++){
            if($scope.users[i].nickname==data.nickname){
                $scope.users.splice(i,1);
                return;
            }
        }
    });

    //接收到新消息
    socket.on('messageAdded', function(data) {
        if(!$scope.hasLogined) return;
        if(data.to){ //私信
            if(!$scope.privateMessages[data.from]){
                $scope.privateMessages[data.from]=[];
            }
            $scope.privateMessages[data.from].push(data);
        }else{//群发
            $scope.publicMessages.push(data);
        }
        var fromUser=userService.get($scope.users,data.from);
        var toUser=userService.get($scope.users,data.to);
        if($scope.receiver!==data.to) {//与来信方不是正在聊天当中才提示新消息
            if (fromUser && toUser.nickname) {
                fromUser.hasNewMessage = true;//私信
            } else {
                toUser.hasNewMessage = true;//群发
            }
        }
    });

*/

}]);

app.directive('message', ['$timeout',function($timeout) {
    return {
        restrict: 'E',
        templateUrl: 'message.html',
        scope:{
            info:"=",
            self:"=",
            scrolltothis:"&"
        },
        link:function(scope, elem, attrs){
                scope.time=new Date();
                $timeout(scope.scrolltothis);
                $timeout(function(){
                    elem.find('.avatar').css('background',scope.info.color);
                });
        }
    };
}])
    .directive('user', ['$timeout',function($timeout) {
        return {
            restrict: 'E',
            templateUrl: 'user.html',
            scope:{
                info:"=",
                iscurrentreceiver:"=",
                setreceiver:"&"
            },
            link:function(scope, elem, attrs,chatCtrl){
                $timeout(function(){
                    elem.find('.avatar').css('background',scope.info.color);
                });
            }
        };
    }])

    .directive('thread', ['$timeout',function($timeout) {
    return {
        restrict: 'E',
        templateUrl: 'thread.html',
        scope:{
            info:"=",
            iscurrentreceiver:"=",
            setthread:"&"
        },
        link:function(scope, elem, attrs,chatCtrl){
            $timeout(function(){
                elem.find('.avatar').css('background',scope.info.color);
            });
        }
    };
}]);

$(document).ready(function () {
    console.log("hello world jquery");
    var currentLat = driverLoc.lat;
    var currentLng = driverLoc.lng;
    var currentTripId;
    // 百度地图API功能
    var map = new BMap.Map("allmap");    // 创建Map实例
    map.centerAndZoom(new BMap.Point( currentLng,currentLat), 12);  // 初始化地图,设置中心点坐标和地图级别
    //添加地图类型控件
    map.addControl(new BMap.MapTypeControl({
        mapTypes: [
            BMAP_NORMAL_MAP,
            BMAP_HYBRID_MAP
        ]
    }));
    map.setCurrentCity("北京");          // 设置地图显示的城市 此项是必须设置的
    map.enableScrollWheelZoom(true);     //开启鼠标滚轮缩放
    var point = new BMap.Point(currentLng,currentLat);
    var marker = new BMap.Marker(point);// 创建标注
    map.addOverlay(marker);
    marker.enableDragging();
    marker.addEventListener("dragend", function(e){
        console.log("当前位置：" + e.point.lng + ", " + e.point.lat);
        currentLat = e.point.lat;
        currentLng = e.point.lng;
        $('#latlnglable').html(currentLat + ',' + currentLng);
    })
    $('#latlnglable').html(currentLat + ',' + currentLng);


    /**
     * 获取用户的离线消息
     */
    var lastMsgUuid;
    var lastMsg;
    var tripRequest;
    var ridermarker;
    fetchMessages();
    connectMessageChannel();

    function connectMessageChannel() {
        $.ajax({
            url: '/ms/connect?userName='+userName,
            type: 'get',
            dataType: 'json',
            contentType: 'application/json',
            success: function (data) {
                console.log(data);
                fetchMessages();
                connectMessageChannel();
            },
            error: function (data) {
                console.log(data);
                connectMessageChannel();
            }
        });
    }

    function fetchMessages(uuid) {
        var url = '/ms/msg?userName='+ userName;
        if(lastMsgUuid != null) {
            url = url + "&uuid=" + lastMsgUuid;
        }
        $.ajax({
            url: url,
            type: 'get',
            dataType: 'json',
            contentType: 'application/json',
            success: function (data) {
                //console.log(data);
                if (data.length > 0) {
                   lastMsgUuid = data[0].time;
                   lastMsg = data[0].msg;
                   tripRequest = $.parseJSON(lastMsg);
                }
                $('#lastMsg').html(lastMsg);

                if (tripRequest != undefined && tripRequest.tripId != null && tripRequest.riderUserName != null && tripRequest.driverUserName == null) {

                    $.ajax({
                        url:'/lbs/trip?uuid=' + tripRequest.tripId,
                        type:'get',
                        dataType: 'json',
                        contentType: 'application/json',
                        success: function (tripInfo) {
                            if(tripInfo != null && tripInfo.driverUserName == null) {
                                var riderpoint = new BMap.Point(tripRequest.lng, tripRequest.lat);
                                ridermarker = new BMap.Marker(riderpoint);// 创建标注
                                map.addOverlay(ridermarker);
                                ridermarker.setAnimation(BMAP_ANIMATION_BOUNCE);
                                var label = new BMap.Label(tripInfo.riderUserName + "：接我!",{offset:new BMap.Size(20,-10)});
                                ridermarker.setLabel(label);
                                $('#myModal').modal('show');
                            }
                        },
                        error: function (err) {

                        }
                    });



                }
                console.log("last UUID is:" + lastMsgUuid);
                /*$.each(data, function(key,value){
                    console.log(value.time);
                    lastMsgUuid = value.time;
                });*/
            },
            error: function (data) {
                connectMessageChannel();
            }
        });
    }

    function grapTripRequest(){
        $.ajax({
            url: '/lbs/trip',
            type: 'put',
            dataType: 'json',
            contentType: 'application/json',
            success: function (data) {
                console.log(data);
                $('#myModal').modal('hide');
            },
            error: function (data) {
                //connectMessageChannel();
                alert("The trip is accpeted by the other driver!");
                $('#myModal').modal('hide');
                map.removeOverlay(ridermarker);
            },
            data: JSON.stringify(tripRequest)
        });
    }

    $('#acceptRide').on('click', function () {
        //var $btn = $(this).button('loading')
        // business logic...console.log("You pressed OK!");
        tripRequest.driverUserName = userName;
        grapTripRequest();
        //$btn.button('Accept it')
    })

    $('#myModal').on('hidden.bs.modal', function () {
        map.removeOverlay(ridermarker);
    });
});





function fire_ajax_submit() {
    var search = {}
    search["username"] = $("#username").val();

    $("#btn-search").prop("disabled", true);

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/api/search",
        data: JSON.stringify(search),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {
        },
        error: function (e) {
        }
    });

}
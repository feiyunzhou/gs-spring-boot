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
                    var riderpoint = new BMap.Point(tripRequest.lng, tripRequest.lat);
                    var ridermarker = new BMap.Marker(riderpoint);// 创建标注
                    map.addOverlay(ridermarker);
                    /*var r=confirm("Take the trip：" + tripRequest.riderUserName + "?");//window.confirm("Press a button");
                    if (r==true)
                    {
                        console.log("You pressed OK!");
                        tripRequest.driverUserName = userName;
                        grapTripRequest();
                    } else
                    {
                        console.log("You pressed Cancel!");
                    }*/

                    $('#myModal').modal('show');

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
            },
            error: function (data) {
                //connectMessageChannel();
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
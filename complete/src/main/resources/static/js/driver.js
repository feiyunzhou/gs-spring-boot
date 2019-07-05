$(document).ready(function () {
    console.log("hello world jquery");
    var map = new BMap.Map("allmap");    // 创建Map实例
    var point = new BMap.Point(116.404, 39.915);
    map.centerAndZoom(point, 15);
    var start;
    var end;
    getRandomLatLng();
    var currentLat = start.lat;
    var currentLng = start.lng;
    var currentTripId;
    // 百度地图API功能
    //map.centerAndZoom(new BMap.Point( currentLng,currentLat), 12);  // 初始化地图,设置中心点坐标和地图级别
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

    var duration;
    var distance;

    function getRandomLatLng() {
        var bounds = map.getBounds();
        var sw = bounds.getSouthWest();
        var ne = bounds.getNorthEast();
        var lngSpan = Math.abs(sw.lng - ne.lng);
        var latSpan = Math.abs(ne.lat - sw.lat);
        start = new BMap.Point(sw.lng + lngSpan * (Math.random() * 0.7), ne.lat - latSpan * (Math.random() * 0.7));
        end =  new BMap.Point(sw.lng + lngSpan * (Math.random() * 0.7), ne.lat - latSpan * (Math.random() * 0.7));
    }
    routeLine();
    var arrPois =[];
    var i = 0;
    function routeLine(){
        var lushu;
        // 实例化一个驾车导航用来生成路线
        var drv = new BMap.DrivingRoute('北京', {
            onSearchComplete: function(res) {
                if (drv.getStatus() == BMAP_STATUS_SUCCESS) {
                    var plan = res.getPlan(0);
                    for(var j=0;j<plan.getNumRoutes();j++){
                        var route = plan.getRoute(j);
                        console.log(plan.getDistance(false));
                        console.log(plan.getDuration(false));
                        distance = plan.getDistance(false);
                        duration = plan.getDuration(false)
                        arrPois= arrPois.concat(route.getPath());
                    }
                    map.addOverlay(new BMap.Polyline(arrPois, {strokeColor: '#111'}));
                    map.setViewport(arrPois);
                    console.log(arrPois.length);

                    lushu = new BMapLib.LuShu(map,arrPois,{
                        defaultContent:"",//"从天安门到百度大厦"
                        autoView:true,//是否开启自动视野调整，如果开启那么路书在运动过程中会根据视野自动调整
                        icon  : new BMap.Icon('http://lbsyun.baidu.com/jsdemo/img/car.png', new BMap.Size(52,26),{anchor : new BMap.Size(27, 13)}),
                        speed: distance/duration,
                        enableRotation:true,//是否设置marker随着道路的走向进行旋转
                        landmarkPois: [
                            {lng:116.314782,lat:39.913508,html:'加油站',pauseTime:2},
                            {lng:116.315391,lat:39.964429,html:'高速公路收费<div><img src="http://map.baidu.com/img/logo-map.gif"/></div>',pauseTime:3},
                            {lng:116.381476,lat:39.974073,html:'肯德基早餐<div><img src="http://ishouji.baidu.com/resource/images/map/show_pic04.gif"/></div>',pauseTime:2}
                        ]});
                    lushu.start();
                    setInterval(reportLoc, 1000 * (duration/arrPois.length));
                }
            }
        });

        drv.search(start, end);
    }

    /**
     *  sending the position to the server
     */
    var status = true;
    //reportLoc();

    function reportLoc() {
        if (i >= arrPois.length) return;
        var point = {
            userName: userName,
            lat: arrPois[i].lat,
            lng: arrPois[i].lng
        }
        i++;
        $.ajax({
            url: '/lbs/driver/location',
            type: 'post',
            async:true,
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify(point),
            success: function (returnValue) {
                if (status) {
                    status = false;
                   // window.setInterval("reportLoc()", 15000);
                }
            }
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
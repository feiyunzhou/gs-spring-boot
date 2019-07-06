$(document).ready(function () {
    console.log("hello world jquery");

    var currentLat = 39.941033;
    var currentLng = 116.425099;
    var currentTripId;
    // 百度地图API功能
    var map = new BMap.Map("allmap");    // 创建Map实例
    map.centerAndZoom(new BMap.Point(currentLng, currentLat), 18);  // 初始化地图,设置中心点坐标和地图级别
    //添加地图类型控件
    map.addControl(new BMap.MapTypeControl({
        mapTypes: [
            BMAP_NORMAL_MAP,
            BMAP_HYBRID_MAP
        ]
    }));
    map.setCurrentCity("北京");          // 设置地图显示的城市 此项是必须设置的
    map.enableScrollWheelZoom(true);     //开启鼠标滚轮缩放
    var point = new BMap.Point(currentLng, currentLat);
    var marker = new BMap.Marker(point);// 创建标注
    map.addOverlay(marker);
    marker.enableDragging();
    marker.addEventListener("dragend", function (e) {
        console.log("当前位置：" + e.point.lng + ", " + e.point.lat);
        currentLat = e.point.lat;
        currentLng = e.point.lng;
        $('#latlnglable').html(currentLat + ',' + currentLng);
    })
    var driverMarker;

    $('#latlnglable').html(currentLat + ',' + currentLng);

    $("#btnRideRequest").click(function () {
        var trip = {
            riderUserName: "rider1",
            lat: currentLat,
            lng: currentLng
        }
        $.ajax({
            url: '/lbs/trip',
            type: 'post',
            dataType: 'json',
            contentType: 'application/json',
            success: function (data) {
                console.log(data);
                //setInterval(fetchdata(data.tripId),5000);
                currentTripId = data.tripId;
                fetchTripInfo();
            },
            data: JSON.stringify(trip)
        });
    });
    var driverInfo = {
        userName:"",
        lat: 39.000000,
        lng: 116.420000
    };
    function fetchTripInfo() {
        $.ajax({
            url: '/lbs/trip?uuid=' + currentTripId,
            type: 'get',
            success: function (response) {
                // Perform operation on the return value
                console.log(response);
            },
            complete: function (data) {
                if (data.responseJSON != null) {
                    if (data.responseJSON.driverUserName == null) {
                        setTimeout(fetchTripInfo, 5000);
                    } else {
                        //获取司机的位置
                        driverInfo.userName = data.responseJSON.driverUserName;
                        fetchDriverInfo();
                    }
                }
            }
        });
    }

    function fetchDriverInfo() {
        $.ajax({
            url: '/lbs/driver/location?userName=' + driverInfo.userName,
            type: 'get',
            success: function (response) {
                // Perform operation on the return value
                console.log(response);
                if (response != null) {
                    driverInfo.lat = response.lat;
                    driverInfo.lng = response.lng;
                    setTimeout(fetchDriverInfo, 10500);
                    if (driverMarker != undefined) {
                        map.removeOverlay(driverMarker);
                    }
                    driverMarker = new BMap.Marker(new BMap.Point(driverInfo.lng, driverInfo.lat));// 创建标注
                    driverMarker.setIcon(new BMap.Icon('http://lbsyun.baidu.com/jsdemo/img/car.png', new BMap.Size(52,26),{anchor : new BMap.Size(27, 13)}));
                    map.addOverlay(driverMarker);
                }
            },
            complete: function (data) {

            }
        });
    }

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

            var json = "<h4>Ajax Response</h4><pre>"
                + JSON.stringify(data, null, 4) + "</pre>";
            $('#feedback').html(json);

            console.log("SUCCESS : ", data);
            $("#btn-search").prop("disabled", false);

        },
        error: function (e) {

            var json = "<h4>Ajax Response</h4><pre>"
                + e.responseText + "</pre>";
            $('#feedback').html(json);

            console.log("ERROR : ", e);
            $("#btn-search").prop("disabled", false);

        }
    });

}
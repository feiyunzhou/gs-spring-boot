package com.learner.lbs;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application.properties")
@Log4j2
public class DriverTest {
    @Autowired
    private RestTemplate template;
    private String baseURL = "http://localhost:8080";
    private double[][] startLoc = {{39.953511, 116.381966},
                                   {39.954166, 116.376183},
                                   {39.963036, 116.390079},
                                   {40.017516, 116.359671},
                                   {39.969652, 116.417899},
                                   {39.959440, 116.425099},
                                   {39.954070, 116.418769},
                                   {39.950500, 116.430345}
                                   };

    private List<VirtualDriver> driverList = Lists.newArrayList();
    private List<VirtualRider> riderList = Lists.newArrayList();
    private Trip currentTrip;

    @Before
    public void setUp() throws Exception {
    }

    /**
     * 用于模拟多辆汽车发送位置信息给服务器端，并且能处理消息
     * @throws InterruptedException
     */
    @Test
    public void driveCar() throws InterruptedException {
        VirtualDriver driver = new VirtualDriver("test1");
        driver.setRestTemplate(template);
        driver.run();
        driverList.add(driver);

        for (int i=2;i<6;i++){
            double lat = startLoc[i][0];
            double lng = startLoc[i][1];
            VirtualDriver drv = new VirtualDriver("test"+ i, lat, lng);
            drv.setRestTemplate(template);
            drv.run();
            driverList.add(drv);
        }

        TimeUnit.HOURS.sleep(1);
    }

    @Test
    public void rideTest() throws Exception {
        VirtualRider rider = new VirtualRider("rider1", 39.941033, 116.425099);
        rider.setRestTemplate(template);
        rider.sendTripRequest();

        while (true) {
            currentTrip = rider.queryTrip();
            log.info("current trip: {}", currentTrip);
            if (!Strings.isNullOrEmpty(currentTrip.getDriverUserName())) break;
            TimeUnit.SECONDS.sleep(10);
        }
    }
}

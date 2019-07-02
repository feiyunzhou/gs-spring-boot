package com.learner.lbs;

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

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application.properties")
@Log4j2
public class DriverTest {
    @Autowired
    private RestTemplate template;
    private double[][] startLoc = {{39.953511, 116.381966},
                                   {39.954166, 116.376183},
                                   {39.963036, 116.390079},
                                   {40.017516, 116.359671},
                                   {39.969652, 116.417899},
                                   {39.959440, 116.425099},
                                   {39.954070, 116.418769},
                                   {39.950500, 116.430345}
                                   };

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void driveCar() throws InterruptedException {
        VirtualDriver driver = new VirtualDriver("test1");
        driver.setRestTemplate(template);
        driver.run();

        for (int i=2;i<6;i++){
            double lat = startLoc[i][0];
            double lng = startLoc[i][1];
            VirtualDriver drv = new VirtualDriver("test"+ i, lat, lng);
            drv.setRestTemplate(template);
            drv.run();
        }

        TimeUnit.HOURS.sleep(1);
    }

    @Test
    public void rideTest() throws Exception {
        VitrualRider rider = new VitrualRider("rider1", 39.941033, 116.425099);
        rider.setRestTemplate(template);
        rider.sendTripRequest();

        rider.queryTrip();
    }
}

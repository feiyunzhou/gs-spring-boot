package com.learner;

import com.google.common.collect.Maps;
import com.learner.lbs.InterestingPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class WelcomeController {

    // inject via application.properties
    @Value("${welcome.message}")
    private String message;

    private static double[][] startLoc = {{39.953511, 116.381966},
            {39.954166, 116.376183},
            {39.963036, 116.390079},
            {40.017516, 116.359671},
            {39.969652, 116.417899},
            {39.959440, 116.425099},
            {39.954070, 116.418769},
            {39.950500, 116.430345}
    };
    private static Map<String, InterestingPoint> driverStartLocs = Maps.newConcurrentMap();
    static {
        for (int i=1;i<6;i++){
            double lat = startLoc[i][0];
            double lng = startLoc[i][1];
            InterestingPoint location = new InterestingPoint();
            location.setLat(lat);
            location.setLng(lng);
            location.setUserName("test"+i);
            driverStartLocs.put("test"+i, location);
        }
    }

    private List<String> tasks = Arrays.asList("a", "b", "c", "d", "e", "f", "g");

    @GetMapping("/world")
    public String main(Model model) {
        model.addAttribute("message", message);
        model.addAttribute("tasks", tasks);

        return "welcome"; //view
    }

    // /hello?name=kotlin
    @GetMapping("/hello")
    public String mainWithParam(
            @RequestParam(name = "name", required = false, defaultValue = "") 
			String name, Model model) {

        model.addAttribute("message", name);

        return "welcome"; //view
    }

    // /hello?name=kotlin
    @GetMapping("/rider")
    public String baidumap(Model model) {

        //model.addAttribute("message", name);

        return "rider"; //view
    }
    @GetMapping("/driver")
    public String driverMap(HttpServletRequest request, Model model) {

        //model.addAttribute("message", name);

        model.addAttribute("userName", request.getParameter("userName"));
        InterestingPoint location = new InterestingPoint();
        model.addAttribute("location", driverStartLocs.get(request.getParameter("userName")));
        return "driver"; //view
    }

    @GetMapping("/index")
    public String indexpage(Model model) {

        //model.addAttribute("message", name);

        return "index"; //view
    }

    @GetMapping("/user.html")
    public String userperage(Model model) {

        //model.addAttribute("message", name);

        return "user"; //view
    }
    @GetMapping("/message.html")
    public String messagepage(Model model) {

        //model.addAttribute("message", name);

        return "message"; //view
    }
}
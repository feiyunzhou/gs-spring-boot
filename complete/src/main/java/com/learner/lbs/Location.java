package com.learner.lbs;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class Location {
    private String locationId;
    private double lat;
    private double lng;
    private String userName;
    private Date createTime;
}

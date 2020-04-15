package com.easygql.util;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Data
public class Trigger {
    private String id;
    private String name;
    private String typename;
    private List<String> eventtype;
    private HashMap headers;
    private String ok_status;
    private String payloadformatter;
    private List<String> payloadargs;
    private int retry_times=0;
    private String webhookurl;
    private String expiredate;
    private String startdate;
    private String description;
}

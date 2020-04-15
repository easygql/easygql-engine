package com.easygql.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public  class MessageClient {
    public HashMap payload;
    public String id;
    public String type;
    @JsonCreator
    public MessageClient( HashMap payload,@JsonProperty("id") String id, @JsonProperty("type") String type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }
}

package com.easygql.util;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class MessageServer {

    public String id;
    public String type;
    public Object payload;

    public MessageServer(Object payload, String id, String type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }
}

package com.easygql.util;

/**
 * @author ：fenyorome
 * @date ：Created in 2019/1/20/020 22:17
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
public class MessageTypes {
    public static final String GQL_CONNECTION_INIT = "connection_init";
    public static final String GQL_CONNECTION_ACK = "connection_ack";
    public static final String GQL_CONNECTION_ERROR = "connection_error";
    public static final String GQL_CONNECTION_KEEP_ALIVE = "ka";
    public static final String GQL_CONNECTION_TERMINATE = "connection_terminate";
    public static final String GQL_START = "start";
    public static final String GQL_DATA = "data";
    public static final String GQL_ERROR = "error";
    public static final String GQL_COMPLETE = "complete";
    public static final String GQL_STOP = "stop";
}

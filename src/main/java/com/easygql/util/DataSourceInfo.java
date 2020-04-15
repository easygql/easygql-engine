package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author guofen
 * @date 2019/11/2 17:25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceInfo implements Cloneable{
    private String id;
    private String name;
    //ip or hostname
    private String host;
    private Integer port;
    private String databasename;
    private String character_encoding;
    private Integer max_connection;
    private Integer min_connection;
    private Integer max_idle_connection;
    private Integer min_idle_connection;
    private String username;
    private String password;
    private String connectionstr;
    private String replica_name;

    @Override
    public DataSourceInfo clone()  {
        return  JSONObject.parseObject(JSONObject.toJSONString(this), DataSourceInfo.class);
    }
}

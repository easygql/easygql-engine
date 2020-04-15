package com.easygql.util;

import lombok.Data;

import java.util.HashMap;

@Data
public class ThirdAPIInput {
    private String schemaID;
    private SchemaData schemaData;
    private HashMap userInfo;
    private HashMap<String,Object> runTimeInfo;
}

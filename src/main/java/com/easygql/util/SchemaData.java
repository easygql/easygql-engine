package com.easygql.util;

import lombok.Data;

import java.util.HashMap;

/**
 * @author guofen
 * @date 2019/11/6 21:09
 */
@Data
public class SchemaData {

    private String schemaname;
    private HashMap<String,ObjectTypeMetaData> objectMetaData;
    private HashMap<String,APIMetaData> queryMetaData;
    private HashMap<String,APIMetaData> mutationMetaData;
    private HashMap<String,APIMetaData> subscriptionMetaData;
    private String schemaid;
    private String databasekind;
    private DataSourceInfo datasourceinfo;
    private String idl;
    private HashMap<String,EnumTypeMetaData> enuminfo;

    /**
     * 从IDL反向构造SchemaMetaData
     * @param idl
     * @return
     */
    public static SchemaData fromIDL(String idl) {
        SchemaData schemaData =null;
       return schemaData;
    }



    public static SchemaData fromSchemaObject(SchemaObject schemaObject) {
        SchemaData schemaData = null;
        return null;
    }
    public HashMap getHashMapRep() {
        HashMap hashMap = new HashMap();
        return  hashMap;
    }
    private void constructUnExecutableSchema(){

    }
    private void constructExecutableSchema(){

    }


}

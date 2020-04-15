package com.easygql.dao;

import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public interface DataDeleter {
     void Init(String objectName, SchemaData schemaData,String schemaID);
     CompletableFuture<Map> deleteDocs(Object whereinput);
}

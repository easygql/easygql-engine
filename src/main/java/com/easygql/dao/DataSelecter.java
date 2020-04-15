package com.easygql.dao;

import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public interface DataSelecter {
    void Init(String objectName, SchemaData schemaData,String schemaID);
    CompletableFuture<Map> getSingleDoc(Object condition, HashMap<String, Object> selectFields);
    CompletableFuture<List<Map>> getFilterDocs(Object InputObj, Integer skip, Integer limit, String orderby, HashMap<String, Object> selectFields);
}

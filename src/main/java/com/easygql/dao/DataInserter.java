package com.easygql.dao;

import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public interface DataInserter {
     void Init(String objectName, SchemaData schemaData,String schemaID);
     CompletableFuture<HashMap> insertDoc(HashMap<String,HashMap> doc, String conflictStrategy,HashMap<String,Object> selectionFields );
}

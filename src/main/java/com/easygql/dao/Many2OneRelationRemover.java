package com.easygql.dao;


import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 */
public interface Many2OneRelationRemover extends RelationRemover{
    void Init(SchemaData schemaData, String schemaID, RelationField relationField);
    CompletableFuture<Object> fromRemove(String srcId);
    CompletableFuture<Object> toRemove(String destID, List<String> srcID,Boolean isReset);
}

package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Many2OneRelationCreater extends RelationObjectCreator {
    void Init(SchemaData schemaData, String schemaID, RelationField relationField);
    CompletableFuture<Object> fromAdd(String srcID,String destID,Boolean reset);
    CompletableFuture<Object> toAdd(List<String> srcID, String destID,Boolean reset);
}


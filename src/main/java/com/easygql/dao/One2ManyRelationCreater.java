package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface One2ManyRelationCreater  extends RelationObjectCreator{
    void Init(SchemaData schemaData, String schemaID, RelationField relationField);
    CompletableFuture<Object> fromAdd(@NonNull String srcID, Object targetObject, Boolean reset);
    CompletableFuture<Object> fromByID(@NonNull String srcID, List<String> destID, Boolean reset);
    CompletableFuture<Object> toAdd(@NonNull String toID,String srcID,Boolean reset);
}

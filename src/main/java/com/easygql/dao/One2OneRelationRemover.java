package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import lombok.NonNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public interface One2OneRelationRemover  extends RelationRemover{
    void Init(SchemaData schemaData, String schemaID, RelationField relationField);
    CompletableFuture<Object> fromRemove(@NonNull  String srcID);
    CompletableFuture<Object> toRemove(@NonNull  String destID);
}

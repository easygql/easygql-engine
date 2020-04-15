package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import lombok.NonNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public interface One2OneRelationCreater  extends RelationObjectCreator {
  void Init(SchemaData schemaData, String schemaID, RelationField relationField);
  CompletableFuture<Object> doAdd(@NonNull String srcID, Object targetObject);
  CompletableFuture<Object> doAddByID(@NonNull String srcID, String destID);
  CompletableFuture<Object> toAdd(@NonNull String destID,String srcID);
}

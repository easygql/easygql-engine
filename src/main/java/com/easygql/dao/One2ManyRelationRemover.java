package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface One2ManyRelationRemover extends RelationRemover{
  void Init(SchemaData schemaData, String schemaID, RelationField relationField);
  CompletableFuture<Object> fromRemove(@NonNull String srcID, List<String> destID);
  CompletableFuture<Object> toRemove(@NonNull String destID);
}

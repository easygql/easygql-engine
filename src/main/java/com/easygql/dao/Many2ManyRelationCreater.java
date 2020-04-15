package com.easygql.dao;

import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Many2ManyRelationCreater  extends RelationObjectCreator{
  void Init(SchemaData schemaData, String schemaID, RelationField relationField);
  CompletableFuture<Object> fromAdd(String srcID, List<String> targetList, Boolean reset);
  CompletableFuture<Object> toAdd(String destID, List<String> srcIDList, Boolean reset);
}

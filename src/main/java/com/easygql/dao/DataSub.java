package com.easygql.dao;

import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.reactivex.ObservableEmitter;
import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019/12/18 19:35
 */
public interface DataSub {
  void Init(String objectName, SchemaData schemaData, String schemaID);

  CompletableFuture<Void> doSub(
      ObservableEmitter<Object> emitter,
      String schemaID,
      SchemaData schemaData,
      String objectName,
      List<String> selectFields,
      List<String> watchFields,String changeFeedName);

  CompletableFuture<Void> doClose(String changeFeedName );
}

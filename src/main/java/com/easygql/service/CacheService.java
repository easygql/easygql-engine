package com.easygql.service;

import com.easygql.dao.DataSub;
import com.easygql.util.SchemaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public interface CacheService {
  CompletableFuture<Void> addWebSocket(
      String schemaID,
      SchemaData schemaData,
      HashMap<String, Object> selectFieldsMap,
      HashMap<String, Object> watchFieldsMap,
      String objectName,
      String subscriptionId,
      String sessionID,
      Object whereInput,
      DataSub dataSub);

  CompletableFuture<Void> endSubThread(String changeFeedName);

  CompletableFuture<Void> endWebSocket(String sessionID);

  CompletableFuture<Void> endWebSocketSub(String subinfoID,String sessionID);

  CompletableFuture<Void> clearSubscription(Long timeStamp);

  CompletableFuture<Void> removeUnsubThread();

  CompletableFuture<Void> stopSchema(String schemaID);

  CompletableFuture<List<Map<String, Object>>> getSubInfoByThread(String changeFeedName);
}

package com.easygql.service;

import com.easygql.dao.DataSub;
import com.easygql.exception.BusinessException;
import com.easygql.util.SchemaData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_SUBSCRIPTION_NAME_PREFIX;

public class SubscriptionService {
  private static HashMap<String, Flowable<Object>> changeFeedMap=new HashMap<>();
  private static HashMap<String, DataSub> dataSubOfChangeFeed=new HashMap<>();
  public static String getSubscriptionName(
      String objectName, List<String> selectFields, List<String> watchFields, String schemaID) {
    List<String> keyList = new ArrayList<>();
    if(null!=selectFields) {
        keyList.addAll(selectFields);
    }
    Collections.sort(keyList);
    List<String> watchList = new ArrayList<>();
    if(null!=watchFields) {
        watchList.addAll(watchFields);
    }
    Collections.sort(watchList);
    keyList.add("|");
    keyList.addAll(watchList);
    keyList.add("|");
    keyList.add(schemaID);
    String keyStr = keyList.stream().collect(Collectors.joining(" "));
    return (objectName
        + GRAPHQL_SUBSCRIPTION_NAME_PREFIX
        + DigestUtils.sha1Hex(keyStr).substring(0, 16)).toLowerCase();
  }

  public static CompletableFuture<Void> doSub(
      String changeFeedName,
      String schemaID,
      SchemaData schemaData,
      String objectName,
      List<String> selectFields,
      List<String> watchFields,
      DataSub dataSub) {
    return CompletableFuture.runAsync(
        () -> {
          Observable<Object> changeFeedsObservable =
              Observable.create(
                  emitter -> {
                    dataSub.doSub(
                        emitter,
                        schemaID,
                        schemaData,
                        objectName,
                        selectFields,
                        watchFields,
                        changeFeedName);
                  });
          Observable<Object> resultObservable = changeFeedsObservable.share();
          changeFeedMap.put(
              changeFeedName, resultObservable.toFlowable(BackpressureStrategy.BUFFER));
          dataSubOfChangeFeed.put(changeFeedName, dataSub);
        });
  }

  public static CompletableFuture<Void> doClose(String changeFeedName) {
    return CompletableFuture.runAsync(
        () -> {
          dataSubOfChangeFeed.get(changeFeedName).doClose(changeFeedName);
        });
  }
  public static Disposable subscriber(@NonNull  String changeFeedName, Consumer<Object> consumer) {
      if(null !=changeFeedMap.get(changeFeedName)) {
            return changeFeedMap.get(changeFeedName).subscribe(consumer);
      }  else {
          throw new BusinessException("E10044");
      }
  }
}

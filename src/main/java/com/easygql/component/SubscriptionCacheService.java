package com.easygql.component;

import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.DataSub;
import com.easygql.exception.BusinessException;
import com.easygql.handler.GraphQLWebSocketHandler;
import com.easygql.service.CacheService;
import com.easygql.service.SubscriptionService;
import com.easygql.threads.SubscriptionCleaner;
import com.easygql.util.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;
import java.util.regex.Pattern;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Component("h2CacheService")
@Order(2)
@Slf4j
public class SubscriptionCacheService implements CacheService {
  @Autowired
  @Qualifier("h2JdbcTemplate")
  private JdbcTemplate h2JdbcTemplate;

  public static SubscriptionCacheService subscriptionCacheService;
  private StampedLock lock = new StampedLock();
  private final String insertWebsocket =
      "insert into websocket(subscription_id,session_id,handler,change_feed_name) values(?,?,?,?)";
  private final String querySubInfo =
      "select change_feed_name from subscription_thread where change_feed_name=? ";
  private final String createSubThread =
      "insert into subscription_thread(change_feed_name,schema_id,object_name,select_fields,watch_fields,last_update,sub_count) values(?,?,?,?,?,?,?) ";
  private final String updateThread =
      "update subscription_thread set last_update=?,sub_count=sub_count+1 where change_feed_name=?";
  private final String changeFeedSql =
      "CREATE  TABLE subscription_thread(\n"
          + "change_feed_name varchar primary key  not null,\n"
          + "schema_id char(32) not null,\n"
          + "object_name varchar not null,\n"
          + "select_fields varchar not null,\n"
          + "watch_fields varchar not null,\n"
          + "last_update bigint,\n"
          + "sub_count int\n"
          + ")";
  private final String WebSocketSql =
      "CREATE  TABLE websocket(\n"
          + "subscription_id varchar(32) not null,\n"
          + "session_id varchar(50) not null,\n"
          + "handler varchar(32) not null,"
          + "change_feed_name varchar, \n"
          + " constraint  pk_websocket_sub primary key (subscription_id,session_id)"
          + ")";
  private final String deleteSubThread =
      "delete from subscription_thread where change_feed_name=? ";
  private final String removeWebsocketSub = "delete from websocket where change_feed_name=? ";
  private final String DecThreadByChangFeedName =
      " update subscription_thread set sub_count=sub_count-1,last_update=? where change_feed_name in (select change_feed_name from websocket where session_id=?)";
  private final String removeWebsocket = "delete from  websocket where session_id=?";
  private final String getUnsubThread =
      "select change_feed_name from subscription_thread where change_feed_name not in (select distinct change_feed_name from websocket)";
  private final String removeUnsubThread =
      "delete from subscription_thread where change_feed_name not in  (select distinct change_feed_name from websocket)";;
  private String deleteWebsocketByID =
      "delete from websocket where change_feed_name in  ( select change_feed_name from"
          + " subscription_thread where schema_id = ? ) ?";
  private final String selectChangeFeedNameBySchema =
      " select change_feed_name from subscription_thread where schema_id=?";
  private final String deleteWebsocketBySchema =
      "delete from websocket where change_feed_name in ( select change_feed_name from subscription_thread where schema_id = ?)";
  private final String deleteChangeFeedBySchema =
      " delete from subscription_thread where schema_id=?";
  private final String delChangeFeedBySubscription =
      " update subscription_thread set sub_count=sub_count-1,last_update=? where change_feed_name in (select change_feed_name from websocket where subscription_id=? and session_id= ? )";
  private final String removeSubscriptionByID =
      "delete from  websocket where subscription_id=? and session_id=?";
  private final String getUnsubThreadFromTime =
      " select  change_feed_name from subscription_thread  where change_feed_name not in (select distinct change_feed_name from websocket) and last_update<? ";

  private final String destroyChangeFeedFromTime =
      " delete from subscription_thread  where change_feed_name not in (select distinct change_feed_name from websocket) and last_update<? ";
  private final String getSubscriptionByChangeFeed =
      "select websocket.session_id, websocket.subscription_id,websocket.handler from websocket left join subscription_thread  on websocket.change_feed_name=subscription_thread.change_feed_name  where  subscription_thread.change_feed_name=? ";
  private final String getSubInfoByChangeFeed =
      " select session_id,subscription_id,handler from websocket where change_feed_name = ?";
  private final String getSubInfoByWebsocket =
      " select subscription_id,handler,change_feed_name from websocket where session_id=?";
  private final String getSubInfoBySchema =
      "select websocket.session_id, websocket.subscription_id,websocket.handler,websocket.change_feed_name  from PUBLIC.websocket left join subscription_thread  on websocket.change_feed_name=subscription_thread.change_feed_name  where  subscription_thread.schema_id=? ";
  private HashMap<String, Disposable> subscribeMap =
      new HashMap<String, io.reactivex.disposables.Disposable>();
  /** */
  @PostConstruct
  public void init() {
    // 验证数据库是否存在
    try {
      this.h2JdbcTemplate.update(changeFeedSql);
      this.h2JdbcTemplate.update(WebSocketSql);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        log.error("{}", LogData.getErrorLog("E10015", errorMap, e));
      }
    }
    subscriptionCacheService = this;
    SubscriptionCleaner subscriptionCleaner = new SubscriptionCleaner();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(subscriptionCleaner);
  }

  /**
   * @param schemaID
   * @param schemaData
   * @param selectFieldsMap
   * @param watchFieldsMap
   * @param objectName
   * @param subscriptionId
   * @param sessionID
   * @param dataSub
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> addWebSocket(
      String schemaID,
      SchemaData schemaData,
      HashMap<String, Object> selectFieldsMap,
      HashMap<String, Object> watchFieldsMap,
      String objectName,
      String subscriptionId,
      String sessionID,
      Object whereInput,
      DataSub dataSub) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String handlerID = sessionID + "_" + subscriptionId;
    CompletableFuture.supplyAsync(
            () -> {
              try {
                List<String> selectFields = getFields(selectFieldsMap);
                List<String> watchFields = getFields(watchFieldsMap);
                if (selectFields.size() == 0 && watchFields.size() == 0) {
                  throw new BusinessException("E10070");
                }
                String changeFeedName =
                    SubscriptionService.getSubscriptionName(
                        objectName, selectFields, watchFields, schemaID);
                int rows =
                    h2JdbcTemplate.update(
                        insertWebsocket, subscriptionId, sessionID, handlerID, changeFeedName);
                if (1 != rows) {
                  throw new BusinessException("E10035");
                }
                Long stamp = lock.writeLock();
                try {
                  List<Map<String, Object>> result =
                      h2JdbcTemplate.queryForList(querySubInfo, changeFeedName);
                  if (result.size() < 1) {
                    rows =
                        h2JdbcTemplate.update(
                            createSubThread,
                            changeFeedName,
                            schemaID,
                            objectName,
                            JSONObject.toJSONString(selectFields),
                            JSONObject.toJSONString(watchFields),
                            System.currentTimeMillis(),
                            1);
                    if (rows < 1) {
                      throw new BusinessException("E10035");
                    }
                    SubscriptionService.doSub(
                            changeFeedName,
                            schemaID,
                            schemaData,
                            objectName,
                            selectFields,
                            watchFields,
                            dataSub)
                        .get();

                  } else if (result.size() > 1) {
                    if (log.isErrorEnabled()) {
                      HashMap errorMap = new HashMap();
                      errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaID);
                      errorMap.put(GRAPHQL_SELECTFIELDS_FIELDNAME, selectFieldsMap);
                      errorMap.put(GRAPHQL_WATCHFIELDS_FIELDNAME, watchFieldsMap);
                      errorMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectName);
                      errorMap.put(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT, subscriptionId);
                      errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
                      errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereInput);
                      log.error(
                          "{}",
                          LogData.getErrorLog("E10034", errorMap, new BusinessException("E10034")));
                    }
                  } else {
                    h2JdbcTemplate.update(updateThread, System.currentTimeMillis(), changeFeedName);
                  }
                  return changeFeedName;
                } catch (Exception e) {
                  throw e;
                } finally {
                  lock.unlockWrite(stamp);
                }
              } catch (BusinessException e) {
                  throw e;
              }catch (Exception e) {
                throw new BusinessException("E10035");
              }
            })
        .whenCompleteAsync(
            (changeFeedName, ex) -> {
              if (null != ex) {
                if (log.isErrorEnabled()) {
                  HashMap errorMap = new HashMap();
                  errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaID);
                  errorMap.put(GRAPHQL_SELECTFIELDS_FIELDNAME, selectFieldsMap);
                  errorMap.put(GRAPHQL_WATCHFIELDS_FIELDNAME, watchFieldsMap);
                  errorMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectName);
                  errorMap.put(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT, subscriptionId);
                  errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
                  errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereInput);
                  log.error("{}", LogData.getErrorLog("E10035", errorMap, ex));
                }
                future.completeExceptionally(new BusinessException("E10035"));
              } else {
                Consumer<Object> mapConsumer =
                    new Consumer<Object>() {
                      @Override
                      public void accept(Object obj) throws Exception {
                        try {
                            JsonArray jsonArray = (JsonArray) obj;
                            String action = String.valueOf(jsonArray.getString(0)).toLowerCase();
                            Map whereCondition = (Map) whereInput;
                            if (SUBSCRIPTION_TYPE_REMOVE.equals(action)) {
                                JSONObject old_val = JSONObject.parseObject(jsonArray.getJsonObject(2).toString());
                                if (isAccept(old_val, whereCondition, objectName, schemaData)) {
                                    sendChangeEvent(
                                            selectFieldsMap,
                                            null,
                                            old_val,
                                            sessionID,
                                            SUBSCRIPTION_TYPE_REMOVE,
                                            subscriptionId);
                                }
                                return;
                            }
                            JSONObject new_val = JSONObject.parseObject(jsonArray.getJsonObject(1).toString());
                            if (!isAccept(new_val, whereCondition, objectName, schemaData)) {
                                if (!SUBSCRIPTION_TYPE_UPDATE.equals(action)) {
                                    return;
                                }
                                JSONObject old_val = JSONObject.parseObject(jsonArray.getJsonObject(2).toString());
                                if (isAccept(old_val, whereCondition, objectName, schemaData)) {
                                    sendChangeEvent(
                                            selectFieldsMap,
                                            null,
                                            old_val,
                                            sessionID,
                                            SUBSCRIPTION_TYPE_REMOVE,
                                            subscriptionId);
                                }
                                return;
                            }
                            JSONObject old_val = null;
                            if(null!=jsonArray.getJsonObject(2)) {
                                old_val = JSONObject.parseObject(jsonArray.getJsonObject(2).toString());
                            }
                            sendChangeEvent(
                                    selectFieldsMap, new_val, old_val, sessionID, action, subscriptionId);
                        } catch (Exception e) {
                            if(log.isErrorEnabled()) {
                                log.error("{}",LogData.getErrorLog("E10097",obj,e));
                            }
                        }
                      }
                    };
                subscribeMap.put(
                    handlerID, SubscriptionService.subscriber(changeFeedName, mapConsumer));
                future.complete(null);
              }
            });
    return future;
  }

  /**
   * @param changeFeedName
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> endSubThread(String changeFeedName) {
    return CompletableFuture.runAsync(
        () -> {
          Long stamp = lock.writeLock();
          try {
            int rows = -1;
            rows = h2JdbcTemplate.update(deleteSubThread, changeFeedName);
            if (rows < 1) {
              return;
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_THREAD_FIELDNAME, changeFeedName);
              log.error("{}", LogData.getErrorLog("E10043", errorMap, e));
            }
          } finally {
            lock.unlockWrite(stamp);
          }
          List<Map<String, Object>> result =
              h2JdbcTemplate.queryForList(getSubInfoByChangeFeed, changeFeedName);
          result.forEach(
              (stringObjectMap -> {
                String handlerID = (String) stringObjectMap.get("handler");
                Disposable disposable = subscribeMap.remove(handlerID);
                if (null != disposable) {
                  disposable.dispose();
                }
              }));
          this.h2JdbcTemplate.update(removeWebsocketSub, changeFeedName);
          try {
            SubscriptionService.doClose(changeFeedName).get();
          } catch (InterruptedException e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_THREAD_FIELDNAME, changeFeedName);
              log.error("{}", LogData.getErrorLog("E10043", errorMap, e));
            }
          } catch (ExecutionException e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_THREAD_FIELDNAME, changeFeedName);
              log.error("{}", LogData.getErrorLog("E10043", errorMap, e));
            }
          }
        });
  }

  /**
   * @param sessionID
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> endWebSocket(String sessionID) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            List<Map<String, Object>> result =
                h2JdbcTemplate.queryForList(getSubInfoByWebsocket, sessionID);
            result.forEach(
                (stringObjectMap -> {
                  String handlerID = (String) stringObjectMap.get("handler");
                  Disposable disposable = subscribeMap.remove(handlerID);
                  if (null != disposable) {
                    disposable.dispose();
                  }
                }));
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
              log.error("{}", LogData.getErrorLog("E10042", errorMap, e));
            }
          }
          Long stamp = lock.writeLock();
          try {
            h2JdbcTemplate.update(DecThreadByChangFeedName, System.currentTimeMillis(), sessionID);
            h2JdbcTemplate.update(removeWebsocket, sessionID);
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
              log.error("{}", LogData.getErrorLog("E10042", errorMap, e));
            }
          } finally {
            lock.unlockWrite(stamp);
          }
        });
  }

  /** @return */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> removeUnsubThread() {
    return CompletableFuture.runAsync(
        () -> {
          List<Map<String, Object>> result = null;
          Long stamp = lock.writeLock();
          try {
            result = h2JdbcTemplate.queryForList(getUnsubThread);
            if (result.size() > 0) {
              h2JdbcTemplate.update(removeUnsubThread);
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              log.error("{}", LogData.getErrorLog("E10041", null, e));
            }
          } finally {
            lock.unlockWrite(stamp);
          }
          if (null != result && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
              Map map = result.get(i);
              String changeFeedName = (String) (map.get("change_feed_name"));
              SubscriptionService.doClose(changeFeedName);
            }
          }
        });
  }

  /**
   * @param schemaID
   * @return
   */
  @Override
  public CompletableFuture<Void> stopSchema(String schemaID) {
    return CompletableFuture.runAsync(
        () -> {
          h2JdbcTemplate.update(deleteWebsocketByID, schemaID);
          Long stamp = lock.writeLock();
          List<Map<String, Object>> result = null;
          try {
            result = h2JdbcTemplate.queryForList(getSubInfoBySchema, schemaID);
            result.forEach(
                (stringObjectMap -> {
                  String handlerID = (String) stringObjectMap.get("handler");
                  Disposable disposable = subscribeMap.remove(handlerID);
                  if (null != disposable) {
                    disposable.dispose();
                  }
                }));
            h2JdbcTemplate.update(deleteWebsocketBySchema, schemaID);
            h2JdbcTemplate.update(deleteChangeFeedBySchema, schemaID);
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME, schemaID);
              log.error("{}", LogData.getErrorLog("E10040", errorMap, e));
            }
          } finally {
            lock.unlockRead(stamp);
          }
          if (null != result) {
            for (int i = 0; i < result.size(); i++) {
              String changeFeedName = String.class.cast(result.get(i));
              SubscriptionService.doClose(changeFeedName);
            }
          }
        });
  }

  /**
   * @param subinfoID
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> endWebSocketSub(String subinfoID, String sessionID) {
    return CompletableFuture.runAsync(
        () -> {
          String handlerID = sessionID + "_" + subinfoID;
          try {
            Disposable disposable = subscribeMap.remove(handlerID);
            if (null != disposable) {
              disposable.dispose();
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT, subinfoID);
              errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
              log.error("{}", LogData.getErrorLog("E10039", errorMap, e));
            }
          }
          Long stamp = lock.writeLock();
          try {
            h2JdbcTemplate.update(
                delChangeFeedBySubscription, System.currentTimeMillis(), subinfoID,sessionID);
            h2JdbcTemplate.update(removeSubscriptionByID, subinfoID, sessionID);
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT, subinfoID);
              errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT, sessionID);
              log.error("{}", LogData.getErrorLog("E10039", errorMap, e));
            }
          } finally {
            lock.unlockWrite(stamp);
          }
        });
  }

  /**
   * @param timeStamp
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<Void> clearSubscription(Long timeStamp) {
    return CompletableFuture.runAsync(
        () -> {
          List<Map<String, Object>> result = null;
          Long stamp = lock.writeLock();
          try {
            result = h2JdbcTemplate.queryForList(getUnsubThreadFromTime, timeStamp);
            int rows = h2JdbcTemplate.update(destroyChangeFeedFromTime, timeStamp);
            if (rows != result.size()) {
              if (log.isErrorEnabled()) {
                log.error(
                    "{}", LogData.getErrorLog("E10038", null, new BusinessException("E10038")));
              }
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              log.error("{}", LogData.getErrorLog("E10038", null, e));
            }
          } finally {
            lock.unlockWrite(stamp);
          }
          if (null != result) {
            for (Map<String, Object> map : result) {
              String changeFeedName = String.class.cast(map.get("change_feed_name"));
              SubscriptionService.doClose(changeFeedName);
            }
          }
        });
  }

  /**
   * @param changeFeedName
   * @return
   */
  @Override
  @Transactional("h2TXManager")
  public CompletableFuture<List<Map<String, Object>>> getSubInfoByThread(String changeFeedName) {
    return CompletableFuture.supplyAsync(
        () -> {
          return h2JdbcTemplate.queryForList(getSubscriptionByChangeFeed, changeFeedName);
        });
  }

  /**
   * @param fieldsMap
   * @return
   */
  public static List<String> getFields(HashMap<String, Object> fieldsMap) {
    if (null == fieldsMap) {
      return new ArrayList<>();
    }
    Iterator<Map.Entry<String, Object>> entryIterator = fieldsMap.entrySet().iterator();
    List<String> fieldList = new ArrayList<>();
    while (entryIterator.hasNext()) {
      Map.Entry<String, Object> entry = entryIterator.next();
      if (isField(entry.getValue())) {
        fieldList.add(entry.getKey());
      }
    }
    return fieldList;
  }

  /**
   * @param obj
   * @return
   */
  public static boolean isField(Object obj) {
    if (obj instanceof Boolean) {
      boolean exist = (Boolean) obj;
      if (exist) {
        return true;
      } else {
        return false;
      }
    } else if (obj instanceof Map) {
      Map<String, Object> map = Map.class.cast(obj);
      Iterator<Object> valueSetIterator = map.values().iterator();
      while (valueSetIterator.hasNext()) {
        if (isField(valueSetIterator.next())) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  public static Boolean isAccept(
      Map changeEvent, Map whereCondition, String objectName, SchemaData schemaData) {
    if (null == whereCondition) {
      return true;
    } else {
      Iterator<Map.Entry> iterator = whereCondition.entrySet().iterator();
      if (iterator.hasNext()) {
        Map.Entry entry = iterator.next();
        String filterKey = (String) entry.getKey();
        if (GRAPHQL_FILTER_FILTER_OPERATOR.equals(filterKey)) {
          ObjectTypeMetaData objectTypeMetaData = schemaData.getObjectMetaData().get(objectName);
          HashMap<String, Object> filterCondition =
              (HashMap<String, Object>) whereCondition.get(GRAPHQL_FILTER_FILTER_OPERATOR);
          Iterator<Map.Entry<String, Object>> entryConditionIterator =
              filterCondition.entrySet().iterator();
          while (entryConditionIterator.hasNext()) {
            Map.Entry<String, Object> filterEntry = entryConditionIterator.next();
            String fieldName = filterEntry.getKey();
            Object filterValue = filterEntry.getValue();
            String fieldKind = objectTypeMetaData.getFields().get(fieldName);
            Map<String, Object> operatorMap = Map.class.cast(filterValue);
            Iterator<Map.Entry<String, Object>> operatorIterator =
                operatorMap.entrySet().iterator();
            if (fieldKind.equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
              ScalarFieldInfo scalarFieldInfo =
                  objectTypeMetaData.getScalarFieldData().get(fieldName);
              String fieldType = scalarFieldInfo.getType();
              if (fieldType.equals(GRAPHQL_INT_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<Integer> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                      Integer leafValue = Integer.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    Integer fieldValue = Integer.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        Integer eqLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        Integer geLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (fieldValue < geLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        Integer gtLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (fieldValue <= gtLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        Integer leLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (fieldValue > leLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        Integer ltLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (fieldValue >= ltLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        Integer neLeafValue = Integer.class.cast(operatorEntry.getValue());
                        if (fieldValue == neLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<Integer> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else if (fieldType.equals(GRAPHQL_LONG_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<Long> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                      Long leafValue = Long.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    Long fieldValue = Long.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        Long eqLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        Long geLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (fieldValue < geLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        Long gtLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (fieldValue <= gtLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        Long leLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (fieldValue > leLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        Long ltLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (fieldValue >= ltLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        Long neLeafValue = Long.class.cast(operatorEntry.getValue());
                        if (fieldValue == neLeafValue) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<Long> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else if (fieldType.equals(GRAPHQL_BIGDECIMAL_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<BigDecimal> fieldValue =
                          ArrayList.class.cast(changeEvent.get(fieldName));
                      BigDecimal leafValue = BigDecimal.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    BigDecimal fieldValue = BigDecimal.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        BigDecimal eqLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        BigDecimal geLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        int geRes = fieldValue.compareTo(geLeafValue);
                        if (geRes < 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        BigDecimal gtLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        int gtRes = fieldValue.compareTo(gtLeafValue);
                        if (gtRes <= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        BigDecimal leLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        int leRes = fieldValue.compareTo(leLeafValue);
                        if (leRes > 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        BigDecimal ltLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        int ltRes = fieldValue.compareTo(ltLeafValue);
                        if (ltRes >= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        BigDecimal neLeafValue = BigDecimal.class.cast(operatorEntry.getValue());
                        int neRes = fieldValue.compareTo(neLeafValue);
                        if (neRes == 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<BigDecimal> inLeafValue =
                            ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else if (fieldType.equals(GRAPHQL_SHORT_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<Short> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                      Short leafValue = Short.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    Short fieldValue = Short.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        Short eqLeafValue = Short.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        Short geLeafValue = Short.class.cast(operatorEntry.getValue());
                        int geRes = fieldValue.compareTo(geLeafValue);
                        if (geRes < 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        Short gtLeafValue = Short.class.cast(operatorEntry.getValue());
                        int gtRes = fieldValue.compareTo(gtLeafValue);
                        if (gtRes <= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        Short leLeafValue = Short.class.cast(operatorEntry.getValue());
                        int leRes = fieldValue.compareTo(leLeafValue);
                        if (leRes > 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        Short ltLeafValue = Short.class.cast(operatorEntry.getValue());
                        int ltRes = fieldValue.compareTo(ltLeafValue);
                        if (ltRes >= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        Short neLeafValue = Short.class.cast(operatorEntry.getValue());
                        int neRes = fieldValue.compareTo(neLeafValue);
                        if (neRes == 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<Short> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else if (fieldType.equals(GRAPHQL_FLOAT_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<Double> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                      Double leafValue = Double.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    Double fieldValue = Double.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        Double eqLeafValue = Double.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        Double geLeafValue = Double.class.cast(operatorEntry.getValue());
                        int geRes = fieldValue.compareTo(geLeafValue);
                        if (geRes < 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        Double gtLeafValue = Double.class.cast(operatorEntry.getValue());
                        int gtRes = fieldValue.compareTo(gtLeafValue);
                        if (gtRes <= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        Double leLeafValue = Double.class.cast(operatorEntry.getValue());
                        int leRes = fieldValue.compareTo(leLeafValue);
                        if (leRes > 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        Double ltLeafValue = Double.class.cast(operatorEntry.getValue());
                        int ltRes = fieldValue.compareTo(ltLeafValue);
                        if (ltRes >= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        Double neLeafValue = Double.class.cast(operatorEntry.getValue());
                        int neRes = fieldValue.compareTo(neLeafValue);
                        if (neRes == 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<Double> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else if (fieldType.equals(GRAPHQL_STRING_TYPENAME)
                  || fieldType.equals(GRAPHQL_ID_TYPENAME)) {
                while (operatorIterator.hasNext()) {
                  Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                  String operator = operatorEntry.getKey();
                  if (scalarFieldInfo.isIslist()) {
                    if (!operator.equals(GRAPHQL_FILTER_HASONE_OPERATOR)) {
                      if (log.isErrorEnabled()) {
                        HashMap errorMap = new HashMap();
                        errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                        errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                        errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                        log.error(
                            "{}",
                            LogData.getErrorLog(
                                "E10011", errorMap, new BusinessException("E10011")));
                      }
                      return false;
                    } else {
                      List<String> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                      String leafValue = String.class.cast(operatorEntry.getValue());
                      if (!fieldValue.contains(leafValue)) {
                        return false;
                      }
                    }
                  } else {
                    String fieldValue = String.class.cast(changeEvent.get(fieldName));
                    switch (operator) {
                      case GRAPHQL_FILTER_EQ_OPERATOR:
                        String eqLeafValue = String.class.cast(operatorEntry.getValue());
                        if (!fieldValue.equals(eqLeafValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GE_OPERATOR:
                        String geLeafValue = String.class.cast(operatorEntry.getValue());
                        int geRes = fieldValue.compareTo(geLeafValue);
                        if (geRes < 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_GT_OPERATOR:
                        String gtLeafValue = String.class.cast(operatorEntry.getValue());
                        int gtRes = fieldValue.compareTo(gtLeafValue);
                        if (gtRes <= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LE_OPERATOR:
                        String leLeafValue = String.class.cast(operatorEntry.getValue());
                        int leRes = fieldValue.compareTo(leLeafValue);
                        if (leRes > 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_LT_OPERATOR:
                        String ltLeafValue = String.class.cast(operatorEntry.getValue());
                        int ltRes = fieldValue.compareTo(ltLeafValue);
                        if (ltRes >= 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_NE_OPERATOR:
                        String neLeafValue = String.class.cast(operatorEntry.getValue());
                        int neRes = fieldValue.compareTo(neLeafValue);
                        if (neRes == 0) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_IN_OPERATOR:
                        List<String> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                        if (!inLeafValue.contains(fieldValue)) {
                          return false;
                        }
                        break;
                      case GRAPHQL_FILTER_MATCH_OPERATOR:
                        String regExpr = String.class.cast(operatorEntry.getValue());
                        if (!Pattern.matches(regExpr, fieldValue)) {
                          return false;
                        }
                        break;
                      default:
                        if (log.isErrorEnabled()) {
                          HashMap errorMap = new HashMap();
                          errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                          errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                          errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                          log.error(
                              "{}",
                              LogData.getErrorLog(
                                  "E10011", errorMap, new BusinessException("E10011")));
                        }
                        return false;
                    }
                  }
                }
              } else {
                if (log.isErrorEnabled()) {
                  HashMap errorMap = new HashMap();
                  errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                  errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                  errorMap.put(GRAPHQL_TYPE_FIELDNAME, fieldType);
                  log.error(
                      "{}",
                      LogData.getErrorLog("E10036", errorMap, new BusinessException("E10036")));
                }
                return false;
              }
            } else if (fieldKind.equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
              EnumField enumField = objectTypeMetaData.getEnumFieldData().get(fieldName);
              while (operatorIterator.hasNext()) {
                Map.Entry<String, Object> operatorEntry = operatorIterator.next();
                String operator = operatorEntry.getKey();
                if (enumField.isIslist()) {
                  if (operator.equals(GRAPHQL_FILTER_CONTAIN_OPERATOR)
                      || operator.equals(GRAPHQL_FILTER_IN_OPERATOR)) {
                    List<String> fieldValue = ArrayList.class.cast(changeEvent.get(fieldName));
                    List<String> leafValue = ArrayList.class.cast(operatorEntry.getValue());
                    HashSet<String> leafSet = new HashSet<>();
                    leafSet.addAll(leafValue);
                    leafValue.clear();
                    leafValue.addAll(leafSet);
                    if (operator.equals(GRAPHQL_FILTER_CONTAIN_OPERATOR)) {
                      if (!fieldValue.containsAll(leafValue)) {
                        return false;
                      }
                    } else {
                      if (!leafValue.containsAll(fieldValue)) {
                        return false;
                      }
                    }
                  } else {
                    if (log.isErrorEnabled()) {
                      HashMap errorMap = new HashMap();
                      errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                      errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                      errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                      log.error(
                          "{}",
                          LogData.getErrorLog("E10011", errorMap, new BusinessException("E10011")));
                    }
                    return false;
                  }
                } else {
                  String fieldValue = String.class.cast(changeEvent.get(fieldName));
                  if (operator.equals(GRAPHQL_FILTER_EQ_OPERATOR)) {
                    String eqLeafValue = String.class.cast(operatorEntry.getValue());
                    if (!fieldValue.equals(eqLeafValue)) {
                      return false;
                    }
                  } else if (operator.equals(GRAPHQL_FILTER_NE_OPERATOR)) {
                    String eqLeafValue = String.class.cast(operatorEntry.getValue());
                    if (fieldValue.equals(eqLeafValue)) {
                      return false;
                    }
                  } else if (operator.equals(GRAPHQL_FILTER_IN_OPERATOR)) {
                    List<String> inLeafValue = ArrayList.class.cast(operatorEntry.getValue());
                    if (!inLeafValue.contains(fieldValue)) {
                      return false;
                    }
                  } else {
                    if (log.isErrorEnabled()) {
                      HashMap errorMap = new HashMap();
                      errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                      errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                      errorMap.put(GRAPHQL_FILTER_FILTER_OPERATOR, operator);
                      log.error(
                          "{}",
                          LogData.getErrorLog(
                              "E10011",
                              errorMap,
                              new BusinessException("E10011")));
                    }
                    return false;
                  }
                }
              }
            } else if (fieldKind.equals(GRAPHQL_FROMRELATION_TYPENAME)) {
              if (log.isErrorEnabled()) {
                HashMap errorMap = new HashMap();
                errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                errorMap.put(GRAPHQL_FIELDNAME_FIELDNAME, fieldName);
                log.error(
                    "{}",
                    LogData.getErrorLog(
                        "E10037",
                        errorMap,
                        new BusinessException("E10037")));
              }
              return false;
            } else {
              if (log.isErrorEnabled()) {
                HashMap errorMap = new HashMap();
                errorMap.put(GRAPHQL_WHERE_ARGUMENT, whereCondition);
                errorMap.put(GRAPHQL_EVENT_ARGUMENT, changeEvent);
                errorMap.put(GRAPHQL_FIELDNAME_FIELDNAME, fieldName);
                log.error(
                    "{}",
                    LogData.getErrorLog(
                        "E10037",
                        errorMap,
                        new BusinessException("E10037")));
              }
              return false;
            }
          }
          return true;
        } else if (GRAPHQL_FILTER_AND_OPERATOR.equals(filterKey)) {
          List<Object> andCondition =
              ArrayList.class.cast(whereCondition.get(GRAPHQL_FILTER_AND_OPERATOR));
          for (Object condition : andCondition) {
            if (!isAccept(changeEvent, (Map) condition, objectName, schemaData)) {
              return false;
            }
          }
          return true;
        } else if (GRAPHQL_FILTER_OR_OPERATOR.equals(filterKey)) {
          List<Object> orCondition =
              ArrayList.class.cast(whereCondition.get(GRAPHQL_FILTER_AND_OPERATOR));
          if (orCondition.size() == 0) {
            return true;
          }
          for (Object condition : orCondition) {
            if (isAccept(changeEvent, (Map) condition, objectName, schemaData)) {
              return true;
            }
          }
          return false;
        } else if (GRAPHQL_FILTER_NOT_OPERATOR.equals(filterKey)) {
          Object notCondition = whereCondition.get(GRAPHQL_FILTER_NOT_OPERATOR);
          return !isAccept(changeEvent, (Map) notCondition, objectName, schemaData);
        }
      }
    }
    return true;
  };

  public static Map fillSelectFields(HashMap<String, Object> selectFieldsMap, Map old_val) {
    Iterator<Map.Entry<String, Object>> iterator = selectFieldsMap.entrySet().iterator();
    HashMap valueMap = new HashMap();
    while (iterator.hasNext()) {
      Map.Entry<String, Object> entry = iterator.next();
      String keyField = entry.getKey();
      Object valObj = entry.getValue();
      if (valObj instanceof Boolean) {
        boolean isFill = (Boolean) valObj;
        if (isFill) {
          valueMap.put(keyField, old_val.get(keyField));
        }
      } else {
        if (null == old_val.get(keyField)) {
          valueMap.put(keyField, null);
        } else {
          valueMap.put(keyField, fillSelectFields(valueMap, (Map) old_val.get(keyField)));
        }
      }
    }
    return valueMap;
  }

  public static void sendChangeEvent(
      HashMap<String, Object> selectFieldsMap,
      Map new_val,
      Map old_val,
      String sessionID,
      String action,
      String subInfoID) {
    HashMap messageMap = new HashMap();
    messageMap.put(SUBSCRIPTION_ACTION, action);
    if (null != new_val) {
      messageMap.put(SUBSCRIPTION_NODE_NEW, fillSelectFields(selectFieldsMap, new_val));
    }
    if (null != old_val) {
      messageMap.put(SUBSCRIPTION_NODE_OLD, fillSelectFields(selectFieldsMap, old_val));
    }
    HashMap resultData = new HashMap();
    resultData.put("data", messageMap);
    MessageServer dataMessage = new MessageServer(resultData, subInfoID, MessageTypes.GQL_DATA);
    if (null != GraphQLWebSocketHandler.sessionhandlers.get(sessionID)) {
      GraphQLWebSocketHandler.sessionhandlers.get(sessionID).outputmessage.onNext(dataMessage);
    } else {
      subscriptionCacheService.endWebSocket(sessionID);
    }
  }
}

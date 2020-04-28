package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.DataSelecter;
import com.easygql.dao.postgres.PostgreSqlTriggerDao;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.EasyGqlUtil.getNowTimeStamp;

@Slf4j
public class TriggerCache {
  private HashMap<String, HashMap<String, HashMap<String,Trigger>>> activeTriggerMap = new HashMap<>();
  private HashMap<String, HashMap<String, HashMap<String,Trigger>>> allTriggerMap = new HashMap<>();
  private HashMap<String, Disposable> triggerSubscribeMap = new HashMap<>();
  private static HashMap<String, HashMap<String, Flowable<Object>>> changeFeedMap = new HashMap<>();
  private static HashMap schemaFilter = new HashMap();
  private static HashMap schemaSelection = new HashMap();
  private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public void init() {
    HashMap eqHashMap = new HashMap();
    eqHashMap.put(GRAPHQL_FILTER_EQ_OPERATOR, SCHEMA_STATUS_RUNNING);
    HashMap statusMap = new HashMap();
    statusMap.put(GRAPHQL_SCHEMASTATUS_FIELDNAME, eqHashMap);
    schemaFilter.put(GRAPHQL_FILTER_FILTER_OPERATOR, statusMap);
    HashMap publishedSchemaObjectField = new HashMap();
    publishedSchemaObjectField.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, 1);
    schemaSelection.put(GRAPHQL_PUBLISHEDSCHEMAID_FIELDNAME, publishedSchemaObjectField);
    HashMap triggerSelection = new HashMap();
    triggerSelection.put(GRAPHQL_ID_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_NAME_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_TYPENAME_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_EVENTTYPE_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_HEADERS_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_OK_STATUS_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_PAYLOADFORMATTER_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_PAYLOADARGS_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_RETRY_TIMES_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_WEBHOOK_URL_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_EXPIREDATE_FIELDNAME, 1);
    triggerSelection.put(GRAPHQL_STARTDATE_FIELDNAME, 1);
    schemaSelection.put(GRAPHQL_TRIGGERS_FIELDNAME, triggerSelection);
    schemaSelection.put(GRAPHQL_ID_FIELDNAME, 1);
    activeTriggerMap.put(GRAPHQL_SCHEMA_ID_DEFAULT,new HashMap<>());
    allTriggerMap.put(GRAPHQL_SCHEMA_ID_DEFAULT,new HashMap<>());
    DataSelecter dataSelecter =
        GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT)
            .getObjectDaoMap()
            .get(GRAPHQL_SCHEMA_TYPENAME)
            .getDataselecter();
    dataSelecter
        .getFilterDocs(schemaFilter, 0, Integer.MAX_VALUE, null, schemaSelection)
        .whenCompleteAsync(
            (schemaListResult, schemaListEx) -> {
              if (null != schemaListEx) {
                if (log.isErrorEnabled()) {
                  log.error("{}", LogData.getErrorLog("E10077", null, (Throwable) schemaListEx));
                }
              } else {
                List<Object> schemaInfoList = ArrayList.class.cast(schemaListResult);
                if (null != schemaInfoList) {
                  for (Object schemaInfo : schemaInfoList) {
                    if (null != schemaInfo) {
                      HashMap schemaInfoMap = HashMap.class.cast(schemaInfo);
                      List<Object> schemaTriggerList =
                          ArrayList.class.cast(schemaInfoMap.get(GRAPHQL_TRIGGERS_FIELDNAME));
                      SchemaData tmpSchemaData =
                          JSONObject.parseObject(
                              JSONObject.toJSONString(schemaInfoMap), SchemaData.class);
                      String schemaID = tmpSchemaData.getSchemaid();
                      if (null != schemaTriggerList && schemaTriggerList.size() > 0) {
                        if (null == allTriggerMap.get(schemaID)) {
                          allTriggerMap.put(schemaID, new HashMap<>());
                        }
                        if (null == activeTriggerMap.get(schemaID)) {
                          activeTriggerMap.put(schemaID,new HashMap<>());
                        }
                        for (Object schemaTrigger : schemaTriggerList) {
                          HashMap triggerMap = HashMap.class.cast(schemaTrigger);
                          Trigger triggerObj = new Trigger();
                          triggerObj.setId(String.class.cast(triggerMap.get(GRAPHQL_ID_FIELDNAME)));
                          triggerObj.setTypename(
                              String.class.cast(triggerMap.get(GRAPHQL_TYPENAME_FIELDNAME)));
                          triggerObj.setOk_status(
                              String.class.cast(triggerMap.get(GRAPHQL_OK_STATUS_FIELDNAME)));
                          triggerObj.setPayloadformatter(
                              String.class.cast(
                                  triggerMap.get(GRAPHQL_PAYLOADFORMATTER_FIELDNAME)));
                          triggerObj.setPayloadargs(
                              ArrayList.class.cast(triggerMap.get(GRAPHQL_PAYLOADARGS_FIELDNAME)));
                          triggerObj.setEventtype(
                              ArrayList.class.cast(triggerMap.get(GRAPHQL_EVENTTYPE_FIELDNAME)));
                          triggerObj.setHeaders(
                              HashMap.class.cast(triggerMap.get(GRAPHQL_HEADERS_FIELDNAME)));
                          triggerObj.setRetry_times(
                              Integer.class.cast(triggerMap.get(GRAPHQL_RETRY_TIMES_FIELDNAME)));
                          triggerObj.setName(
                              String.class.cast(triggerMap.get(GRAPHQL_NAME_FIELDNAME)));
                          triggerObj.setWebhookurl(
                              String.class.cast(triggerMap.get(GRAPHQL_WEBHOOK_URL_FIELDNAME)));
                          triggerObj.setStartdate(
                              String.class.cast(triggerMap.get(GRAPHQL_STARTDATE_FIELDNAME)));
                          triggerObj.setExpiredate(
                              String.class.cast(triggerMap.get(GRAPHQL_EXPIREDATE_FIELDNAME)));
                          String startDate = triggerObj.getStartdate();
                          String expireDate = triggerObj.getExpiredate();
                          LocalDate startTimeDate = null;
                          LocalDate expireTimeDate = null;
                          if (null == activeTriggerMap.get(schemaID).get(triggerObj.getTypename())) {
                            activeTriggerMap.get(tmpSchemaData.getSchemaid()).put(triggerObj.getTypename(), new HashMap<>());
                          }
                          if (null == allTriggerMap.get(schemaID).get(triggerObj.getTypename())) {
                            allTriggerMap.get(tmpSchemaData.getSchemaid()).put(triggerObj.getTypename(), new HashMap<>());
                          }
                          try {
                            startTimeDate = LocalDate.parse(startDate, df);
                          } catch (Exception e) {
                            if (log.isErrorEnabled()) {
                              log.error("{}", LogData.getErrorLog("E10078", triggerMap, e));
                            }
                          }
                          try {
                            expireTimeDate = LocalDate.parse(expireDate, df);
                          } catch (Exception e) {
                            if (log.isErrorEnabled()) {
                              log.error("{}", LogData.getErrorLog("E10078", triggerMap, e));
                            }
                          }
                          if (null != startTimeDate && null != expireTimeDate) {
                            LocalDate now = LocalDate.now();
                            if (!startTimeDate.isAfter(now) && !expireTimeDate.isBefore(now)) {
                              activeTriggerMap.get(schemaID).get(triggerObj.getTypename()).put(triggerObj.getId(),triggerObj);
                              addTrigger(schemaID, tmpSchemaData, triggerObj);
                            }
                            allTriggerMap.get(schemaID).get(triggerObj.getTypename()).put(triggerObj.getId(),triggerObj);
                          }
                        }
                      }
                    }
                  }
                }
                Consumer<Object> mapConsumer =
                    new Consumer<Object>() {
                      @Override
                      public void accept(Object obj) throws Exception {
                        try {
                          JsonArray jsonArray = (JsonArray) obj;
                          String action = String.valueOf(jsonArray.getString(0)).toLowerCase();
                          JSONObject new_val = null;
                          JSONObject old_val = null;
                          if(null!=jsonArray.getJsonObject(1)) {
                            new_val = JSONObject.parseObject(jsonArray.getJsonObject(1).toString());
                          }
                          if(null!=jsonArray.getJsonObject(2)){
                            old_val = JSONObject.parseObject(jsonArray.getJsonObject(2).toString());
                          }
                          Trigger tmpNewTrigger = new Trigger();
                          if (SUBSCRIPTION_TYPE_INSERT.equals(action)) {
                            String triggerID = new_val.getString(GRAPHQL_ID_FIELDNAME);
                            tmpNewTrigger.setId(triggerID);
                            String triggerName = new_val.getString(GRAPHQL_NAME_FIELDNAME);
                            tmpNewTrigger.setName(triggerName);
                            String tmpTypeName = new_val.getString(GRAPHQL_TYPENAME_FIELDNAME);
                            tmpNewTrigger.setTypename(tmpTypeName);
                            HashMap triggerHeaderMap = new HashMap();
                            if (null != new_val.getJSONObject(GRAPHQL_HEADERS_FIELDNAME)) {
                              triggerHeaderMap.putAll(
                                      new_val.getJSONObject(GRAPHQL_HEADERS_FIELDNAME).getInnerMap());
                            }
                            tmpNewTrigger.setHeaders(triggerHeaderMap);
                            List<String> eventList = new ArrayList<>();
                            if (null != new_val.getJSONArray(GRAPHQL_EVENTTYPE_FIELDNAME)) {
                              eventList.addAll(
                                      new_val
                                              .getJSONArray(GRAPHQL_EVENTTYPE_FIELDNAME)
                                              .toJavaList(String.class));
                            }
                            tmpNewTrigger.setEventtype(eventList);
                            String payloadFormatter =
                                    new_val.getString(GRAPHQL_PAYLOADFORMATTER_FIELDNAME);
                            tmpNewTrigger.setPayloadformatter(payloadFormatter);
                            List<String> payLoadArgs = new ArrayList<>();
                            if (null != new_val.getJSONArray(GRAPHQL_PAYLOADARGS_FIELDNAME)) {
                              payLoadArgs.addAll(
                                      new_val
                                              .getJSONArray(GRAPHQL_PAYLOADARGS_FIELDNAME)
                                              .toJavaList(String.class));
                            }
                            tmpNewTrigger.setPayloadargs(payLoadArgs);
                            tmpNewTrigger.setOk_status(
                                    new_val.getString(GRAPHQL_OK_STATUS_FIELDNAME));
                            Integer retryTimes = new_val.getInteger(GRAPHQL_RETRY_TIMES_FIELDNAME);
                            tmpNewTrigger.setRetry_times(retryTimes);
                            tmpNewTrigger.setWebhookurl(
                                    new_val.getString(GRAPHQL_WEBHOOK_URL_FIELDNAME));
                            tmpNewTrigger.setExpiredate(
                                    new_val.getString(GRAPHQL_EXPIREDATE_FIELDNAME));
                            tmpNewTrigger.setStartdate(
                                    new_val.getString(GRAPHQL_STARTDATE_FIELDNAME));
                            String tmpSchemaID = new_val.getString(GRAPHQL_SCHEMAID_FIELDNAME);
                            LocalDate startTimeDate = null;
                            LocalDate expireTimeDate = null;
                            try {
                              startTimeDate = LocalDate.parse(tmpNewTrigger.getStartdate(), df);
                            } catch (Exception e) {
                              if (log.isErrorEnabled()) {
                                log.error(
                                        "{}",
                                        LogData.getErrorLog(
                                                "E10078",
                                                JSONObject.parseObject(
                                                        JSONObject.toJSONString(tmpNewTrigger)),
                                                e));
                              }
                            }
                            try {
                              expireTimeDate = LocalDate.parse(tmpNewTrigger.getExpiredate(), df);
                            } catch (Exception e) {
                              if (log.isErrorEnabled()) {
                                log.error("{}", LogData.getErrorLog("E10078", JSONObject.parseObject(JSONObject.toJSONString(tmpNewTrigger)), e));
                              }
                            }
                            if(null==activeTriggerMap.get(tmpSchemaID)) {
                              activeTriggerMap.put(tmpSchemaID,new HashMap<>());
                            }
                            if(null== allTriggerMap.get(tmpSchemaID)) {
                              allTriggerMap.put(tmpSchemaID,new HashMap<>());
                            }
                            if (null != startTimeDate && null != expireTimeDate) {
                              LocalDate now = LocalDate.now();
                              if (!startTimeDate.isAfter(now) && !expireTimeDate.isBefore(now)) {
                                if(GraphQLCache.ifContain(tmpSchemaID)&&null!=GraphQLCache.getEasyGQL(tmpSchemaID).getSchemaData().getObjectMetaData().get(tmpTypeName)) {
                                  if(null==activeTriggerMap.get(tmpSchemaID).get(tmpTypeName)) {
                                    activeTriggerMap.get(tmpSchemaID).put(tmpTypeName,new HashMap<>());
                                  }
                                  activeTriggerMap.get(tmpSchemaID).get(tmpTypeName).put(tmpNewTrigger.getId(),tmpNewTrigger);
                                  addTrigger(tmpSchemaID, GraphQLCache.getEasyGQL(tmpSchemaID).getSchemaData(), tmpNewTrigger);
                                }
                              }
                              if(null==allTriggerMap.get(tmpSchemaID).get(tmpTypeName)) {
                                allTriggerMap.get(tmpSchemaID).put(tmpTypeName,new HashMap<>());
                              }
                              allTriggerMap.get(tmpSchemaID).get(tmpTypeName).put(tmpNewTrigger.getId(),tmpNewTrigger);
                            }
                          } else if (SUBSCRIPTION_TYPE_REMOVE.equals(action)) {
                            String triggerID = old_val.getString(GRAPHQL_ID_FIELDNAME);
                          } else {
                            String triggerID = new_val.getString(GRAPHQL_ID_FIELDNAME);
                            tmpNewTrigger.setId(triggerID);
                            String triggerName = new_val.getString(GRAPHQL_NAME_FIELDNAME);
                            tmpNewTrigger.setName(triggerName);
                            HashMap triggerHeaderMap = new HashMap();
                            if (null != new_val.getJSONObject(GRAPHQL_HEADERS_FIELDNAME)) {
                              triggerHeaderMap.putAll(
                                      new_val.getJSONObject(GRAPHQL_HEADERS_FIELDNAME).getInnerMap());
                            }
                            tmpNewTrigger.setHeaders(triggerHeaderMap);
                            List<String> eventList = new ArrayList<>();
                            if (null != new_val.getJSONArray(GRAPHQL_EVENTTYPE_FIELDNAME)) {
                              eventList.addAll(
                                      new_val
                                              .getJSONArray(GRAPHQL_EVENTTYPE_FIELDNAME)
                                              .toJavaList(String.class));
                            }
                            tmpNewTrigger.setEventtype(eventList);
                            String payloadFormatter =
                                    new_val.getString(GRAPHQL_PAYLOADFORMATTER_FIELDNAME);
                            tmpNewTrigger.setPayloadformatter(payloadFormatter);
                            List<String> payLoadArgs = new ArrayList<>();
                            if (null != new_val.getJSONArray(GRAPHQL_PAYLOADARGS_FIELDNAME)) {
                              payLoadArgs.addAll(
                                      new_val
                                              .getJSONArray(GRAPHQL_PAYLOADARGS_FIELDNAME)
                                              .toJavaList(String.class));
                            }
                            tmpNewTrigger.setPayloadargs(payLoadArgs);
                            tmpNewTrigger.setOk_status(
                                    new_val.getString(GRAPHQL_OK_STATUS_FIELDNAME));
                            Integer retryTimes = new_val.getInteger(GRAPHQL_RETRY_TIMES_FIELDNAME);
                            tmpNewTrigger.setRetry_times(retryTimes);
                            List<String> watchFieldsList = new ArrayList<>();
                            if (null == new_val.getJSONArray(GRAPHQL_WATCHFIELDS_FIELDNAME)) {
                              watchFieldsList.addAll(
                                      new_val
                                              .getJSONArray(GRAPHQL_WATCHFIELDS_FIELDNAME)
                                              .toJavaList(String.class));
                            }
                            tmpNewTrigger.setWebhookurl(
                                    new_val.getString(GRAPHQL_WEBHOOK_URL_FIELDNAME));
                            tmpNewTrigger.setExpiredate(
                                    new_val.getString(GRAPHQL_EXPIREDATE_FIELDNAME));
                            tmpNewTrigger.setStartdate(
                                    new_val.getString(GRAPHQL_STARTDATE_FIELDNAME));
                          }
                        } catch (Exception e) {
                          if(log.isErrorEnabled()) {
                            HashMap errorMap = new HashMap();
                            errorMap.put(GRAPHQL_EVENT_ARGUMENT,obj);
                            log.error("{}",LogData.getErrorLog("E10097",errorMap,e));
                          }
                        }
                      }
                    };
                PostgreSqlTriggerDao postgreSqlTriggerDao = new PostgreSqlTriggerDao();
                postgreSqlTriggerDao.init(
                    GraphQLCache.getEasyGQL(GRAPHQL_SCHEMA_ID_DEFAULT).getSchemaData(),
                    GRAPHQL_SCHEMA_ID_DEFAULT);
                Observable<Object> changeFeedsObservable =
                    Observable.create(
                        emitter -> {
                          postgreSqlTriggerDao.ListenTrigger(GRAPHQL_TRIGGER_TYPENAME, emitter);
                        });
                Observable<Object> resultObservable = changeFeedsObservable.share();
                if (null == changeFeedMap.get(GRAPHQL_SCHEMA_ID_DEFAULT)) {
                  changeFeedMap.put(
                      GRAPHQL_SCHEMA_ID_DEFAULT, new HashMap<String, Flowable<Object>>());
                }
                changeFeedMap
                    .get(GRAPHQL_SCHEMA_ID_DEFAULT)
                    .put(
                        GRAPHQL_TRIGGER_TYPENAME,
                        resultObservable.toFlowable(BackpressureStrategy.BUFFER));
                changeFeedMap
                    .get(GRAPHQL_SCHEMA_ID_DEFAULT)
                    .get(GRAPHQL_TRIGGER_TYPENAME)
                    .subscribe(mapConsumer);
              }
            });
  }

  public static void addTrigger(String schemaID, SchemaData schemaData, Trigger triggerObj) {
    CompletableFuture.runAsync(
        () -> {
          if (null == changeFeedMap.get(schemaID)) {
            changeFeedMap.put(schemaID, new HashMap<String, Flowable<Object>>());
          }
          String typeName = triggerObj.getTypename();
          List<String> tmpEventTypeList = triggerObj.getEventtype();
          List<String> eventTypeList = new ArrayList<>();
          for (String eventAction :tmpEventTypeList ) {
              eventTypeList.add(eventAction.toLowerCase());
          }
          Consumer<Object> mapConsumer =
              new Consumer<Object>() {
                @Override
                public void accept(Object obj) throws Exception {
                  JsonArray jsonArray = (JsonArray) obj;
                  String action = String.valueOf(jsonArray.getString(0)).toLowerCase();
                  if (eventTypeList.contains(action)) {
                    String returnVal = null;
                    JSONObject new_val = null;
                    JSONObject old_val = null;
                    if(null!=jsonArray.getJsonObject(1)) {
                      new_val = JSONObject.parseObject(jsonArray.getJsonObject(1).toString());
                    }
                    if(null!=jsonArray.getJsonObject(2)){
                      old_val = JSONObject.parseObject(jsonArray.getJsonObject(2).toString());
                    }
                    if (SUBSCRIPTION_TYPE_INSERT.equals(action)) {
                      returnVal =
                          parsePayLoad(
                              triggerObj.getPayloadformatter(),
                              triggerObj.getPayloadargs(),
                              action,
                              new_val,
                              null);
                    } else if (SUBSCRIPTION_TYPE_REMOVE.equals(action)) {
                      returnVal =
                          parsePayLoad(
                              triggerObj.getPayloadformatter(),
                              triggerObj.getPayloadargs(),
                              action,
                              null,
                              old_val);
                    } else {
                      returnVal =
                          parsePayLoad(
                              triggerObj.getPayloadformatter(),
                              triggerObj.getPayloadargs(),
                              action,
                              new_val,
                              old_val);
                    }
                    final String returnValue = returnVal;
                    JSONObject tmpOldVal = old_val;
                    JSONObject tmpNewVal = new_val;
                    TriggerSender.sendEvent(returnVal, triggerObj)
                        .whenCompleteAsync(
                            (result, resultEx) -> {
                              Boolean isSucceed = false;
                              HashMap triggerHistoryMap = new HashMap();
                              triggerHistoryMap.put(GRAPHQL_ID_FIELDNAME, IDTools.getID());
                              triggerHistoryMap.put(GRAPHQL_TRIGGER_FIELDNAME, triggerObj.getId());
                              triggerHistoryMap.put(GRAPHQL_EVENTTYPE_FIELDNAME, action);
                              triggerHistoryMap.put(VALUETYPE_OLDVALUE, tmpOldVal);
                              triggerHistoryMap.put(VALUETYPE_NEWVALUE, tmpNewVal);
                              triggerHistoryMap.put(
                                  GRAPHQL_TRIGGER_TIME_FIELDNAME, getNowTimeStamp());
                              triggerHistoryMap.put(GRAPHQL_PAYLOAD_FIELDNAME, returnValue);
                              if (null == resultEx && result) {
                                isSucceed=true;
                              }
                              GraphQLCache.getEasyGQL(schemaID)
                                      .getTriggerDao().AddTriggerEvent(triggerObj.getId(),action,tmpOldVal,tmpNewVal,isSucceed);
                            });
                  }
                }
              };
          if (null == changeFeedMap.get(schemaID).get(typeName)) {
            PostgreSqlTriggerDao postgreSqlTriggerDao = new PostgreSqlTriggerDao();
            postgreSqlTriggerDao.init(schemaData, schemaID);
            Observable<Object> changeFeedsObservable =
                Observable.create(
                    emitter -> {
                      postgreSqlTriggerDao.ListenTrigger(typeName, emitter);
                    });
            Observable<Object> resultObservable = changeFeedsObservable.share();
            changeFeedMap
                .get(schemaID)
                .put(typeName, resultObservable.toFlowable(BackpressureStrategy.BUFFER));
          }
          changeFeedMap.get(schemaID).get(typeName).subscribe(mapConsumer);
        });
  }

  public static String parsePayLoad(
      String payLoadFormatter,
      List<String> payLoadArgs,
      String action,
      JSONObject new_val,
      JSONObject old_val) {
    try {
      if (null == payLoadFormatter || payLoadFormatter.trim().equals("")) {
        HashMap payLoadResult = new HashMap();
        payLoadResult.put(GRAPHQL_ACTION_FIELDNAME, action);
        payLoadResult.put(SUBSCRIPTION_NODE_OLD, old_val);
        payLoadResult.put(SUBSCRIPTION_NODE_NEW, new_val);
        return JSONObject.toJSONString(payLoadResult);
      } else if (null == payLoadArgs || payLoadArgs.size() == 0) {
        return payLoadFormatter;
      } else {
        List<String> argString = new ArrayList<>();
        for (String arg : payLoadArgs) {
          arg = arg.trim();
          if (arg.equals(GRAPHQL_ACTION_FIELDNAME)) {
            argString.add(action);
          } else {
            if (arg.startsWith("new.")) {
              String key = arg.substring(4);
              String argVal = String.class.cast(new_val.get(key));
              argString.add(argVal);
            } else if (arg.startsWith("old.")) {
              String key = arg.substring(4);
              String argVal = String.class.cast(old_val.get(key));
              argString.add(argVal);
            } else {
              argString.add(arg);
            }
          }
        }
        return String.format(payLoadFormatter, argString.toArray());
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_PAYLOADFORMATTER_FIELDNAME, payLoadFormatter);
        errorMap.put(GRAPHQL_PAYLOADARGS_FIELDNAME, payLoadArgs);
        errorMap.put(GRAPHQL_ACTION_FIELDNAME, action);
        errorMap.put(SUBSCRIPTION_NODE_OLD, old_val);
        errorMap.put(SUBSCRIPTION_NODE_NEW, new_val);
        log.error("{}", LogData.getErrorLog("E10082", errorMap, e));
      }
      HashMap payLoadResult = new HashMap();
      payLoadResult.put(GRAPHQL_ACTION_FIELDNAME, action);
      payLoadResult.put(SUBSCRIPTION_NODE_OLD, old_val);
      payLoadResult.put(SUBSCRIPTION_NODE_NEW, new_val);
      return JSONObject.toJSONString(payLoadResult);
    }
  }
}

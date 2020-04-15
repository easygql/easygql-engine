package com.easygql.handler;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.BusinessException;
import com.easygql.component.SubscriptionCacheService;
import com.easygql.util.*;
import graphql.ExecutionInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.UnicastProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
;import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author ：fenyorome
 * @date ：Created in 2019/3/7/007 13:35
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
@Slf4j
public class WebSocketSessionHandler {
  public UnicastProcessor<MessageServer> outputmessage;
  private WebSocketSession session;
  private boolean isConnected = false;
  private String schemaID;
  private HashMap<String, Object> userInfo = null;
  private String subInfoID=null;

  private MessageClient toMessageClient(String json) {
    return JSONObject.parseObject(json, MessageClient.class);
  }

  /** */
  private void unAuthorized() {
    String[] errors = {"connection prohibited"};
    HashMap<String, Object> errorMap = new HashMap<>();
    errorMap.put("errors", errors);
    MessageServer messageServer =
        new MessageServer(errorMap, null, MessageTypes.GQL_CONNECTION_ERROR);
    outputmessage.onNext(messageServer);
    doClose();
  }

  /**
   * @param e
   * @return
   */
  private Object getError(Throwable e) {
    HashMap<String, Object> errorMap = new HashMap<>();
    ErrorClient[] errors = new ErrorClient[e.getStackTrace().length];
    for (int i = 0; i < e.getStackTrace().length; i++) {
      errors[i] = new ErrorClient(e.getStackTrace()[i].toString());
    }
    errorMap.put("errors", errors);
    return errorMap;
  }

  /** */
  private void unSubscribe() {
    if (null != subInfoID) {
      SubscriptionCacheService.subscriptionCacheService.endWebSocketSub(subInfoID,session.getId());
    }
    subInfoID = null;
  }

  /**
   * @param webSocketSession
   * @param schemaID
   */
  public WebSocketSessionHandler(WebSocketSession webSocketSession, String schemaID) {
    outputmessage = UnicastProcessor.create();
    session = webSocketSession;
    this.schemaID = schemaID;
  }

  /** @param message */
  // 处理消息请求
  public void onReceive(String message) {
    MessageClient messageClient = toMessageClient(message);
//    if (!messageClient.type.equals(MessageTypes.GQL_CONNECTION_INIT) && (null == userInfo)) {
//      unAuthorized();
//      return;
//    }
    switch (messageClient.type) {
      case MessageTypes.GQL_CONNECTION_INIT:
//        this.userInfo = new HashMap();
//        if (null == messageClient.payload.get("Authorization") + ""
//            || !JwtUtil.checkJWT(messageClient.payload.get("Authorization") + "")) {
//          this.userInfo.put(GRAPHQL_ID_FIELDNAME, UUID.randomUUID().toString());
//          this.userInfo.put("username", "guest");
//          this.userInfo.put("role", "Guest");
//        } else {
//          this.userInfo =
//              JSONObject.parseObject(
//                  JSONObject.toJSONString(
//                      JwtUtil.parser(messageClient.payload.get("Authorization") + "")),
//                  HashMap.class);
//        }
        MessageServer messageServer =
            new MessageServer(null, null, MessageTypes.GQL_CONNECTION_ACK);
        outputmessage.onNext(messageServer);
        break;
      case MessageTypes.GQL_START:
        GraphQLParameters graphQLParameters = getGraphQLParams(messageClient.payload);
        switch (GraphQLUtil.getQuerytype(graphQLParameters.getQuery())) {
          case QUERY:
          case MUTATION:
            throw new RuntimeException("Web Socket只支持订阅接口！！");
          case SUBSCRIPTION:
            this.unSubscribe();
            this.subInfoID = messageClient.id;
            doSubscription(graphQLParameters, subInfoID).whenComplete((subResult,subEx)->{
              if(null!=subEx) {
                String[] errors = {"invalid Subscription"};
                HashMap<String, Object> errorMap = new HashMap<>();
                errorMap.put("errors",errors );
                MessageServer tmpMessage =
                        new MessageServer(errorMap, subInfoID, MessageTypes.GQL_ERROR);
                outputmessage.onNext(tmpMessage);
              }
            });
            break;
          case NOTQUERY:
            String[] errors = {"invalid operation type"};
            HashMap<String, Object> errorMap = new HashMap<>();
            errorMap.put("errors", errors);
            MessageServer errormessage =
                new MessageServer(errorMap, messageClient.id, MessageTypes.GQL_ERROR);
            outputmessage.onNext(errormessage);
            break;
        }
        break;
      case MessageTypes.GQL_STOP:
        this.unSubscribe();
        break;
      case MessageTypes.GQL_CONNECTION_TERMINATE:
        doClose();
        break;
      default:
        String[] errors = {"invalid message type"};
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put("errors", errors);
        MessageServer errormessage =
            new MessageServer(errorMap, messageClient.id, MessageTypes.GQL_ERROR);
        outputmessage.onNext(errormessage);
    }
  }

  /** */
  public void onSubscribe() {
    // 进行初始化
    if(log.isInfoEnabled()) {
      log.info("{}",LogData.getInfoLog("I10001",null));
    }
    isConnected = true;
  }

  /** */
  public void onComplete() {
    // 释放相关资源
    if(log.isInfoEnabled()) {
      HashMap infoMap = new HashMap();
      infoMap.put(GRAPHQL_SESSION_ID_ARGUMENT,session.getId());
      log.info("{}",LogData.getInfoLog("I10002",infoMap));
    }
    outputmessage.onComplete();
    doClose();
  }

  /**
   * @param payload
   * @return
   */
  private GraphQLParameters getGraphQLParams(HashMap<String, Object> payload) {
    String queryInfo = String.class.cast(payload.get("query"));
    String operationName = String.class.cast(payload.get("operationName"));
    Map<String, Object> variables = Map.class.cast(payload.get("variables"));
    return new GraphQLParameters(queryInfo, operationName, variables);
  }

  /**
   * 出现问题,准备关闭连接并释放资源
   *
   * @param error
   */
  public void onError(Throwable error) {
    if (error instanceof java.io.IOException) {
      log.info(error.getMessage());
      if(log.isInfoEnabled()) {
        HashMap infoMap = new HashMap();
        infoMap.put(GRAPHQL_SESSION_ID_ARGUMENT,session.getId());
        log.info("{}",LogData.getInfoLog("I10003",infoMap));
      }
    } else {
      if(log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_SESSION_ID_ARGUMENT,session.getId());
        log.error("{}",LogData.getErrorLog("E10026",errorMap,error));
      }
    }
    MessageServer messageServer = new MessageServer(getError(error), null, MessageTypes.GQL_ERROR);
    outputmessage.onNext(messageServer);
  }

  /** */
  public void doClose() {
    if (!isConnected) {
      return;
    }
    if(log.isInfoEnabled()) {
      HashMap infoMap = new HashMap();
      infoMap.put(GRAPHQL_SESSION_ID_ARGUMENT,session.getId());
      log.info("{}",LogData.getInfoLog("I10002",infoMap));
    }
    SubscriptionCacheService.subscriptionCacheService.endWebSocket(session.getId());
    session.close();
    isConnected = false;
  }

  private CompletableFuture<Void> doSubscription(
      GraphQLParameters graphQLParameters, String subInfoID) {
    CompletableFuture<Void> future = new CompletableFuture<>();
     CompletableFuture.runAsync(
        () -> {
          try {
            HashMap<String, Object> contextHashMap = new HashMap<>();
            if(null==graphQLParameters.getVariables()) {
              graphQLParameters.setVariables(new HashMap<>());
            }
            contextHashMap.put(GRAPHQL_SUBSCRIPTION_ID_ARGUMENT, subInfoID);
            contextHashMap.put(GRAPHQL_SESSION_ID_ARGUMENT, session.getId());
            ExecutionInput executionInput =
                ExecutionInput.newExecutionInput()
                    .query(graphQLParameters.getQuery())
                    .operationName(graphQLParameters.getOperationName())
                    .context(contextHashMap)
                    .variables(graphQLParameters.getVariables())
                    .build();
            GraphQLCache.getGraphql(schemaID).whenComplete((graphQL, throwable) -> {
              if(null!=throwable) {
                if(log.isErrorEnabled()) {
                  HashMap errorMap = new HashMap();
                  errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME,schemaID);
                  log.error("{}",LogData.getErrorLog("E10013",errorMap,throwable));
                }
                future.completeExceptionally(new BusinessException("E10013"));
              } else {
                graphQL.executeAsync(executionInput).whenComplete((executionResult, executionEx) ->{
                  if(null!=executionEx) {
                    if(log.isErrorEnabled()) {
                      log.error("{}",LogData.getErrorLog("E10096",new HashMap<>(),executionEx));
                    } else if(null!=executionResult.getErrors() && executionResult.getErrors().size()>0) {
                      if(log.isErrorEnabled()) {
                        log.error("{}",LogData.getErrorMessage("E10096"));
                      }
                      future.completeExceptionally(new BusinessException("E10096"));
                    } else {
                      future.complete(null);
                    }
                  }
                });
              }
            });
          } catch (Exception e) {
            if(log.isErrorEnabled()) {
              HashMap errorMap = new HashMap();
              errorMap.put("subscription",graphQLParameters);
              log.error("{}",LogData.getErrorLog("E10026",errorMap,e));
            }
            future.completeExceptionally(e);
          }
        });
     return future;
  }
}

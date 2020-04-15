package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.dao.DataSelecter;
import com.easygql.exception.BusinessException;
import com.easygql.thirdapis.SchemaStart;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.easygql.component.ConfigurationProperties.*;

/** @Author: fenyorome
 *  @Date: 2019/1/2/002 21:01
 *  */
@Slf4j
public class GraphQLCache {
  private static ConcurrentHashMap<String, EasyGQL> graphqlCache =
      new ConcurrentHashMap<String, EasyGQL>();
  private static ConcurrentHashMap<String,List<CompletableFuture<GraphQL>>> pendingCache = new ConcurrentHashMap<>();
  private static HashMap publishedSchemaSelecter = new HashMap();
  private static DataSelecter schemaSelecter = null;

  static {
    publishedSchemaSelecter.put(GRAPHQL_ID_FIELDNAME, 1);
    HashMap publishedSchema = new HashMap();
    publishedSchemaSelecter.put(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME, publishedSchema);
    publishedSchema.put(GRAPHQL_ID_FIELDNAME, 1);
    publishedSchema.put(GRAPHQL_SCHEMAOBJECT_FIELDNAME, 1);
  }

  private static SchemaStart schemaStart = new SchemaStart();

  public static void clear() {
    graphqlCache.clear();
  }

  public static void addGraphQL(String schemaID, EasyGQL graphQL) {
    graphqlCache.put(schemaID, graphQL);
  }

  public static void init() {
    schemaSelecter =
        graphqlCache
            .get(GRAPHQL_SCHEMA_ID_DEFAULT)
            .getObjectDaoMap()
            .get(GRAPHQL_SCHEMA_TYPENAME)
            .getDataselecter();
  }

  /**
   * @param schemaID
   * @return
   */
  public static CompletableFuture<GraphQL> getGraphql(String schemaID) {
    CompletableFuture<GraphQL> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          EasyGQL easyGQL = graphqlCache.get(schemaID);
          if (null != easyGQL) {
            if (null != easyGQL.getGraphQL()) {
              future.complete(easyGQL.getGraphQL());
            } else {
              future.completeExceptionally(new BusinessException("E10086"));
            }
          } else {
            try {
              List<CompletableFuture<GraphQL>> tmpList =pendingCache.putIfAbsent(schemaID,new ArrayList<>());
              if(null == tmpList) {
                pendingCache.get(schemaID).add(future);
                HashMap eqMap = new HashMap();
                eqMap.put(GRAPHQL_FILTER_EQ_OPERATOR,schemaID);
                HashMap idMap = new HashMap();
                idMap.put(GRAPHQL_ID_FIELDNAME,eqMap);
                HashMap filterMap = new HashMap();
                filterMap.put(GRAPHQL_FILTER_FILTER_OPERATOR,idMap);
                schemaSelecter
                        .getSingleDoc(filterMap, publishedSchemaSelecter)
                        .whenCompleteAsync(
                                (schemaInfo, ex) -> {
                                  if (null != ex || null == schemaInfo) {
                                    future.completeExceptionally(new BusinessException("E10086"));
                                  } else {
                                    try {
                                      HashMap schemaInfoMap = (HashMap) schemaInfo;
                                      HashMap publishedSchemaInfo =
                                              (HashMap) schemaInfoMap.get(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME);
                                      if (null == publishedSchemaInfo) {
                                        future.completeExceptionally(new BusinessException("E10087"));
                                      } else {
                                        Object schemaDataJson =
                                                publishedSchemaInfo.get(GRAPHQL_SCHEMAOBJECT_FIELDNAME);
                                        if (null == schemaDataJson) {
                                          future.completeExceptionally(new BusinessException("E10088"));
                                        } else {
                                          SchemaData schemaData =
                                                  JSONObject.parseObject(
                                                          JSONObject.toJSONString(schemaDataJson), SchemaData.class);
                                          schemaStart
                                                  .startSchema(schemaData, schemaID)
                                                  .whenComplete(
                                                          (result, startEx) -> {
                                                            if (null != startEx) {
                                                              List<CompletableFuture<GraphQL>> requestList = pendingCache.remove(schemaID);
                                                              requestList.forEach(request->{
                                                                request.completeExceptionally(startEx);
                                                              });
                                                            } else {
                                                              List<CompletableFuture<GraphQL>> requestList = pendingCache.remove(schemaID);
                                                              requestList.forEach(request->{
                                                                request.complete(graphqlCache.get(schemaID).getGraphQL());
                                                              });
                                                            }
                                                          });
                                        }
                                      }
                                    } catch (Exception e) {
                                      future.completeExceptionally(e);
                                    }
                                  }
                                });
              } else {
                tmpList.add(future);
              }
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          }
        });
    return future;
  }

  public static EasyGQL getEasyGQL(String schemaID) {
    if(null==graphqlCache.get(schemaID)) {
      try {
        getGraphql(schemaID).get();
      } catch ( Exception e) {
        if(log.isErrorEnabled()) {
          HashMap errorMap = new HashMap();
          errorMap.put(GRAPHQL_SCHEMAID_FIELDNAME,schemaID);
          log.error("{}",LogData.getErrorLog("E10002",errorMap,e));
        }
      }
    }
   return graphqlCache.get(schemaID);
  }

  public static boolean ifContain(String schemaID) {
    return graphqlCache.keySet().contains(schemaID);
  }

  public static void removeAll(List<String> schemaIDList) {
    for (String key : schemaIDList) {
      graphqlCache.remove(key);
    }
  }

  public static void remove(String schemaID) {
    graphqlCache.remove(schemaID);
  }
}

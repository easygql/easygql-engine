package com.easygql.service;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.NotAuthorizedException;
import com.easygql.util.GraphQLCache;
import com.easygql.util.GraphQLParameters;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class QueryService {

    /**
     * @param schemaid
     * @param parameters
     * @param userObject
     * @return
     */
    public static CompletableFuture<Object> query(String schemaid, String parameters, HashMap userObject) {
        parameters = parameters.trim();
        HashMap<String, Object> contextHashMap = new HashMap<>();
        contextHashMap.put("logineduser", userObject);
        if (!parameters.startsWith("[")) {
            GraphQLParameters graphQLParameters = JSONObject.parseObject(parameters, GraphQLParameters.class);
            if (null == graphQLParameters.getVariables()) {
                graphQLParameters.setVariables(new HashMap<>());
            }
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(graphQLParameters.getQuery())
                    .operationName(graphQLParameters.getOperationName())
                    .variables(graphQLParameters.getVariables())
                    .context(contextHashMap)
                    .build();
            return GraphQLCache.getGraphql(schemaid).thenCompose(graphQL -> graphQL.executeAsync(executionInput)).thenApply(it -> {
                if (null == it.getErrors() || it.getErrors().size() == 0) {
                    return it.toSpecification();
                } else {
                    for (GraphQLError graphqlerror : it.getErrors()) {
                        if (graphqlerror instanceof ExceptionWhileDataFetching) {
                            ExceptionWhileDataFetching exceptionWhileDataFetching = (ExceptionWhileDataFetching) graphqlerror;
                            if (exceptionWhileDataFetching.getException().equals(NotAuthorizedException.notAuthorizedException)) {
                                throw NotAuthorizedException.notAuthorizedException;
                            }
                        }
                    }
                }
                return it.toSpecification();
            });
        } else {
            List<GraphQLParameters> graphQLParametersList = JSONObject.parseArray(parameters, GraphQLParameters.class);
            List<CompletableFuture<ExecutionResult>> completableresult = graphQLParametersList.stream().map(graphQLParameter -> {
                if (null == graphQLParameter.getVariables()) {
                    graphQLParameter.setVariables(new HashMap<>());
                }
                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(graphQLParameter.getQuery())
                        .operationName(graphQLParameter.getOperationName())
                        .variables(graphQLParameter.getVariables())
                        .context(contextHashMap)
                        .build();
                return GraphQLCache.getGraphql(schemaid).thenCompose(graphql -> graphql.executeAsync(executionInput));
            }).collect(Collectors.toList());
            CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(completableresult.toArray(new CompletableFuture[completableresult.size()]));
            return allFuturesResult.thenApplyAsync(v -> {
                return completableresult.stream().map(CompletableFuture::join).map(result -> {
                    return result.toSpecification();
                }).collect(Collectors.toList());
            });
        }
    }

    /**
     * @param schemaid
     * @param graphQLParameters
     * @param userObject
     * @return
     */
    public static CompletableFuture<Map<String, Object>> query(String schemaid, GraphQLParameters graphQLParameters, HashMap userObject) {
        HashMap<String, Object> contextHashMap = new HashMap<>();
        contextHashMap.put("logineduser", userObject);
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(graphQLParameters.getQuery())
                .operationName(graphQLParameters.getOperationName())
                .variables(graphQLParameters.getVariables())
                .context(contextHashMap)
                .build();
        return GraphQLCache.getGraphql(schemaid).thenCompose(graphQL -> graphQL.executeAsync(executionInput)).thenApply(it -> it.toSpecification());
    }
}

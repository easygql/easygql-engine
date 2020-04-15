package com.easygql.util;


import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * @Author: fenyorome
 * @Date: 2019/1/6/006 19:03
 */
public class GraphQLParameters {

    private String query;
    private String operationName = null;
    private Map<String, Object> variables = null;

    public GraphQLParameters(String query, String operationName, Map<String, Object> variables) {
        this.query = query;
        this.operationName = operationName;
        this.variables = variables;
        if (null != this.query) {
            this.query = this.query.trim();
        }
        if (null != operationName) {
            this.operationName = this.operationName.trim();
        }
    }


    public GraphQLParameters(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationname(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public static GraphQLParameters from(String queryMessage) {
        GraphQLParameters graphQLParameters = JSONObject.parseObject(queryMessage, GraphQLParameters.class);
        return graphQLParameters;
    }

    private static Map<String, Object> getVariables(Object variables) {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.class.cast(k), v));
            return vars;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(variables), Map.class);
    }

    public String getVariableString() {
        if(null==variables) {
            return null;
        }
        return JSONObject.toJSONString(variables);
    }

    @Override
    public boolean equals(Object parameters) {
        if (null == parameters) {
            return false;
        }
        if (parameters instanceof GraphQLParameters) {
            GraphQLParameters graphQLParameters = (GraphQLParameters) parameters;
            if (null == graphQLParameters.operationName && null != this.operationName) {
                return false;
            }
            if ((null == graphQLParameters.operationName || null == this.operationName) && this.operationName != graphQLParameters.operationName) {
                return false;
            }
            if ((null == graphQLParameters.query || null == this.query) && graphQLParameters.query != this.query) {
                return false;
            }
            if ((null == variables || null == this.variables) && graphQLParameters.variables != this.variables) {
                return false;
            }
            if (null != graphQLParameters.operationName && !graphQLParameters.operationName.equals(this.operationName)) {
                return false;
            }
            if (null != graphQLParameters.query && !graphQLParameters.query.equals(this.query)) {
                return false;
            }
            if ((null == graphQLParameters.variables || null != this.variables) && graphQLParameters.variables != this.variables) {
                return false;
            }
            if (graphQLParameters.variables != null && graphQLParameters.variables.size() != this.variables.size()) {
                return false;
            }
            if (null != graphQLParameters.variables) {
                return graphQLParameters.variables.entrySet().stream().allMatch(e -> {
                    return EqualsBuilder.reflectionEquals(e.getValue(), this.variables.get(e.getKey()));
                });
            }
            return true;
        }
        return false;
    }
}

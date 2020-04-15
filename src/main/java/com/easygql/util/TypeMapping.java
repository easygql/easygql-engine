package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.BusinessException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019/11/1 10:33
 */
@Slf4j
public class TypeMapping {
    //postgresql mapping
    private final HashMap<String, String> postgresqlMap;
    private final HashMap<String,Class> graphqlTypeMap;
    private static volatile  TypeMapping instance;
    /**
     *
     */
    private TypeMapping() {
        postgresqlMap = new HashMap();
        postgresqlMap.put(GRAPHQL_INT_TYPENAME,"integer");
        postgresqlMap.put(GRAPHQL_LONG_TYPENAME,"bigint");
        postgresqlMap.put(GRAPHQL_SHORT_TYPENAME,"smallint");
        postgresqlMap.put(GRAPHQL_FLOAT_TYPENAME,"real");
        postgresqlMap.put(GRAPHQL_BIGDECIMAL_TYPENAME,"numeric");
        postgresqlMap.put(GRAPHQL_ID_TYPENAME,"character(32)");
        postgresqlMap.put(GRAPHQL_BOOLEAN_TYPENAME,"boolean");
        postgresqlMap.put(GRAPHQL_STRING_TYPENAME,"varchar");
        postgresqlMap.put(GRAPHQL_BYTE_TYPENAME,"byte");
        postgresqlMap.put(GRAPHQL_CHAR_TYPENAME,"char(1)");
        postgresqlMap.put(GRAPHQL_OBJECT_TYPENAME,"json");
        postgresqlMap.put(GRAPHQL_JSON_TYPENAME,"json");
        postgresqlMap.put(GRAPHQL_DATE_TYPENAME,"date");
        postgresqlMap.put(GRAPHQL_DATETIME_TYPENAME,"timestamp");
        postgresqlMap.put(GRAPHQL_TIME_TYPENAME,"time");
        postgresqlMap.put(GRAPHQL_CREATEDAT_TYPENAME,"timestamp");
        postgresqlMap.put(GRAPHQL_LASTUPDATE_TYPENAME,"timestamp");
        graphqlTypeMap = new HashMap<>();
        graphqlTypeMap.put(GRAPHQL_INT_TYPENAME,Integer.class);
        graphqlTypeMap.put(GRAPHQL_LONG_TYPENAME,Long.class);
        graphqlTypeMap.put(GRAPHQL_SHORT_TYPENAME,Short.class);
        graphqlTypeMap.put(GRAPHQL_FLOAT_TYPENAME,Float.class);
        graphqlTypeMap.put(GRAPHQL_ID_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_BOOLEAN_TYPENAME,Boolean.class);
        graphqlTypeMap.put(GRAPHQL_STRING_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_BYTE_TYPENAME,Byte.class);
        graphqlTypeMap.put(GRAPHQL_CHAR_TYPENAME,char.class);
        graphqlTypeMap.put(GRAPHQL_OBJECT_TYPENAME, JSONObject.class);
        graphqlTypeMap.put(GRAPHQL_JSON_TYPENAME,JSONObject.class);
        graphqlTypeMap.put(GRAPHQL_DATE_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_DATETIME_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_TIME_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_CREATEDAT_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_LASTUPDATE_TYPENAME,String.class);
        graphqlTypeMap.put(GRAPHQL_BIGDECIMAL_TYPENAME, BigDecimal.class);
    }

    /**
     * 获取实例
     * @return
     */
    public static TypeMapping getInstance() {
        if(null==instance) {
            synchronized (TypeMapping.class) {
                if(null==instance) {
                    instance=new TypeMapping();
                }
            }
        }
        return instance;
    }

    /**
     * 根据GraphQL 数据类型返回对应数据库字段类型
     * @param databaseKind
     * @param graphqlTypeName
     * @return
     */
    public static String getTypeName(@NonNull String databaseKind, @NonNull String graphqlTypeName) {
        switch (databaseKind) {
            case DATABASE_KIND_MONGODB:
            case DATABASE_KIND_MYSQL:
                return null;
            case DATABASE_KIND_POSTGRES:
                return getInstance().postgresqlMap.getOrDefault(graphqlTypeName,"varchar");
            default:
                throw new BusinessException("E10047");
        }
    }

    /**
     * 用于获取字段对应的Java类型
     * @param typeName
     * @return
     */
    public static Class getClassNameInfo(@NonNull  String typeName) {
        return instance.graphqlTypeMap.getOrDefault(typeName,String.class);
    }

}

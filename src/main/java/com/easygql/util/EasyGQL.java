package com.easygql.util;

import graphql.GraphQL;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Data
public class EasyGQL {
    private GraphQL graphQL;
    private SchemaData schemaData;
    private HashMap<String,ObjectDao> objectDaoMap;
}

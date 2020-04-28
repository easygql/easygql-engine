package com.easygql.util;

import com.easygql.dao.TriggerDao;
import graphql.GraphQL;
import lombok.Data;

import java.util.HashMap;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Data
public class EasyGQL {
    private GraphQL graphQL;
    private SchemaData schemaData;
    private HashMap<String,ObjectDao> objectDaoMap;
    private TriggerDao triggerDao;
}

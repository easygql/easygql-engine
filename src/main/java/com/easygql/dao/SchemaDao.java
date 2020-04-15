package com.easygql.dao;

import com.easygql.util.DataSourceInfo;
import com.easygql.util.SchemaData;
import com.easygql.util.SchemaObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author guofen
 * @date 2019/12/7 19:54
 */
public interface SchemaDao {
    //获取当前未发布的Schema内容
    CompletableFuture<Boolean>   schemaInitial(SchemaData schemadata);
    CompletableFuture<Boolean> ifDataBaseExists(DataSourceInfo dataSourceInfo);
    CompletableFuture<Boolean> createDataBase(DataSourceInfo dataSourceInfo, String databaseName);
}

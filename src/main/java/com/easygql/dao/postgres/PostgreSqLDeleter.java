package com.easygql.dao.postgres;

import com.easygql.dao.DataDeleter;
import com.easygql.exception.BusinessException;
import com.easygql.util.LogData;
import com.easygql.util.PostgreSQLPoolCache;
import com.easygql.util.SchemaData;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_AFFECTEDROW_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_WHERE_ARGUMENT;


/**
 * @author guofen
 * @date 2019/11/3 14:58
 */
@Slf4j
public class PostgreSqLDeleter implements DataDeleter {
    private SchemaData schemaData;
    private String schemaID;
    private String objectName;

    @Override
    public void Init(String objectName, SchemaData schemaData, String schemaID) {
        this.objectName = objectName;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
    }

    /**
     * 基于PostgreSQL进行数据删除
     *
     * @param whereinput
     * @return
     */
    @Override
    public CompletableFuture<Map> deleteDocs(Object whereinput) {
        CompletableFuture<Map> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                HashMap finalCondition = HashMap.class.cast(whereinput);
                StringBuilder deleteSQL = new StringBuilder();
                deleteSQL.append("delete from  ");
                deleteSQL.append(schemaData.getObjectMetaData().get(objectName).getTableName());
                String whereClaus = PostgreSqlSelecter.getWhereCondition( finalCondition, objectName, schemaData);
                if(null!=whereClaus) {
                    deleteSQL.append(" where ").append(whereClaus);
                }
                PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync(((sqlConnection, throwable) -> {
                    if(null!=throwable) {
                        if(log.isErrorEnabled()) {
                            HashMap detailMap = new HashMap();
                            detailMap.put(GRAPHQL_WHERE_ARGUMENT,whereinput);
                            log.error("{}",LogData.getErrorLog("E10024",detailMap,throwable));
                        }
                        future.completeExceptionally(new BusinessException("E10024"));
                    } else {
                        sqlConnection.query(deleteSQL.toString(), deleteHandler -> {
                            if (deleteHandler.succeeded()) {
                                RowSet<Row> rows = deleteHandler.result();
                                HashMap resultmap = new HashMap();
                                resultmap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, rows.rowCount());
                                future.complete(resultmap);
                            } else {
                                if(log.isErrorEnabled()) {
                                    HashMap detailMap = new HashMap();
                                    detailMap.put(GRAPHQL_WHERE_ARGUMENT,whereinput);
                                    log.error("{}",LogData.getErrorLog("E10024",detailMap,deleteHandler.cause()));
                                }
                                future.completeExceptionally(new BusinessException("E10024"));
                            }
                            sqlConnection.close();
                        });
                    }
                }));
            }  catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

}

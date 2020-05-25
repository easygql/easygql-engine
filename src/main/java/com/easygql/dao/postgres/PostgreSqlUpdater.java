package com.easygql.dao.postgres;

import com.easygql.dao.DataUpdater;
import com.easygql.util.PostgreSQLPoolCache;
import com.easygql.util.SchemaData;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_AFFECTEDROW_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.POSTGRES_COLUMNNAME_PREFIX;
import static com.easygql.util.EasyGqlUtil.getLastUpdateFields;
import static com.easygql.util.EasyGqlUtil.getNowTimeStamp;

/**
 * @author guofen
 * @date 2019/11/3 15:08
 */
@Slf4j
public class PostgreSqlUpdater implements DataUpdater {
    private SchemaData schemaData;
    private String schemadID;
    private String objectName;
    private String tableName;
    private List<String> lastUpdateFields;

    @Override
    public void Init(String objectName, SchemaData schemaData, String schemaID) {
        this.schemadID = schemaID;
        this.objectName = objectName;
        this.schemaData = schemaData;
        this.tableName = schemaData.getObjectMetaData().get(objectName).getTableName();
        this.lastUpdateFields =getLastUpdateFields(schemaData.getObjectMetaData().get(objectName));
    }

    @Override
    public CompletableFuture<Map> updateWhere(Object whereInput, Object updateObject, String updateType,  HashMap<String,Object> selectionFields) {
        CompletableFuture<Map> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
           try {
               HashMap updateObjectMap = (HashMap) updateObject;
               HashMap finalCondition = (HashMap) whereInput;
               StringBuilder updateSQL = new StringBuilder();
               updateSQL.append("update  ").append(tableName).append(" set ");
               if(lastUpdateFields.size()>0) {
                   OffsetDateTime nowTime = getNowTimeStamp();
                   for(String lastUpdateField : lastUpdateFields) {
                       updateObjectMap.put(lastUpdateField,nowTime);
                   }
               }
               Iterator iterator = updateObjectMap.keySet().iterator();
               int phaseCounter = 0;
               Tuple tuple = Tuple.tuple();
               while (iterator.hasNext()) {
                   if (phaseCounter > 0) {
                       updateSQL.append(" , ");
                   }
                   phaseCounter++;
                   String fieldStr = String.class.cast(iterator.next());
                   Object objectValue=updateObjectMap.get(fieldStr);
                   if(null==objectValue) {
                       tuple.addValue(null);
                   } else {
                       if(objectValue instanceof  List){
                           tuple.addValue(new JsonArray((List)objectValue));
                       } else {
                           tuple.addValue(objectValue);
                       }
                   }
                   updateSQL.append(POSTGRES_COLUMNNAME_PREFIX).append(fieldStr);
                   updateSQL.append(" = $").append(phaseCounter);
               }
               String whereSQL = PostgreSqlSelecter.getWhereCondition((HashMap) finalCondition, objectName, schemaData);
               if(null!=whereSQL) {
                   updateSQL.append(" where ");
                   updateSQL.append(whereSQL);
               }
               PostgreSQLPoolCache.getConnectionFactory(schemaData.getSchemaid()).whenCompleteAsync((sqlConnection, throwable) -> {
                   if(null!=throwable) {
                       future.completeExceptionally(throwable);
                   } else {
                       sqlConnection.preparedQuery(updateSQL.toString(),tuple,updateHandler->{
                           if(updateHandler.succeeded()) {
                               RowSet<Row> rows = updateHandler.result();
                               HashMap resultMap = new HashMap();
                               resultMap.put(GRAPHQL_AFFECTEDROW_FIELDNAME, rows.rowCount());
                               future.complete(resultMap);
                           }  else {
                               future.completeExceptionally(updateHandler.cause());
                           }
                           sqlConnection.close();

                       });
                   }
               });
           }  catch ( Exception e) {
               future.completeExceptionally(e);
           }
        });
        return future;
    }

}

package com.easygql.dao.postgres;

import com.easygql.dao.DataUpdater;
import com.easygql.util.ObjectTypeMetaData;
import com.easygql.util.PostgreSQLPoolCache;
import com.easygql.util.ScalarFieldInfo;
import com.easygql.util.SchemaData;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
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
    private ObjectTypeMetaData objectTypeMetaData;

    @Override
    public void Init(String objectName, SchemaData schemaData, String schemaID) {
        this.schemadID = schemaID;
        this.objectName = objectName;
        this.schemaData = schemaData;
        this.tableName = schemaData.getObjectMetaData().get(objectName).getTableName();
        this.lastUpdateFields =getLastUpdateFields(schemaData.getObjectMetaData().get(objectName));
        this.objectTypeMetaData=schemaData.getObjectMetaData().get(objectName);
    }

    @Override
    public CompletableFuture<Map> updateWhere(Object whereInput, Object updateObject, String updateType) {
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
                       if (objectTypeMetaData
                               .getFields()
                               .get(fieldStr)
                               .equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
                           if (objectTypeMetaData.getEnumFieldData().get(fieldStr).isList()) {
                               List<String> tmpEnumList = (List<String>) objectValue;
                               tuple.addStringArray(tmpEnumList.toArray(new String[tmpEnumList.size()]));
                           } else {
                               tuple.addValue(objectValue);
                           }
                       } else if (objectTypeMetaData.getFields().get(fieldStr).equals(GRAPHQL_SCALARFIELD_TYPENAME)) {
                           ScalarFieldInfo scalarFieldInfo =
                                   objectTypeMetaData.getScalarFieldData().get(fieldStr);
                           if (scalarFieldInfo.isList()) {
                               switch (scalarFieldInfo.getType()) {
                                   case GRAPHQL_ID_TYPENAME:
                                   case GRAPHQL_CHAR_TYPENAME:
                                   case GRAPHQL_STRING_TYPENAME:
                                   case GRAPHQL_URL_TYPENAME:
                                   case GRAPHQL_EMAIL_TYPENAME:
                                   case GRAPHQL_BYTE_TYPENAME:
                                       List<String> tmpScalarStringList = (List<String>) objectValue;
                                       tuple.addStringArray(
                                               tmpScalarStringList.toArray(new String[tmpScalarStringList.size()]));
                                       break;
                                   case GRAPHQL_INT_TYPENAME:
                                       List<Integer> tmpScalarIntegerList = (List<Integer>) objectValue;
                                       tuple.addIntegerArray(
                                               tmpScalarIntegerList.toArray(
                                                       new Integer[tmpScalarIntegerList.size()]));
                                       break;
                                   case GRAPHQL_BOOLEAN_TYPENAME:
                                       List<Boolean> tmpScalarBooleanList = (List<Boolean>) objectValue;
                                       tuple.addBooleanArray(
                                               tmpScalarBooleanList.toArray(
                                                       new Boolean[tmpScalarBooleanList.size()]));
                                       break;
                                   case GRAPHQL_LONG_TYPENAME:
                                   case GRAPHQL_BIGINTEGER_TYPENAME:
                                       List<Long> tmpScalarLongList = (List<Long>) objectValue;
                                       tuple.addLongArray(
                                               tmpScalarLongList.toArray(new Long[tmpScalarLongList.size()]));
                                       break;
                                   case GRAPHQL_SHORT_TYPENAME:
                                       List<Short> tmpScalarShortList = (List<Short>) objectValue;
                                       tuple.addShortArray(
                                               tmpScalarShortList.toArray(new Short[tmpScalarShortList.size()]));
                                       break;
                                   case GRAPHQL_DATE_TYPENAME:
                                   case GRAPHQL_DATETIME_TYPENAME:
                                   case GRAPHQL_CREATEDAT_TYPENAME:
                                   case GRAPHQL_LASTUPDATE_TYPENAME:
                                       List<OffsetDateTime> tmpScalarDateList =
                                               (List<OffsetDateTime>) objectValue;
                                       tuple.addOffsetDateTimeArray(
                                               tmpScalarDateList.toArray(
                                                       new OffsetDateTime[tmpScalarDateList.size()]));
                                       break;
                                   case GRAPHQL_FLOAT_TYPENAME:
                                       List<Float> tmpScalarFloatList = (List<Float>) objectValue;
                                       tuple.addFloatArray(
                                               tmpScalarFloatList.toArray(new Float[tmpScalarFloatList.size()]));
                                       break;
                                   case GRAPHQL_BIGDECIMAL_TYPENAME:
                                       List<BigDecimal> tmpScalarBigDecimalist = (List<BigDecimal>) objectValue;
                                       tuple.addValues(tmpScalarBigDecimalist.toArray(new Float[tmpScalarBigDecimalist.size()]));
                                       break;
                                   default:
                                       List tmpObjectValue = (List) objectValue;
                                       tuple.addValues(tmpObjectValue.toArray());
                               }
                           } else {
                               tuple.addValue(objectValue);
                           }
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

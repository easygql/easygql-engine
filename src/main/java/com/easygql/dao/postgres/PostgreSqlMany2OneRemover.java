package com.easygql.dao.postgres;

import com.easygql.dao.Many2OneRelationRemover;
import com.easygql.util.ObjectTypeMetaData;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_ID_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.POSTGRES_COLUMNNAME_PREFIX;

public class PostgreSqlMany2OneRemover implements Many2OneRelationRemover{
    private String schemaID;
    private SchemaData schemaData;
    private RelationField relationField;
    private String fromSql;
    private String toSql1;
    private String toSql2;
    private ObjectTypeMetaData fromObject;
    private ObjectTypeMetaData toObject;
    @Override
    public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
        this.schemaData=schemaData;
        this.schemaID=schemaID;
        this.relationField=relationField;
        this.fromSql = "update "+schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()+" set  "+POSTGRES_COLUMNNAME_PREFIX+relationField.getFromfield()+"= null  where "+POSTGRES_COLUMNNAME_PREFIX+GRAPHQL_ID_FIELDNAME+"=$1 ";
        this.toSql1 = "update "+schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()+" set  "+POSTGRES_COLUMNNAME_PREFIX+relationField.getFromfield()+"= null  where "+POSTGRES_COLUMNNAME_PREFIX+relationField.getFromfield()+"=$1 and "+POSTGRES_COLUMNNAME_PREFIX+GRAPHQL_ID_FIELDNAME+" = $2 ";
        this.toSql2 = "update "+schemaData.getObjectMetaData().get(relationField.getFromobject()).getTableName()+" set  "+POSTGRES_COLUMNNAME_PREFIX+relationField.getFromfield()+"= null  where "+POSTGRES_COLUMNNAME_PREFIX+relationField.getFromfield()+"=$1 ";
        this.fromObject=schemaData.getObjectMetaData().get(relationField.getFromobject());
        this.toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
    }

    @Override
    public CompletableFuture<Object> fromRemove(String srcId) {
        Tuple tuple = Tuple.of(srcId);
        return  PostgreSqlOne2OneRemover.doRemove(fromSql,tuple,schemaData);
    }

    @Override
    public CompletableFuture<Object> toRemove(String destID, List<String> srcIDList, Boolean isReset) {
        if(null==srcIDList) {
            return  PostgreSqlOne2OneRemover.doRemove(toSql2,Tuple.of(destID),schemaData);
        } else {
            List<Tuple> tupleList = new ArrayList<>();
            for (String srcID:srcIDList) {
                tupleList.add(Tuple.of(destID).addValue(srcID));
            }
            return  PostgreSqlOne2ManyRemover.doRemove(toSql1,tupleList,schemaData);
        }
    }

}

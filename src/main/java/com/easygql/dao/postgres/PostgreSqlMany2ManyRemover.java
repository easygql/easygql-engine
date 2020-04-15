package com.easygql.dao.postgres;

import com.easygql.dao.Many2ManyRelationRemover;
import com.easygql.util.ObjectTypeMetaData;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.POSTGRES_FROM_COLUMNNAME;
import static com.easygql.component.ConfigurationProperties.POSTGRES_TO_COLUMNNAME;

public class PostgreSqlMany2ManyRemover implements Many2ManyRelationRemover{
    private String schemaID;
    private SchemaData schemaData;
    private RelationField relationField;
    private String fromSql1;
    private String fromSql2;
    private String toSql1;
    private ObjectTypeMetaData fromObject;
    private ObjectTypeMetaData toObject;
    @Override
    public void Init(SchemaData schemaData, String schemaID, RelationField relationField) {
        this.schemaData = schemaData;
        this.schemaID=schemaID;
        this.relationField = relationField;
        this.fromObject=schemaData.getObjectMetaData().get(relationField.getFromobject());
        this.toObject = schemaData.getObjectMetaData().get(relationField.getToobject());
        String tableName = PostgreSqlSchema.getTableNameofRelation(relationField);
        this.fromSql1 = "delete from "+tableName+" where "+POSTGRES_FROM_COLUMNNAME+" = $1 ";
        this.fromSql2 = "delete from "+tableName+" where "+POSTGRES_FROM_COLUMNNAME+" = $1  and "+POSTGRES_TO_COLUMNNAME+" = $2 ";
        this.toSql1 = "delete from "+tableName+" where "+POSTGRES_TO_COLUMNNAME+" = $1 ";

    }

    @Override
    public CompletableFuture<Object> fromRemove(String srcID, List<String> destIDList) {
        if(null==destIDList) {
            return  PostgreSqlOne2OneRemover.doRemove(fromSql1,Tuple.of(srcID),schemaData);
        } else {
            List<Tuple> tupleList = new ArrayList<>();
            for (String destID:destIDList) {
                tupleList.add(Tuple.of(srcID).addString(destID));
            }
            return PostgreSqlOne2ManyRemover.doRemove(fromSql2,tupleList,schemaData);
        }
    }

    @Override
    public CompletableFuture<Object> toRemove(String destID, List<String> srcIDList) {
        if(null==srcIDList) {
            Tuple tuple = Tuple.of(destID);
            return  PostgreSqlOne2OneRemover.doRemove(toSql1,tuple,schemaData);
        } else {
            List<Tuple> tupleList = new ArrayList<>();
            for (String srcID : srcIDList) {
                tupleList.add(Tuple.of(srcID).addString(destID));
            }
            return PostgreSqlOne2ManyRemover.doRemove(fromSql2,tupleList,schemaData);
        }
    }
}

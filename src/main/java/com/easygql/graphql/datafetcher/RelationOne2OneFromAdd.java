package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2OneRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_FROM_ID;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_TO_OBJECT;
import static com.easygql.util.EasyGqlUtil.*;

public class RelationOne2OneFromAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2OneRelationCreater one2OneRelationCreater;
    private HashSet<String> disabledRoles ;
    private HashMap<String, HashSet> forbiddenFields;
    private List<String> lastUpdateFields;
    private List<String> createdAtFields;

    public RelationOne2OneFromAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2OneRelationCreater = DaoFactory.getOne2OneRelationCreator(schemaData.getDatabasekind());
        this.one2OneRelationCreater.Init(schemaData,schemaID,relationField);
        this.disabledRoles=new HashSet<>();
        this.disabledRoles.addAll(schemaData.getObjectMetaData().get(relationField.getToObject()).getUninsertableRoles());
        this.forbiddenFields = new HashMap<>();
        DataFetcherCreate.forbiddenFieldsConstruct(schemaData,relationField.getToObject(),forbiddenFields);
        this.lastUpdateFields =getLastUpdateFields(schemaData.getObjectMetaData().get(relationField.getToObject()));
        this.createdAtFields =getCreateAtFields(schemaData.getObjectMetaData().get(relationField.getToObject()));
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String fromID = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                Object toObject = dataFetchingEnvironment.getArgument(GRAPHQL_TO_OBJECT);
                HashMap resultInfo = new HashMap();
                transferIdAndLastUpdate(toObject, resultInfo,lastUpdateFields,createdAtFields);
                Object addObject = null;
                Iterator<Object> iterator = resultInfo.values().iterator();
                if(iterator.hasNext()) {
                    addObject=iterator.next();
                }
                one2OneRelationCreater.doAdd(fromID,addObject).whenComplete((result,resultEx)->{
                    if(null!=resultEx) {
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}

package com.easygql.graphql.datafetcher;

import com.easygql.dao.One2ManyRelationCreater;
import com.easygql.util.DaoFactory;
import com.easygql.util.RelationField;
import com.easygql.util.SchemaData;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.EasyGqlUtil.*;

public class RelationOne2ManyFromAdd implements  EasyGQLDataFetcher<Object> {
    protected RelationField relationField;
    protected String schemaID;
    protected SchemaData schemaData;
    protected One2ManyRelationCreater one2ManyRelationCreater;
    private HashSet<String> disabledRoles ;
    private HashMap<String, HashSet> forbiddenFields;
    private List<String> lastUpdateFields;
    private List<String> createdAtFields;


    public RelationOne2ManyFromAdd(RelationField relationField, String schemaID, SchemaData schemaData) {
        this.relationField = relationField;
        this.schemaID = schemaID;
        this.schemaData = schemaData;
        this.one2ManyRelationCreater = DaoFactory.getOne2ManyRelationCreator(schemaData.getDatabasekind());
        this.one2ManyRelationCreater.Init(schemaData,schemaID,relationField);
        this.disabledRoles=new HashSet<>();
        this.disabledRoles.addAll(schemaData.getObjectMetaData().get(relationField.getToobject()).getUninsertable_roles());
        if(null!=relationField.getIrrevisible()) {
            this.disabledRoles.addAll(relationField.getIrrevisible());
        }
        this.forbiddenFields = new HashMap<>();
        DataFetcherCreate.forbiddenFieldsConstruct(schemaData,relationField.getToobject(),forbiddenFields);
        this.lastUpdateFields =getLastUpdateFields(schemaData.getObjectMetaData().get(relationField.getToobject()));
        this.createdAtFields =getCreateAtFields(schemaData.getObjectMetaData().get(relationField.getToobject()));
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture.runAsync(()->{
            try {
                String fromID = dataFetchingEnvironment.getArgument(GRAPHQL_FROM_ID);
                List<Object> toObjects = dataFetchingEnvironment.getArgument(GRAPHQL_TO_OBJECT);
                HashMap resultInfo = new HashMap();
                transferIdAndLastUpdate(toObjects, resultInfo,lastUpdateFields,createdAtFields);
                Boolean isReset = dataFetchingEnvironment.getArgument(GRAPHQL_RESET_FIELDNAME);
                List<Object> addObjects = new ArrayList<>();
                addObjects.addAll(resultInfo.values());
                one2ManyRelationCreater.fromAdd(fromID,addObjects,isReset).whenComplete((result,resultEx)->{
                    if(null!=resultEx){
                        future.completeExceptionally(resultEx);
                    } else {
                        future.complete(result);
                    }
                });
            } catch (Exception e ) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}

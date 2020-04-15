package com.easygql.dao;

public interface DaoGenerator {
    DataInserter getInserter();
    DataDeleter getDeleter();
    DataSelecter getSelecter();
    DataUpdater getUpdater();
    DataSub getSub();
    SchemaDao getSchemaDao();
    TriggerDao getTriggerDao();
    Many2ManyRelationCreater getMany2ManyRelationCreater();
    Many2ManyRelationRemover getMany2ManyRelationRemover();
    Many2OneRelationCreater getMany2OneRelationCreater();
    Many2OneRelationRemover getMany2OneRelationRemover();
    One2ManyRelationCreater getOne2ManyRelationCreater();
    One2ManyRelationRemover getOne2ManyRelationRemover();
    One2OneRelationCreater getOne2OneRelationCreater();
    One2OneRelationRemover getOne2OneRelationRemover();
}

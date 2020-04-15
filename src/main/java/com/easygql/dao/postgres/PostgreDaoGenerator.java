package com.easygql.dao.postgres;

import com.easygql.annotation.EasyGQLDaoGenerator;
import com.easygql.dao.*;

@EasyGQLDaoGenerator("PostgreSQL")
public class PostgreDaoGenerator implements DaoGenerator {
    @Override
    public DataInserter getInserter() {
       return new PostgreSqlInserter();
    }

    @Override
    public DataDeleter getDeleter() {
        return new PostgreSqLDeleter();
    }

    @Override
    public DataSelecter getSelecter() {
        return new PostgreSqlSelecter();
    }

    @Override
    public DataUpdater getUpdater() {
        return new PostgreSqlUpdater();
    }


    @Override
    public DataSub getSub() {
        return new PostgreSqlSub();
    }

    @Override
    public SchemaDao getSchemaDao() {
        return new PostgreSqlSchema();
    }

    @Override
    public Many2ManyRelationCreater getMany2ManyRelationCreater() {
        return  new PostgreSqlMany2ManyAdd();
    }

    @Override
    public Many2ManyRelationRemover getMany2ManyRelationRemover() {
        return new PostgreSqlMany2ManyRemover();
    }

    @Override
    public Many2OneRelationCreater getMany2OneRelationCreater() {
        return new PostgreSqlMany2OneAdd();
    }

    @Override
    public Many2OneRelationRemover getMany2OneRelationRemover() {
        return new PostgreSqlMany2OneRemover();
    }

    @Override
    public One2ManyRelationCreater getOne2ManyRelationCreater() {
        return new PostgreSqlOne2ManyAdd();
    }

    @Override
    public One2ManyRelationRemover getOne2ManyRelationRemover() {
        return new PostgreSqlOne2ManyRemover();
    }

    @Override
    public One2OneRelationCreater getOne2OneRelationCreater() {
        return new PostgreSqlOne2OneAdd();
    }

    @Override
    public One2OneRelationRemover getOne2OneRelationRemover() {
        return new PostgreSqlOne2OneRemover();
    }
    @Override
    public TriggerDao getTriggerDao() {
        return new PostgreSqlTriggerDao();
    }
}

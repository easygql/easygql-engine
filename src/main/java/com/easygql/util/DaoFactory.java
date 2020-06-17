package com.easygql.util;

import com.easygql.dao.*;
import com.easygql.exception.BusinessException;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author guofen
 * @date 2019/12/18 19:29
 */
public class DaoFactory {
  private static HashMap<String, DaoGenerator> daoGeneratorHashMap = new HashMap<>();

  public static synchronized void reset(HashMap daoGeneratorHashMaptmp) {
    daoGeneratorHashMap.clear();
    daoGeneratorHashMap.putAll(daoGeneratorHashMaptmp);
  }

  public static DataDeleter getDeleteDao(@NonNull String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getDeleter();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static DataInserter getInsertDao(@NonNull String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getInserter();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static DataUpdater getUpdaterDao(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getUpdater();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static DataSelecter getSelecterDao(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getSelecter();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static DataSub getDataSub(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getSub();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static Many2ManyRelationCreater getMany2ManyRelationCreator(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getMany2ManyRelationCreater();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static Many2OneRelationCreater getMany2OneRelationCreator(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getMany2OneRelationCreater();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static One2OneRelationCreater getOne2OneRelationCreator(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getOne2OneRelationCreater();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static One2ManyRelationCreater getOne2ManyRelationCreator(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getOne2ManyRelationCreater();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static Many2ManyRelationRemover getMany2ManyRelationRemover(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getMany2ManyRelationRemover();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static Many2OneRelationRemover getMany2OneRelationRemover(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getMany2OneRelationRemover();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static One2ManyRelationRemover getOne2ManyRelationRemover(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getOne2ManyRelationRemover();
    } else {

      throw new BusinessException("E10047");
    }
  }

  public static One2OneRelationRemover getOne2OneRelationRemover(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getOne2OneRelationRemover();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static SchemaDao getSchemaDao(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getSchemaDao();
    } else {
      throw new BusinessException("E10047");
    }
  }

  public static TriggerDao getTriggerDao(String databaseKind) {
    if (null != daoGeneratorHashMap.get(databaseKind)) {
      return daoGeneratorHashMap.get(databaseKind).getTriggerDao();
    } else {
      throw new BusinessException("E10047");
    }
  }
  public static List<String> supportedDataBase(){
    List<String> suporrtedDB = new ArrayList<>();
    suporrtedDB.addAll(daoGeneratorHashMap.keySet());
    return  suporrtedDB;
  }
}

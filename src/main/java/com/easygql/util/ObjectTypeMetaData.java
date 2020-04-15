package com.easygql.util;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/6 20:27
 */
@Data
public class ObjectTypeMetaData {
    private String id;
    private String outPutName;
    private String whereInputObjectName;
    private String updateObjectName;
    private String fieldFilterName;
    private String inputObjectName;
    private String apiName_selectByID;
    private String apiName_selectAll;
    private String apiName_update;
    private String apiName_delete;
    private String apiName_insert;
    private String apiName_subscription;
    private String tableName;
    private HashMap<String,String> fields;
    private HashMap<String, ScalarFieldInfo> scalarFieldData;
    private HashMap<String,EnumField> enumFieldData;
    private HashMap<String,RelationField> fromRelationFieldData;
    private HashMap<String,RelationField> toRelationFieldData;
    private List<String> unreadable_roles;
    private List<String> uninsertable_roles;
    private List<String> undeletable_roles;
    private List<String> unupdatable_roles;
    private HashMap<String,HashMap> read_constraints;
    private HashMap<String,HashMap> update_constraints;
    private HashMap<String,HashMap> delete_constraints;
    private List<UniqueConstraint> uniqueConstraints;
}

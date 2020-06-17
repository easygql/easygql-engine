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
    private String apiNameSelectByID;
    private String apiNameSelectAll;
    private String apiNameUpdate;
    private String apiNameDelete;
    private String apiNameInsert;
    private String apiNameSubscription;
    private String tableName;
    private String alias;
    private HashMap<String,String> fields;
    private HashMap<String, ScalarFieldInfo> scalarFieldData;
    private HashMap<String,EnumField> enumFieldData;
    private HashMap<String,RelationField> fromRelationFieldData;
    private HashMap<String,RelationField> toRelationFieldData;
    private List<String> unreadableRoles;
    private List<String> uninsertableRoles;
    private List<String> undeletableRoles;
    private List<String> unupdatableRoles;
    private HashMap<String,HashMap> readConstraints;
    private HashMap<String,HashMap> updateConstraints;
    private HashMap<String,HashMap> deleteConstraints;
    private List<UniqueConstraint> uniqueConstraints;
}

package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 10:27
 */
@Data
public class SchemaObject {
    private String id;
    private String name;
    private DataSourceInfo datasourceinfo;
    private String databasekind;
    private List<ThirdPartAPIMetaData> thirdapis= new ArrayList<>();
    private List<ObjectTypeInfo> objecttypes = new ArrayList<>();
    private List<EnumTypeMetaData> enumtypes=new ArrayList<>();
    private List<RelationField> relations = new ArrayList<>();
    private List<Trigger> triggerInfo = new ArrayList<>();
}

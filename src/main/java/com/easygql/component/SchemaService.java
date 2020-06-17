package com.easygql.component;

import com.easygql.dao.SchemaDao;
import com.easygql.service.EasyGQLInitator;
import com.easygql.thirdapis.SchemaStart;
import com.easygql.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Component("SchemaService")
@Slf4j
public class SchemaService {
  public static SchemaService schemaService;
  public static final String[] restoreStatus = {"starting", "running"};
  public static final String closeStatus = "stopping";
  private static String schemaDB;
  private SchemaDao schemadao;
  private SchemaData defaultSchemaData;
  private SchemaStart schemaStart=new SchemaStart();
  private EasyGQLInitator easyGQLInitator=new EasyGQLInitator();
  private TriggerCache triggerCache=new TriggerCache();

  public SchemaService() {}

  /** */
  @PostConstruct
  public void init() {
    ConfigurationProperties.reload();
    LogData.resetMessage();
    easyGQLInitator.restPool();
    schemaDB = GRAPHQL_SCHEMA_ID_DEFAULT;
    schemaService = this;
    schemadao = DaoFactory.getSchemaDao(getInstance().SCHEMA_DATABASE_KIND);
    OperatorType.setup();
    SchemaObject schemaObject = initialSchemaDbObject();
    SchemaData defaultSchemaData = GraphQLElementGenerator.transferSchema(schemaObject);
    try {
      Boolean ifDataBaseExists = schemadao.ifDataBaseExists(schemaObject.getDatasourceinfo()).get();
      if (!ifDataBaseExists) {
           schemadao.schemaInitial(defaultSchemaData);
      }
    } catch (Exception e) {
      if(log.isErrorEnabled()) {
        log.error("{}", LogData.getErrorLog("E10033",null,e));
      }
    }
    try {
      schemaStart.startSchema(defaultSchemaData, schemaDB,new ArrayList<>()).get();
      GraphQLCache.init();
    } catch (Exception e) {
      if(log.isErrorEnabled()) {
        log.error("{}",LogData.getErrorLog("E10010",null,e));
      }
    }
    triggerCache.init();
  }

  /** 初始化SchemaDB */
  public SchemaObject initialSchemaDbObject() {
    // 判断数据库是否初始化完毕
    // 判断是否由默认的schema
    SchemaObject schemaDBOBJ = new SchemaObject();
    List<String> allRoleStr = new ArrayList<>();
    allRoleStr.add("Admin");
    allRoleStr.add("Guest");
    schemaDBOBJ.setId(GRAPHQL_SCHEMA_ID_DEFAULT);
    List thirdAPIS  = new ArrayList();
      schemaDBOBJ.setThirdapis(thirdAPIS);
    schemaDBOBJ.setName(GRAPHQL_SCHEMANAME_DEFAULT);
    schemaDBOBJ.setDatabasekind(DATABASE_KIND_POSTGRES);
    DataSourceInfo dataSourceInfo =
        DataSourceInfo.builder()
            .host(getInstance().POSTGRES_IP)
            .port(getInstance().POSTGRES_PORT)
            .username(getInstance().POSTGRES_USER)
            .password(getInstance().POSTGRES_PASS)
            .databasename(GRAPHQL_DATABASENAME_PREFIX + GRAPHQL_SCHEMA_ID_DEFAULT)
            .max_connection(getInstance().POSTGRES_MAX_CONNECTIONS)
            .max_idle_connection(getInstance().POSTGRES_MAX_IDLE_CONNECTIONS)
            .build();
    schemaDBOBJ.setDatasourceinfo(dataSourceInfo);
    // schemaObjectType
    ObjectTypeInfo schemaObjectType = new ObjectTypeInfo();
    schemaObjectType.setId(GRAPHQL_SCHEMAOBJECT_ID);
    schemaObjectType.setName(GRAPHQL_SCHEMA_TYPENAME);
    schemaObjectType.getScalarFields().add(getIDField());
    schemaObjectType.getScalarFields().add(getNameField());
    ScalarFieldInfo schemaObjectDescriptionField = new ScalarFieldInfo();
    schemaObjectDescriptionField.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    schemaObjectDescriptionField.setDefaultValue("");
    schemaObjectDescriptionField.setType(GRAPHQL_STRING_TYPENAME);
    schemaObjectType.getScalarFields().add(schemaObjectDescriptionField);
    EnumField schemaObjectDataBaseKindField = new EnumField();
    schemaObjectDataBaseKindField.setId(IDTools.getID());
    schemaObjectDataBaseKindField.setDescription("数据库类型");
    schemaObjectDataBaseKindField.setName(GRAPHQL_DATABASEKIND_FIELDNAME);
    schemaObjectDataBaseKindField.setDefaultValue(ConfigurationProperties.getInstance().SCHEMA_DATABASE_KIND);
    schemaObjectDataBaseKindField.setType(GRAPHQL_DATABASEKIND_ENUMNAME);
    schemaObjectType.getEnumFields().add(schemaObjectDataBaseKindField);
    RelationField schemaObjectDatasourceField = new RelationField();
    schemaObjectDatasourceField.setId(IDTools.getID());
    schemaObjectDatasourceField.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    schemaObjectDatasourceField.setToObject(GRAPHQL_DATASOURCE_TYPENAME);
    schemaObjectDatasourceField.setRelationType(GRAPHQL_ONE2ONE_NAME);
    schemaObjectDatasourceField.setFromField(GRAPHQL_DATASOURCE_FIELDNAME);
    schemaObjectDatasourceField.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    schemaObjectDatasourceField.setDescription("数据库");
    schemaObjectDatasourceField.setIfCascade(true);
    UniqueConstraint schemaNameUniqueConstraint = new UniqueConstraint();
    schemaNameUniqueConstraint.setConstraintName("schemaNameUnique");
    List<String> schemaNameUnique = new ArrayList<>();
    schemaNameUnique.add(GRAPHQL_NAME_FIELDNAME);
    schemaNameUniqueConstraint.setFieldNames(schemaNameUnique);
    schemaObjectType.getUniqueConstraints().add(schemaNameUniqueConstraint);
    schemaDBOBJ.getRelations().add(schemaObjectDatasourceField);
    RelationField schemaObjectThirdAPIs = new RelationField();
    schemaObjectThirdAPIs.setId(IDTools.getID());
    schemaObjectThirdAPIs.setFromField(GRAPHQL_THIRD_API_FIELDNAME);
    schemaObjectThirdAPIs.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    schemaObjectThirdAPIs.setToObject(GRAPHQL_THIRDAPI_TYPE_NAME);
    schemaObjectThirdAPIs.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    schemaObjectThirdAPIs.setRelationType(GRAPHQL_ONE2MANY_NAME);
    schemaObjectThirdAPIs.setIfCascade(true);
    schemaDBOBJ.getRelations().add(schemaObjectThirdAPIs);
    RelationField schemaObjectObjectTypesField = new RelationField();
    schemaObjectObjectTypesField.setId(IDTools.getID());
    schemaObjectObjectTypesField.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    schemaObjectObjectTypesField.setFromField(GRAPHQL_OBJECTTYPES_FIELDNAME);
    schemaObjectObjectTypesField.setToObject(GRAPHQL_CONTENTTYPE_TYPENAME);
    schemaObjectObjectTypesField.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    schemaObjectObjectTypesField.setRelationType(GRAPHQL_ONE2MANY_NAME);
    schemaObjectObjectTypesField.setIfCascade(true);
    schemaDBOBJ.getRelations().add(schemaObjectObjectTypesField);
    RelationField schemaObjectEnumTypes = new RelationField();
    schemaObjectEnumTypes.setId(IDTools.getID());
    schemaObjectEnumTypes.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    schemaObjectEnumTypes.setFromField(GRAPHQL_ENUMTYPES_FIELDNAME);
    schemaObjectEnumTypes.setToObject(GRAPHQL_ENUMTYPE_TYPENAME);
    schemaObjectEnumTypes.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    schemaObjectEnumTypes.setRelationType(GRAPHQL_ONE2MANY_NAME);
    schemaObjectEnumTypes.setIfCascade(true);
    schemaDBOBJ.getRelations().add(schemaObjectEnumTypes);
    EnumField schemaInfoStatus = new EnumField();
    schemaInfoStatus.setName(GRAPHQL_SCHEMASTATUS_FIELDNAME);
    schemaInfoStatus.setId(IDTools.getID());
    schemaInfoStatus.setDefaultValue(SCHEMA_STATUS_UNINITIALIZED);
    schemaInfoStatus.setType(GRAPHQL_SCHEMASTATUS_TYPENAME);
    schemaObjectType.getEnumFields().add(schemaInfoStatus);
    RelationField publishedSchemaField = new RelationField();
    publishedSchemaField.setId(IDTools.getID());
    publishedSchemaField.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    publishedSchemaField.setRelationType(GRAPHQL_ONE2ONE_NAME);
    publishedSchemaField.setFromField(GRAPHQL_PUBLISHEDSCHEMA_FIELDNAME);
    publishedSchemaField.setToObject(GRAPHQL_PUBLISHEDSCHEMA_TYPENAME);
    publishedSchemaField.setToField(GRAPHQL_PUBLISHEDSCHEMAID_FIELDNAME);
    publishedSchemaField.setIfCascade(true);

    schemaDBOBJ.getRelations().add(publishedSchemaField);
    RelationField publishedHistorySchemaField = new RelationField();
    publishedHistorySchemaField.setId(IDTools.getID());
    publishedHistorySchemaField.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    publishedHistorySchemaField.setRelationType(GRAPHQL_ONE2MANY_NAME);
    publishedHistorySchemaField.setFromField(GRAPHQL_PUBLISHEDHISTORY_FIELDNAME);
    publishedHistorySchemaField.setToObject(GRAPHQL_PUBLISHEDSCHEMA_TYPENAME);
    publishedHistorySchemaField.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    publishedHistorySchemaField.setIfCascade(true);
    schemaDBOBJ.getRelations().add(publishedHistorySchemaField);
    RelationField triggersField = new RelationField();
    triggersField.setId(IDTools.getID());
    triggersField.setIfCascade(true);
    triggersField.setFromField(GRAPHQL_TRIGGERS_FIELDNAME);
    triggersField.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    triggersField.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    triggersField.setToObject(GRAPHQL_TRIGGER_TYPENAME);
    triggersField.setRelationType(GRAPHQL_ONE2MANY_NAME);
    schemaDBOBJ.getRelations().add(triggersField);
    RelationField relationFields = new RelationField();
    relationFields.setId(IDTools.getID());
    relationFields.setFromObject(GRAPHQL_SCHEMA_TYPENAME);
    relationFields.setFromField(GRAPHQL_RELATION_FIELDNAME);
    relationFields.setToObject(GRAPHQL_RELATIONFIELD_TYPENAME);
    relationFields.setToField(GRAPHQL_SCHEMAID_FIELDNAME);
    relationFields.setIfCascade(true);
    relationFields.setRelationType(GRAPHQL_ONE2MANY_NAME);
    schemaDBOBJ.getRelations().add(relationFields);
    schemaDBOBJ.getObjecttypes().add(schemaObjectType);
    // ThirdAPI
    ObjectTypeInfo thirdAPIType = new ObjectTypeInfo();
    thirdAPIType.setId(GRAPHQL_THIRDAPI_TYPE_ID);
    thirdAPIType.setName(GRAPHQL_THIRDAPI_TYPE_NAME);
    thirdAPIType.getScalarFields().add(getIDField());
    ScalarFieldInfo thirdAPIAPIName = new ScalarFieldInfo();
    thirdAPIAPIName.setId(IDTools.getID());
    thirdAPIAPIName.setType(GRAPHQL_STRING_TYPENAME);
    thirdAPIAPIName.setNotNull(true);
    thirdAPIAPIName.setName(GRAPHQL_APINAME_FIELDNAME);
    thirdAPIAPIName.setDescription("对应API名字");
    thirdAPIType.getScalarFields().add(thirdAPIAPIName);
    ScalarFieldInfo thirdAPIDisabledRoles = new ScalarFieldInfo();
    thirdAPIDisabledRoles.setId(IDTools.getID());
    thirdAPIDisabledRoles.setType(GRAPHQL_STRING_TYPENAME);
    thirdAPIDisabledRoles.setName(GRAPHQL_DISABLED_ROLES_FIELDNAME);
    thirdAPIDisabledRoles.setList(true);
    thirdAPIDisabledRoles.setDescription("限制访问角色");
    thirdAPIType.getScalarFields().add(thirdAPIDisabledRoles);
    thirdAPIType.setUndeletableRoles(allRoleStr);
    thirdAPIType.setUninsertableRoles(allRoleStr);
    thirdAPIType.setUnupdatableRoles(allRoleStr);
    thirdAPIType.setUnreadableRoles(allRoleStr);
    schemaDBOBJ.getObjecttypes().add(thirdAPIType);
    // DataSource
    ObjectTypeInfo datasourceInfoType = new ObjectTypeInfo();
    datasourceInfoType.setId(IDTools.getID());
    datasourceInfoType.setName(GRAPHQL_DATASOURCE_TYPENAME);
    datasourceInfoType.setDescription("数据库连接信息");
    datasourceInfoType.getScalarFields().add(getIDField());
    ScalarFieldInfo datasourceDatasourceName = new ScalarFieldInfo();
    datasourceDatasourceName.setId(IDTools.getID());
    datasourceDatasourceName.setName(GRAPHQL_NAME_FIELDNAME);
    datasourceDatasourceName.setType(GRAPHQL_STRING_TYPENAME);
    datasourceDatasourceName.setDescription("数据源名字");
    datasourceDatasourceName.setNotNull(true);
    datasourceInfoType.getScalarFields().add(datasourceDatasourceName);
    ScalarFieldInfo databaseName = new ScalarFieldInfo();
    databaseName.setName(GRAPHQL_DATABASENAME_FIELDNAME);
    databaseName.setType(GRAPHQL_STRING_TYPENAME);
    databaseName.setDescription("数据库名");
    datasourceInfoType.getScalarFields().add(databaseName);
    ScalarFieldInfo datasourceHost = new ScalarFieldInfo();
    datasourceHost.setDescription("数据源的IP地址或域名");
    datasourceHost.setType(GRAPHQL_STRING_TYPENAME);
    datasourceHost.setName(GRAPHQL_HOST_FIELDNAME);
    datasourceHost.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasourceHost);
    ScalarFieldInfo datasourcePort = new ScalarFieldInfo();
    datasourcePort.setId(IDTools.getID());
    datasourcePort.setName(GRAPHQL_PORT_FIELDNAME);
    datasourcePort.setType(GRAPHQL_STRING_TYPENAME);
    datasourcePort.setDescription("端口");
    datasourceInfoType.getScalarFields().add(datasourcePort);
    ScalarFieldInfo datasource_character_encoding = new ScalarFieldInfo();
    datasource_character_encoding.setId(IDTools.getID());
    datasource_character_encoding.setType(GRAPHQL_STRING_TYPENAME);
    datasource_character_encoding.setName(GRAPHQL_CHARACTER_ENCODING_FIELDNAME);
    datasource_character_encoding.setDescription("数据库编码方式");
    datasourceInfoType.getScalarFields().add(datasource_character_encoding);
    ScalarFieldInfo datasource_max_connections = new ScalarFieldInfo();
    datasource_max_connections.setName(GRAPHQL_MAX_CONNECTION_FIELDNAME);
    datasource_max_connections.setType(GRAPHQL_INT_TYPENAME);
    datasource_max_connections.setDescription("最大连接数");
    datasource_max_connections.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasource_max_connections);
    ScalarFieldInfo datasource_min_connection = new ScalarFieldInfo();
    datasource_min_connection.setDescription("最少连接数");
    datasource_min_connection.setName(GRAPHQL_MIN_CONNECTION_FIELDNAME);
    datasource_min_connection.setType(GRAPHQL_INT_TYPENAME);
    datasource_min_connection.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasource_min_connection);
    ScalarFieldInfo datasource_max_idle_connection = new ScalarFieldInfo();
    datasource_max_idle_connection.setId(IDTools.getID());
    datasource_max_idle_connection.setName(GRAPHQL_MAX_IDLE_CONNECTION_FIELDNAME);
    datasource_max_idle_connection.setType(GRAPHQL_INT_TYPENAME);
    datasource_max_idle_connection.setDescription("最大空闲连接数");
    datasourceInfoType.getScalarFields().add(datasource_max_idle_connection);
    ScalarFieldInfo datasource_min_idle_connection = new ScalarFieldInfo();
    datasource_min_idle_connection.setName(GRAPHQL_MIN_IDLE_CONNECTION_FIELDNAME);
    datasource_min_idle_connection.setDescription("最少空闲连接数");
    datasource_min_idle_connection.setType(GRAPHQL_INT_TYPENAME);
    datasourceInfoType.getScalarFields().add(datasource_min_idle_connection);
    ScalarFieldInfo datasourceUsername = new ScalarFieldInfo();
    datasourceUsername.setName(GRAPHQL_USERNAME_FIELD);
    datasourceUsername.setDescription("用户名");
    datasourceUsername.setType(GRAPHQL_STRING_TYPENAME);
    datasourceUsername.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasourceUsername);
    ScalarFieldInfo dataSourcePassword = new ScalarFieldInfo();
    dataSourcePassword.setName(GRAPHQL_PASSWORD_FIELD);
    dataSourcePassword.setDescription("密码");
    dataSourcePassword.setType(GRAPHQL_STRING_TYPENAME);
    dataSourcePassword.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(dataSourcePassword);
    ScalarFieldInfo datasource_connectionstr = new ScalarFieldInfo();
    datasource_connectionstr.setType(GRAPHQL_STRING_TYPENAME);
    datasource_connectionstr.setName(GRAPHQL_CONNECTION_STR_FIELDNAME);
    datasource_connectionstr.setDescription("连接字符串");
    datasource_connectionstr.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasource_connectionstr);
    ScalarFieldInfo datasource_replica_name = new ScalarFieldInfo();
    datasource_replica_name.setDescription("副本集名字");
    datasource_replica_name.setName(GRAPHQL_REPLICA_NAME_FIELDNAME);
    datasource_replica_name.setType(GRAPHQL_STRING_TYPENAME);
    datasource_replica_name.setId(IDTools.getID());
    datasourceInfoType.getScalarFields().add(datasource_replica_name);
    schemaDBOBJ.getObjecttypes().add(datasourceInfoType);
    // SchemaMetaData 用于存储发布了的graphql schema
    ObjectTypeInfo schemaMetaData = new ObjectTypeInfo();
    schemaMetaData.setName(GRAPHQL_PUBLISHEDSCHEMA_TYPENAME);
    schemaMetaData.getScalarFields().add(getIDField());
    ScalarFieldInfo schemaobjectField = new ScalarFieldInfo();
    schemaobjectField.setName(GRAPHQL_SCHEMAOBJECT_FIELDNAME);
    schemaobjectField.setId(IDTools.getID());
    schemaobjectField.setNotNull(true);
    schemaobjectField.setType(GRAPHQL_OBJECT_TYPENAME);
    schemaMetaData.getScalarFields().add(schemaobjectField);
    ScalarFieldInfo versionIDField = new ScalarFieldInfo();
    versionIDField.setName(GRAPHQL_VERSIONID_FIELDNAME);
    versionIDField.setId(IDTools.getID());
    versionIDField.setType(GRAPHQL_STRING_TYPENAME);
    versionIDField.setNotNull(true);
    schemaMetaData.getScalarFields().add(versionIDField);
    schemaDBOBJ.getObjecttypes().add(schemaMetaData);
    // ObjectTypeInfo
    ObjectTypeInfo objectTypeSelf = new ObjectTypeInfo();
    objectTypeSelf.setName(GRAPHQL_CONTENTTYPE_TYPENAME);
    objectTypeSelf.getScalarFields().add(getIDField());
    ScalarFieldInfo nameField = new ScalarFieldInfo();
    nameField.setNotNull(true);
    nameField.setName(GRAPHQL_NAME_FIELDNAME);
    nameField.setId(IDTools.getID());
    nameField.setType(GRAPHQL_STRING_TYPENAME);
    nameField.setUnmodifiableRoles(allRoleStr);
    objectTypeSelf.getScalarFields().add(nameField);
    ScalarFieldInfo descriptionField = new ScalarFieldInfo();
    descriptionField.setId(IDTools.getID());
    descriptionField.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    descriptionField.setType(GRAPHQL_STRING_TYPENAME);
    descriptionField.setDefaultValue("");
    objectTypeSelf.getScalarFields().add(descriptionField);
    ScalarFieldInfo readConstraintField = new ScalarFieldInfo();
    readConstraintField.setId(IDTools.getID());
    readConstraintField.setName("read_constraints");
    readConstraintField.setType(GRAPHQL_OBJECT_TYPENAME);
    readConstraintField.setDefaultValue(null);
    objectTypeSelf.getScalarFields().add(readConstraintField);
    ScalarFieldInfo updateConstraintField = new ScalarFieldInfo();
    updateConstraintField.setId(IDTools.getID());
    updateConstraintField.setName("update_constraints");
    updateConstraintField.setType(GRAPHQL_OBJECT_TYPENAME);
    updateConstraintField.setDefaultValue(null);
    objectTypeSelf.getScalarFields().add(updateConstraintField);
    ScalarFieldInfo deleteConstraintField = new ScalarFieldInfo();
    deleteConstraintField.setId(IDTools.getID());
    deleteConstraintField.setName("delete_constraints");
    deleteConstraintField.setType(GRAPHQL_OBJECT_TYPENAME);
    deleteConstraintField.setDefaultValue(null);
    objectTypeSelf.getScalarFields().add(deleteConstraintField);
    ScalarFieldInfo unReadableRoles = new ScalarFieldInfo();
    unReadableRoles.setId(IDTools.getID());
    unReadableRoles.setList(true);
    unReadableRoles.setDescription("不可阅读的角色列表");
    unReadableRoles.setName("unreadable_roles");
    unReadableRoles.setType(GRAPHQL_STRING_TYPENAME);
    unReadableRoles.setDefaultValue(new ArrayList<>());
    objectTypeSelf.getScalarFields().add(unReadableRoles);
    ScalarFieldInfo unInsertableRoles = new ScalarFieldInfo();
    unInsertableRoles.setId(IDTools.getID());
    unInsertableRoles.setList(true);
    unInsertableRoles.setDescription("不可插入的角色列表");
    unInsertableRoles.setName("uninsertable_roles");
    unInsertableRoles.setType(GRAPHQL_STRING_TYPENAME);
    unInsertableRoles.setDefaultValue(new ArrayList<>());
    objectTypeSelf.getScalarFields().add(unInsertableRoles);
    ScalarFieldInfo unDeletableRoles = new ScalarFieldInfo();
    unDeletableRoles.setId(IDTools.getID());
    unDeletableRoles.setList(true);
    unDeletableRoles.setDescription("不可删除的角色列表");
    unDeletableRoles.setName(GRAPHQL_UNDELETABLE_ROLES_FIELDNAME);
    unDeletableRoles.setType(GRAPHQL_STRING_TYPENAME);
    unDeletableRoles.setDefaultValue(new ArrayList<>());
    objectTypeSelf.getScalarFields().add(unDeletableRoles);
    ScalarFieldInfo unUpdatableRoles = new ScalarFieldInfo();
    unUpdatableRoles.setId(IDTools.getID());
    unUpdatableRoles.setList(true);
    unUpdatableRoles.setDescription("不可更新的角色列表");
    unUpdatableRoles.setName(GRAPHQL_UNUPDATEBLE_ROLES_FIELDNAME);
    unUpdatableRoles.setType(GRAPHQL_STRING_TYPENAME);
    unUpdatableRoles.setDefaultValue(new ArrayList<>());
    objectTypeSelf.getScalarFields().add(unUpdatableRoles);
    RelationField scalarFields = new RelationField();
    scalarFields.setRelationType(GRAPHQL_ONE2MANY_NAME);
    scalarFields.setFromObject(GRAPHQL_CONTENTTYPE_TYPENAME);
    scalarFields.setFromField(GRAPHQL_SCALARFIELD_FIELDNAME);
    scalarFields.setToObject(GRAPHQL_SCALARFIELD_TYPENAME);
    scalarFields.setToField(GRAPHQL_CONTENTTYPE_FIELDNAME);
    scalarFields.setIfCascade(true);
    scalarFields.setId(IDTools.getID());
    schemaDBOBJ.getRelations().add(scalarFields);
    RelationField enumFieldsDef = new RelationField();
    enumFieldsDef.setId(IDTools.getID());
    enumFieldsDef.setFromObject(GRAPHQL_CONTENTTYPE_TYPENAME);
    enumFieldsDef.setFromField(GRAPHQL_ENUMFIELD_FIELDNAME);
    enumFieldsDef.setToObject(GRAPHQL_ENUMFIELD_TYPENAME);
    enumFieldsDef.setToField(GRAPHQL_CONTENTTYPE_FIELDNAME);
    enumFieldsDef.setRelationType(GRAPHQL_ONE2MANY_NAME);
    enumFieldsDef.setIfCascade(true);
    schemaDBOBJ.getRelations().add(enumFieldsDef);
    RelationField unique_constraintsField = new RelationField();
    unique_constraintsField.setId(IDTools.getID());
    unique_constraintsField.setFromObject(GRAPHQL_CONTENTTYPE_TYPENAME);
    unique_constraintsField.setFromField(GRAPHQL_UNIQUE_CONSTRAINT_FIELDNAME);
    unique_constraintsField.setToObject(GRAPHQL_UNIQUECONSTRAINTS_TYPE_NAME);
    unique_constraintsField.setRelationType(GRAPHQL_ONE2MANY_NAME);
    unique_constraintsField.setToField(GRAPHQL_CONTENTTYPE_FIELDNAME);
    unique_constraintsField.setIfCascade(true);
    schemaDBOBJ.getRelations().add(unique_constraintsField);
    schemaDBOBJ.getObjecttypes().add(objectTypeSelf);
    // UniqueConstraints
    ObjectTypeInfo uniqueConstraints = new ObjectTypeInfo();
    uniqueConstraints.setId(GRAPHQL_UNIQUECONSTRAINTS_TYPE_ID);
    uniqueConstraints.setName(GRAPHQL_UNIQUECONSTRAINTS_TYPE_NAME);
    ScalarFieldInfo fields = new ScalarFieldInfo();
    fields.setName(GRAPHQL_FIELDS_FIELDNAME);
    fields.setId(IDTools.getID());
    fields.setType(GRAPHQL_STRING_TYPENAME);
    fields.setList(true);
    uniqueConstraints.getScalarFields().add(getIDField());
    uniqueConstraints.getScalarFields().add(fields);
    schemaDBOBJ.getObjecttypes().add(uniqueConstraints);
    // EnumTypes
    ObjectTypeInfo enumTypes = new ObjectTypeInfo();
    enumTypes.setId(GRAPHQL_ENUMTYPE_TYPE_ID);
    enumTypes.setName(GRAPHQL_ENUMTYPE_TYPENAME);
    enumTypes.getScalarFields().add(getIDField());
    enumTypes.getScalarFields().add(getNameField());
    RelationField valuesField = new RelationField();
    valuesField.setId(IDTools.getID());
    valuesField.setFromObject(GRAPHQL_ENUMTYPE_TYPENAME);
    valuesField.setFromField(GRAPHQL_VALUES_FIELDNAME);
    valuesField.setToObject(GRAPHQL_ENUMELEMENT_NAME);
    valuesField.setToField(GRAPHQL_ENUM_FIELDNAME);
    valuesField.setRelationType(GRAPHQL_ONE2MANY_NAME);
    valuesField.setIfCascade(true);
    ScalarFieldInfo enumTypeDescriptionField = new ScalarFieldInfo();
    enumTypeDescriptionField.setId(IDTools.getID());
    enumTypeDescriptionField.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    enumTypeDescriptionField.setDefaultValue("");
    enumTypeDescriptionField.setType(GRAPHQL_STRING_TYPENAME);
    schemaDBOBJ.getRelations().add(valuesField);
    enumTypes.getScalarFields().add(enumTypeDescriptionField);
    schemaDBOBJ.getObjecttypes().add(enumTypes);
    // EnumElement
    ObjectTypeInfo enumElement = new ObjectTypeInfo();
    enumElement.setId(GRAPHQL_ENUMELEMENT_ID);
    enumElement.setName(GRAPHQL_ENUMELEMENT_NAME);
    enumElement.getScalarFields().add(getIDField());
    ScalarFieldInfo valueField = new ScalarFieldInfo();
    valueField.setName(GRAPHQL_VALUE_FIELDNAME);
    valueField.setId(IDTools.getID());
    valueField.setType(GRAPHQL_STRING_TYPENAME);
    valueField.setNotNull(true);
    enumElement.getScalarFields().add(valueField);
    ScalarFieldInfo enumDescriptionField = new ScalarFieldInfo();
    enumDescriptionField.setId(IDTools.getID());
    enumDescriptionField.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    enumDescriptionField.setDefaultValue("");
    enumDescriptionField.setType(GRAPHQL_STRING_TYPENAME);
    enumElement.getScalarFields().add(enumDescriptionField);
    schemaDBOBJ.getObjecttypes().add(enumElement);

    ObjectTypeInfo trigger_Type = new ObjectTypeInfo();
    trigger_Type.setId(IDTools.getID());
    trigger_Type.setDescription("触发器");
    trigger_Type.setName(GRAPHQL_TRIGGER_TYPENAME);
    List<ScalarFieldInfo> trigger_scalarTypes = new ArrayList<>();
    trigger_scalarTypes.add(getIDField());
    trigger_scalarTypes.add(getNameField());
    ScalarFieldInfo triggerType = new ScalarFieldInfo();
    triggerType.setName(GRAPHQL_TYPENAME_FIELDNAME);
    triggerType.setNotNull(true);
    triggerType.setDescription("触发器对象类型");
    triggerType.setType(GRAPHQL_STRING_TYPENAME);
    trigger_scalarTypes.add(triggerType);
    ScalarFieldInfo trigger_Headers = new ScalarFieldInfo();
    trigger_Headers.setName(GRAPHQL_HEADERS_FIELDNAME);
    trigger_Headers.setType(GRAPHQL_OBJECT_TYPENAME);
    trigger_scalarTypes.add(trigger_Headers);
    ScalarFieldInfo trigger_ok_status = new ScalarFieldInfo();
    trigger_ok_status.setName(GRAPHQL_OK_STATUS_FIELDNAME);
    trigger_ok_status.setDefaultValue("200");
    trigger_ok_status.setType(GRAPHQL_STRING_TYPENAME);
    trigger_scalarTypes.add(trigger_ok_status);
    ScalarFieldInfo trigger_payload = new ScalarFieldInfo();
    trigger_payload.setType(GRAPHQL_STRING_TYPENAME);
    trigger_payload.setName(GRAPHQL_PAYLOADFORMATTER_FIELDNAME);
    trigger_scalarTypes.add(trigger_payload);
    ScalarFieldInfo trigger_payloadArg= new ScalarFieldInfo();
    trigger_payloadArg.setType(GRAPHQL_STRING_TYPENAME);
    trigger_payloadArg.setList(true);
    trigger_payloadArg.setName(GRAPHQL_PAYLOADARGS_FIELDNAME);
    trigger_scalarTypes.add(trigger_payloadArg);
    ScalarFieldInfo trigger_retrytimes = new ScalarFieldInfo();
    trigger_retrytimes.setName(GRAPHQL_RETRY_TIMES_FIELDNAME);
    trigger_retrytimes.setType(GRAPHQL_INT_TYPENAME);
    trigger_retrytimes.setDefaultValue(3);
    trigger_scalarTypes.add(trigger_retrytimes);
    ScalarFieldInfo triggerWebhookURL = new ScalarFieldInfo();
    triggerWebhookURL.setName(GRAPHQL_WEBHOOK_URL_FIELDNAME);
    triggerWebhookURL.setType(GRAPHQL_STRING_TYPENAME);
    triggerWebhookURL.setNotNull(true);
    trigger_scalarTypes.add(triggerWebhookURL);
    ScalarFieldInfo triggerStartDate = new ScalarFieldInfo();
    triggerStartDate.setName(GRAPHQL_STARTDATE_FIELDNAME);
    triggerStartDate.setNotNull(true);
    triggerStartDate.setType(GRAPHQL_DATE_TYPENAME);
    trigger_scalarTypes.add(triggerStartDate);
    ScalarFieldInfo triggerExpireDate = new ScalarFieldInfo();
    triggerExpireDate.setName(GRAPHQL_EXPIREDATE_FIELDNAME);
    triggerExpireDate.setType(GRAPHQL_DATE_TYPENAME);
    triggerExpireDate.setNotNull(true);
    trigger_scalarTypes.add(triggerExpireDate);
    ScalarFieldInfo triggerDescription = new ScalarFieldInfo();
    triggerDescription.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    triggerDescription.setType(GRAPHQL_STRING_TYPENAME);
    triggerDescription.setDefaultValue("");
    trigger_scalarTypes.add(triggerDescription);
    trigger_Type.setScalarFields(trigger_scalarTypes);
    List<EnumField> trigger_EnumList = new ArrayList<>();
    EnumField  eventTypeList = new EnumField();
    eventTypeList.setId(IDTools.getID());
    eventTypeList.setName(GRAPHQL_EVENTTYPE_FIELDNAME);
    eventTypeList.setList(true);
    eventTypeList.setType(GRAPHQL_EVENT_TYPENAME);
    trigger_EnumList.add(eventTypeList);
    trigger_Type.setEnumFields(trigger_EnumList);
    schemaDBOBJ.getObjecttypes().add(trigger_Type);

    // User  Schemadb的用户管理
    ObjectTypeInfo userType = new ObjectTypeInfo();
    userType.setId(GRAPHQL_USERTYPE_ID);
    userType.setName(GRAPHQL_USER_TYPENAME);
    userType.getScalarFields().add(getIDField());
    ScalarFieldInfo user_username = new ScalarFieldInfo();
    user_username.setId(IDTools.getID());
    user_username.setName(GRAPHQL_USERNAME_FIELD);
    user_username.setType(GRAPHQL_STRING_TYPENAME);
    user_username.setNotNull(true);
    userType.getScalarFields().add(user_username);
    ScalarFieldInfo user_password = new ScalarFieldInfo();
    user_password.setId(IDTools.getID());
    user_password.setName(GRAPHQL_PASSWORD_FIELD);
    user_password.setType(GRAPHQL_STRING_TYPENAME);
    user_password.setNotNull(true);
    userType.getScalarFields().add(user_password);
    EnumField user_rolefield = new EnumField();
    user_rolefield.setId(IDTools.getID());
    user_rolefield.setName(GRAPHQL_ROLE_FIELDNAME);
    user_rolefield.setType(GRAPHQL_ROLE_ENUMNAME);
    user_rolefield.setNotNull(true);
    userType.getEnumFields().add(user_rolefield);
    UniqueConstraint userNameUnique = new UniqueConstraint();
    userNameUnique.setConstraintName("usernameUnique");
    List<String> userNameUniqueFields = new ArrayList<>();
    userNameUniqueFields.add(GRAPHQL_USERNAME_FIELD);
    userNameUnique.setFieldNames(userNameUniqueFields);
    userType.getUniqueConstraints().add(userNameUnique);
    schemaDBOBJ.getObjecttypes().add(userType);
    // ScalarField
    ObjectTypeInfo scalarFieldType = new ObjectTypeInfo();
    scalarFieldType.setId(GRAPHQL_SCALARFIELD_TYPE_ID);
    scalarFieldType.setName(GRAPHQL_SCALARFIELD_TYPENAME);
    scalarFieldType.getScalarFields().add(getIDField());
    ScalarFieldInfo scalarFieldName = getNameField();
    scalarFieldName.setUnmodifiableRoles(allRoleStr);
    scalarFieldType.getScalarFields().add(scalarFieldName);
    ScalarFieldInfo notNullField = new ScalarFieldInfo();
    notNullField.setId(IDTools.getID());
    notNullField.setName(GRAPHQL_NOTNULL_FIELDNAME);
    notNullField.setType(GRAPHQL_BOOLEAN_TYPENAME);
    notNullField.setDefaultValue(false);
    scalarFieldType.getScalarFields().add(notNullField);
    ScalarFieldInfo isListField = new ScalarFieldInfo();
    isListField.setId(IDTools.getID());
    isListField.setName(GRAPHQL_ISLIST_FIELDNAME);
    isListField.setType(GRAPHQL_BOOLEAN_TYPENAME);
    isListField.setDefaultValue(false);
    scalarFieldType.getScalarFields().add(isListField);
    ScalarFieldInfo scalarfield_description = new ScalarFieldInfo();
    scalarfield_description.setId(IDTools.getID());
    scalarfield_description.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    scalarfield_description.setType(GRAPHQL_STRING_TYPENAME);
    scalarfield_description.setDefaultValue("");
    scalarFieldType.getScalarFields().add(scalarfield_description);
    ScalarFieldInfo scalarfield_defaultvalue = new ScalarFieldInfo();
    scalarfield_defaultvalue.setId(IDTools.getID());
    scalarfield_defaultvalue.setName(GRAPHQL_DEFAULTVALUE_FIELDNAME);
    scalarfield_defaultvalue.setType(GRAPHQL_STRING_TYPENAME);
    scalarFieldType.getScalarFields().add(scalarfield_defaultvalue);
    EnumField typeFieldInfo = new EnumField();
    typeFieldInfo.setId(IDTools.getID());
    typeFieldInfo.setNotNull(true);
    typeFieldInfo.setName(GRAPHQL_TYPE_FIELDNAME);
    typeFieldInfo.setType(GRAPHQL_SCALAR_ENUM_NAME);
    typeFieldInfo.setUnmodifiableRoles(allRoleStr);
    scalarFieldType.getEnumFields().add(typeFieldInfo);
    ScalarFieldInfo scalarfield_invisible_roles = new ScalarFieldInfo();
    scalarfield_invisible_roles.setId(IDTools.getID());
    scalarfield_invisible_roles.setName(GRAPHQL_INVISIBLE_ROLES_FIELDNAME);
    scalarfield_invisible_roles.setType(GRAPHQL_STRING_TYPENAME);
    scalarfield_invisible_roles.setList(true);
    scalarFieldType.getScalarFields().add(scalarfield_invisible_roles);
    ScalarFieldInfo scalarfield_unmodifiable_roles = new ScalarFieldInfo();
    scalarfield_unmodifiable_roles.setId(IDTools.getID());
    scalarfield_unmodifiable_roles.setName(GRAPHQL_UNMODIFIABLE_ROLES_FIELDNAME);
    scalarfield_unmodifiable_roles.setType(GRAPHQL_STRING_TYPENAME);
    scalarfield_unmodifiable_roles.setList(true);
    scalarFieldType.getScalarFields().add(scalarfield_unmodifiable_roles);
    schemaDBOBJ.getObjecttypes().add(scalarFieldType);

    // EnumField
    ObjectTypeInfo enumFieldType = new ObjectTypeInfo();
    enumFieldType.setId(GRAPHQL_ENUMFIELD_ID);
    enumFieldType.setName(GRAPHQL_ENUMFIELD_TYPENAME);
    enumFieldType.getScalarFields().add(getIDField());
    ScalarFieldInfo enumFieldName = getNameField();
    enumFieldName.setUnmodifiableRoles(allRoleStr);
    enumFieldType.getScalarFields().add(enumFieldName);
    ScalarFieldInfo enumField_islist = new ScalarFieldInfo();
    enumField_islist.setId(IDTools.getID());
    enumField_islist.setName(GRAPHQL_ISLIST_FIELDNAME);
    enumField_islist.setType(GRAPHQL_BOOLEAN_TYPENAME);
    enumField_islist.setDefaultValue(false);
    enumFieldType.getScalarFields().add(enumField_islist);
    ScalarFieldInfo enumfield_defaultvalue = new ScalarFieldInfo();
    enumfield_defaultvalue.setId(IDTools.getID());
    enumfield_defaultvalue.setName(GRAPHQL_DEFAULTVALUE_FIELDNAME);
    enumfield_defaultvalue.setType(GRAPHQL_OBJECT_TYPENAME);
    enumFieldType.getScalarFields().add(enumfield_defaultvalue);
    ScalarFieldInfo enumField_type = new ScalarFieldInfo();
    enumField_type.setId(IDTools.getID());
    enumField_type.setName(GRAPHQL_TYPE_FIELDNAME);
    enumField_type.setType(GRAPHQL_STRING_TYPENAME);
    enumField_type.setNotNull(true);
    enumFieldType.getScalarFields().add(enumField_type);
    ScalarFieldInfo scalarField_invisible_roles = new ScalarFieldInfo();
    scalarField_invisible_roles.setId(IDTools.getID());
    scalarField_invisible_roles.setName(GRAPHQL_INVISIBLE_ROLES_FIELDNAME);
    scalarField_invisible_roles.setType(GRAPHQL_STRING_TYPENAME);
    scalarField_invisible_roles.setList(true);
    enumFieldType.getScalarFields().add(scalarField_invisible_roles);
    ScalarFieldInfo scalarField_unmodifiable_roles = new ScalarFieldInfo();
    scalarField_unmodifiable_roles.setId(IDTools.getID());
    scalarField_unmodifiable_roles.setName(GRAPHQL_UNMODIFIABLE_ROLES_FIELDNAME);
    scalarField_unmodifiable_roles.setType(GRAPHQL_STRING_TYPENAME);
    scalarField_unmodifiable_roles.setList(true);
    enumFieldType.getScalarFields().add(scalarField_unmodifiable_roles);
    schemaDBOBJ.getObjecttypes().add(enumFieldType);

    // RelationField
    ObjectTypeInfo relationFieldObject = new ObjectTypeInfo();
    relationFieldObject.setName(GRAPHQL_RELATIONFIELD_TYPENAME);
    relationFieldObject.setId(IDTools.getID());
    relationFieldObject.getScalarFields().add(getIDField());
    ScalarFieldInfo fromObjectField = new ScalarFieldInfo();
    fromObjectField.setNotNull(true);
    fromObjectField.setName(GRAPHQL_FROMOBJECT_FIELDNAME);
    fromObjectField.setType(GRAPHQL_STRING_TYPENAME);
    fromObjectField.setDescription("左连接类型名");
    fromObjectField.setUnmodifiableRoles(allRoleStr);
    relationFieldObject.getScalarFields().add(fromObjectField);
    ScalarFieldInfo fromFieldField = new ScalarFieldInfo();
    fromFieldField.setNotNull(true);
    fromFieldField.setName(GRAPHQL_FROMFIELD_FIELDNAME);
    fromFieldField.setType(GRAPHQL_STRING_TYPENAME);
    fromFieldField.setDescription("左连接字段名");
    fromFieldField.setUnmodifiableRoles(allRoleStr);
    relationFieldObject.getScalarFields().add(fromFieldField);
    ScalarFieldInfo toObjectField = new ScalarFieldInfo();
    toObjectField.setDescription("右连接类型名");
    toObjectField.setType(GRAPHQL_STRING_TYPENAME);
    toObjectField.setName(GRAPHQL_TOOBJECT_FIELDNAME);
    toObjectField.setNotNull(true);
    toObjectField.setUnmodifiableRoles(allRoleStr);
    relationFieldObject.getScalarFields().add(toObjectField);
    ScalarFieldInfo toFieldField = new ScalarFieldInfo();
    toFieldField.setDescription("右连接字段名");
    toFieldField.setNotNull(true);
    toFieldField.setName(GRAPHQL_TOFIELD_FIELDNAME);
    toFieldField.setType(GRAPHQL_STRING_TYPENAME);
    toFieldField.setUnmodifiableRoles(allRoleStr);
    relationFieldObject.getScalarFields().add(toFieldField);
    EnumField relation_kind = new EnumField();
    relation_kind.setId(IDTools.getID());
    relation_kind.setNotNull(true);
    relation_kind.setName(GRAPHQL_RELATIONTYPE_FIELDNAME);
    relation_kind.setDescription("关联关系种类");
    relation_kind.setType(GRAPHQL_RELATIONTYPE_ENUMNAME);
    relationFieldObject.getEnumFields().add(relation_kind);
    ScalarFieldInfo ifCasacade = new ScalarFieldInfo();
    ifCasacade.setName(GRAPHQL_IF_CASCADE_FIELDNAME);
    ifCasacade.setType(GRAPHQL_BOOLEAN_TYPENAME);
    ifCasacade.setDescription("是否进行级联删除");
    ifCasacade.setDefaultValue(false);
    relationFieldObject.getScalarFields().add(ifCasacade);
    ScalarFieldInfo relationfield_invisible_roles = new ScalarFieldInfo();
    relationfield_invisible_roles.setId(IDTools.getID());
    relationfield_invisible_roles.setName(GRAPHQL_INVISIBLE_ROLES_FIELDNAME);
    relationfield_invisible_roles.setType(GRAPHQL_STRING_TYPENAME);
    relationfield_invisible_roles.setList(true);
    relationFieldObject.getScalarFields().add(relationfield_invisible_roles);
    ScalarFieldInfo relationfield_unmodifiable_roles = new ScalarFieldInfo();
    relationfield_unmodifiable_roles.setId(IDTools.getID());
    relationfield_unmodifiable_roles.setName(GRAPHQL_UNMODIFIABLE_ROLES_FIELDNAME);
    relationfield_unmodifiable_roles.setType(GRAPHQL_STRING_TYPENAME);
    relationfield_unmodifiable_roles.setList(true);
    relationFieldObject.getScalarFields().add(relationfield_unmodifiable_roles);
    schemaDBOBJ.getObjecttypes().add(relationFieldObject);

    //RemoteEndPoint
    ObjectTypeInfo remoteEndPoint = new ObjectTypeInfo();
    remoteEndPoint.setName(GRAPHQL_REMOTE_ENDPOINT_TYPENAME);
    remoteEndPoint.setId(IDTools.getID());
    remoteEndPoint.getScalarFields().add(getIDField());
    remoteEndPoint.getScalarFields().add(getNameField());
    ScalarFieldInfo remote_endpoint=new ScalarFieldInfo();
    remote_endpoint.setType(GRAPHQL_URL_TYPENAME);
    remote_endpoint.setName(GRAPHQL_ENDPOINT_FIELDNAME);
    remote_endpoint.setNotNull(true);
    remoteEndPoint.getScalarFields().add(remote_endpoint);
    ScalarFieldInfo headers = new ScalarFieldInfo();
    headers.setType(GRAPHQL_OBJECT_TYPENAME);
    headers.setName(GRAPHQL_HEADERS_FIELDNAME);
    remoteEndPoint.getScalarFields().add(headers);
    ScalarFieldInfo remote_endpoint_description = new ScalarFieldInfo();
    remote_endpoint_description.setId(IDTools.getID());
    remote_endpoint_description.setName(GRAPHQL_DESCRIPTION_FIELDNAME);
    remote_endpoint_description.setType(GRAPHQL_STRING_TYPENAME);
    remote_endpoint_description.setDefaultValue("");
    remoteEndPoint.getScalarFields().add(remote_endpoint_description);
    RelationField enabledapis=new RelationField();
    enabledapis.setRelationType(GRAPHQL_ONE2MANY_NAME);
    enabledapis.setIfCascade(true);
    enabledapis.setFromObject(GRAPHQL_REMOTE_ENDPOINT_TYPENAME);
    enabledapis.setFromField(GRAPHQL_ENABLE_API_FIELDNAME);
    enabledapis.setToObject(GRAPHQL_REMOTE_API_TYPENAME);
    enabledapis.setToField(GRAPHQL_ENDPOINT_FIELDNAME);
    schemaDBOBJ.getRelations().add(enabledapis);
    schemaDBOBJ.getObjecttypes().add(remoteEndPoint);

    //RemoteAPI
    ObjectTypeInfo remoteAPI = new ObjectTypeInfo();
    remoteAPI.setId(IDTools.getID());
    remoteAPI.setName(GRAPHQL_REMOTE_API_TYPENAME);
    remoteAPI.getScalarFields().add(getIDField());
    ScalarFieldInfo remoteapi_apiname = new ScalarFieldInfo();
    remoteapi_apiname.setName(GRAPHQL_APINAME_FIELDNAME);
    remoteapi_apiname.setType(GRAPHQL_STRING_TYPENAME);
    remoteapi_apiname.setNotNull(true);
    remoteAPI.getScalarFields().add(remoteapi_apiname);
    ScalarFieldInfo remote_API_disabledRole = new ScalarFieldInfo();
    remote_API_disabledRole.setName(GRAPHQL_DISABLED_ROLES_FIELDNAME);
    remote_API_disabledRole.setType(GRAPHQL_STRING_TYPENAME);
    remote_API_disabledRole.setDescription("禁止调用的角色列表");
    remoteAPI.getScalarFields().add(remote_API_disabledRole);
    schemaDBOBJ.getObjecttypes().add(remoteAPI);
    // Enums
    EnumElement adminRole = new EnumElement();
    adminRole.setId(GRAPHQL_ADMIN_ID);
    adminRole.setValue(ROLE_ADMIN);
    adminRole.setDescription("管理员");

    EnumElement guestRole = new EnumElement();
    guestRole.setId(GRAPHQL_GUEST_ID);
    guestRole.setValue(ROLE_GUEST);
    guestRole.setDescription("游客");

    EnumElement defaultRole = new EnumElement();
    defaultRole.setId(IDTools.getID());
    defaultRole.setValue(ROLE_DEFAULT);
    defaultRole.setDescription("默认角色");
    List roles = new ArrayList();
    roles.add(guestRole);
    roles.add(adminRole);
    roles.add(defaultRole);
    EnumTypeMetaData roleEnum = new EnumTypeMetaData();
    roleEnum.setId(GRAPHQL_ROLE_DEFAULT_ID);
    roleEnum.setName(GRAPHQL_ROLE_TYPENAME);
    roleEnum.setValues(roles);
    schemaDBOBJ.getEnumtypes().add(roleEnum);

    // DataBaseKind

    List enumDatabaseKind = new ArrayList();
    List<String> supportedDBs = DaoFactory.supportedDataBase();
    for(String db:supportedDBs) {
      EnumElement databaseElement = new EnumElement();
      databaseElement.setId(IDTools.getID());
      databaseElement.setValue(db);
      databaseElement.setDescription("");
      enumDatabaseKind.add(databaseElement);
    }
    EnumTypeMetaData databaseEnums = new EnumTypeMetaData();
    databaseEnums.setId(GRAPHQL_DATABASEKIND_DEFAULT_ID);
    databaseEnums.setName(GRAPHQL_DATABASEKIND_ENUMNAME);
    databaseEnums.setValues(enumDatabaseKind);
    schemaDBOBJ.getEnumtypes().add(databaseEnums);
    // 指定枚举值，Schemab 本身不能修改他自身的枚举值
    // RelationType
    EnumTypeMetaData relationTypeEnums = new EnumTypeMetaData();
    relationTypeEnums.setId(IDTools.getID());
    relationTypeEnums.setName(GRAPHQL_RELATIONTYPE_ENUMNAME);
    EnumElement enumelement_one2one = new EnumElement();
    enumelement_one2one.setId(IDTools.getID());
    enumelement_one2one.setValue(GRAPHQL_ONE2ONE_NAME);
    enumelement_one2one.setDescription("1对1关系");
    EnumElement enumElement_one2many = new EnumElement();
    enumElement_one2many.setId(IDTools.getID());
    enumElement_one2many.setValue(GRAPHQL_ONE2MANY_NAME);
    enumElement_one2many.setDescription("1对多关系");
    EnumElement enumElement_many2one = new EnumElement();
    enumElement_many2one.setId(IDTools.getID());
    enumElement_many2one.setValue(GRAPHQL_MANY2ONE_NAME);
    enumElement_many2one.setDescription("多对1关系");
    EnumElement enumElement_many2many = new EnumElement();
    enumElement_many2many.setId(IDTools.getID());
    enumElement_many2many.setValue(GRAPHQL_MANY2MANY_NAME);
    enumElement_many2many.setDescription("多对多关系");
    List relationtypeList = new ArrayList();
    relationtypeList.add(enumelement_one2one);
    relationtypeList.add(enumElement_one2many);
    relationtypeList.add(enumElement_many2one);
    relationtypeList.add(enumElement_many2many);
    relationTypeEnums.setValues(relationtypeList);
    schemaDBOBJ.getEnumtypes().add(relationTypeEnums);
    //SchemaStatus
    EnumTypeMetaData schemaStatusEnums = new EnumTypeMetaData();
    schemaStatusEnums.setId(IDTools.getID());
    schemaStatusEnums.setName(GRAPHQL_SCHEMASTATUS_TYPENAME);
    List<EnumElement> statusElements = new ArrayList<>();
    EnumElement unInitializedStatusEnum = new EnumElement();
    unInitializedStatusEnum.setId(IDTools.getID());
    unInitializedStatusEnum.setValue(SCHEMA_STATUS_UNINITIALIZED);
    unInitializedStatusEnum.setDescription("unInitialized");
    statusElements.add(unInitializedStatusEnum);
    EnumElement startingStatusEnum = new EnumElement();
    startingStatusEnum.setId(IDTools.getID());
    startingStatusEnum.setValue(SCHEMA_STATUS_STARTTING);
    startingStatusEnum.setDescription("Starting");
    statusElements.add(startingStatusEnum);
    EnumElement runningStatusEnum = new EnumElement();
    runningStatusEnum.setId(IDTools.getID());
    runningStatusEnum.setValue(SCHEMA_STATUS_RUNNING);
    runningStatusEnum.setDescription("Running");
    statusElements.add(runningStatusEnum);
    EnumElement stoppingStatusEnum = new EnumElement();
    stoppingStatusEnum.setId(IDTools.getID());
    stoppingStatusEnum.setValue(SCHEMA_STATUS_STOPPING);
    stoppingStatusEnum.setDescription("Stopping");
    statusElements.add(stoppingStatusEnum);
    EnumElement stoppedStatusEnum = new EnumElement();
    stoppedStatusEnum.setId(IDTools.getID());
    stoppedStatusEnum.setValue(SCHEMA_STATUS_STOPPED);
    stoppedStatusEnum.setDescription("Stopped");
    statusElements.add(stoppedStatusEnum);
    schemaStatusEnums.setValues(statusElements);
    schemaDBOBJ.getEnumtypes().add(schemaStatusEnums);
    // ScalarType
    EnumElement intType = new EnumElement();
    intType.setId(GRAPHQL_INTTYPE_ID);
    intType.setValue(GRAPHQL_INT_TYPENAME);
    intType.setDescription("整形");
    EnumElement IDType = new EnumElement();
    IDType.setId(GRAPHQL_ID_TYPE_ID);
    IDType.setValue(GRAPHQL_ID_TYPENAME);
    IDType.setDescription("ID类型");
    EnumElement floatType = new EnumElement();
    floatType.setId(GRAPHQL_FLOATTYPE_ID);
    floatType.setValue(GRAPHQL_FLOAT_TYPENAME);
    floatType.setDescription("浮点型");
    EnumElement booleanType = new EnumElement();
    booleanType.setId(GRAPHQL_BOOLEAN_TYPE_ID);
    booleanType.setValue(GRAPHQL_BOOLEAN_TYPENAME);
    booleanType.setDescription("布尔型");
    EnumElement longType = new EnumElement();
    longType.setId(GRAPHQL_LONG_TYPE_ID);
    longType.setValue(GRAPHQL_LONG_TYPENAME);
    longType.setDescription("长整型");

    EnumElement byteType = new EnumElement();
    byteType.setId(GRAPHQL_BYTE_TYPE_ID);
    byteType.setValue(GRAPHQL_BYTE_TYPENAME);
    byteType.setDescription("字节型");

    EnumElement bigIntegerType = new EnumElement();
    bigIntegerType.setId(GRAPHQL_BIGINTEGER_TYPE_ID);
    bigIntegerType.setValue(GRAPHQL_BIGINTEGER_TYPENAME);
    bigIntegerType.setDescription("大整形");

    EnumElement bigDecimal = new EnumElement();
    bigDecimal.setId(GRAPHQL_BIGDECIMAL_TYPE_ID);
    bigDecimal.setValue(GRAPHQL_BIGDECIMAL_TYPENAME);
    bigDecimal.setDescription("数值型");
    EnumElement charType = new EnumElement();
    charType.setId(GRAPHQL_CHAR_TYPE_ID);
    charType.setValue(GRAPHQL_CHAR_TYPENAME);
    charType.setDescription("字符型");
    EnumElement objectType = new EnumElement();
    objectType.setId(GRAPHQL_object_TYPE_ID);
    objectType.setValue(GRAPHQL_OBJECT_TYPENAME);
    objectType.setDescription("对象型");

    EnumElement shortType = new EnumElement();
    shortType.setId(GRAPHQL_SHORT_TYPE_ID);
    shortType.setValue(GRAPHQL_SHORT_TYPENAME);
    shortType.setDescription("短整形");

    EnumElement jsonType = new EnumElement();
    jsonType.setId(GRAPHQL_JSON_TYPE_ID);
    jsonType.setValue(GRAPHQL_JSON_TYPENAME);
    jsonType.setDescription("JSON");

    EnumElement dateType = new EnumElement();
    dateType.setId(GRAPHQL_DATE_TYPE_ID);
    dateType.setValue(GRAPHQL_DATE_TYPENAME);
    dateType.setDescription("日期型");

    EnumElement datetimeType = new EnumElement();
    datetimeType.setId(GRAPHQL_DATETIME_TYPE_ID);
    datetimeType.setValue(GRAPHQL_DATETIME_TYPENAME);
    datetimeType.setDescription("日期时间型");

    EnumElement timeType = new EnumElement();
    timeType.setId(GRAPHQL_TIME_TYPE_ID);
    timeType.setValue(GRAPHQL_TIME_TYPENAME);
    timeType.setDescription("时间型");

    EnumElement emailType = new EnumElement();
    emailType.setId(GRAPHQL_EMAIL_TYPE_ID);
    emailType.setValue(GRAPHQL_EMAIL_TYPENAME);
    emailType.setDescription("邮箱");

    EnumElement lastUpadteType = new EnumElement();
    lastUpadteType.setId(GRAPHQL_LASTUPDATE_TYPE_ID);
    lastUpadteType.setValue(GRAPHQL_LASTUPDATE_TYPENAME);
    lastUpadteType.setDescription("最新更新时间");

    EnumElement createdateType = new EnumElement();
    createdateType.setId(GRAPHQL_CREATEDAT_TYPE_ID);
    createdateType.setValue(GRAPHQL_CREATEDAT_TYPENAME);
    createdateType.setDescription("最新更新时间");

    EnumElement urlType = new EnumElement();
    urlType.setId(GRAPHQL_URL_TYPE_ID);
    urlType.setValue(GRAPHQL_URL_TYPENAME);
    urlType.setDescription("URL");
    EnumElement stringType = new EnumElement();
    stringType.setId(GRAPHQL_STRING_TYPE_ID);
    stringType.setValue(GRAPHQL_STRING_TYPENAME);
    stringType.setDescription("String");
    List typeEnums = new ArrayList();
    typeEnums.add(intType);
    typeEnums.add(IDType);
    typeEnums.add(floatType);
    typeEnums.add(booleanType);
    typeEnums.add(longType);
    typeEnums.add(byteType);
    typeEnums.add(bigIntegerType);
    typeEnums.add(bigDecimal);
    typeEnums.add(charType);
    typeEnums.add(objectType);
    typeEnums.add(shortType);
    typeEnums.add(jsonType);
    typeEnums.add(dateType);
    typeEnums.add(datetimeType);
    typeEnums.add(timeType);
    typeEnums.add(emailType);
    typeEnums.add(lastUpadteType);
    typeEnums.add(createdateType);
    typeEnums.add(urlType);
    typeEnums.add(stringType);
    EnumTypeMetaData scalarTypes = new EnumTypeMetaData();
    scalarTypes.setId(GRAPHQL_SCALAR_ENUM_ID);
    scalarTypes.setName(GRAPHQL_SCALAR_ENUM_NAME);
    scalarTypes.setValues(typeEnums);
    schemaDBOBJ.getEnumtypes().add(scalarTypes);
    // RelationKind
    EnumElement one2one = new EnumElement();
    one2one.setId(GRAPHQL_ONE2ONE_ID);
    one2one.setValue(GRAPHQL_ONE2ONE_NAME);
    one2one.setDescription("1对1关系");
    EnumElement one2many = new EnumElement();
    one2many.setId(GRAPHQL_ONE2MANY_ID);
    one2many.setValue(GRAPHQL_ONE2MANY_NAME);
    one2many.setDescription("1对多关系");
    EnumElement many2one = new EnumElement();
    many2one.setId(GRAPHQL_MANY2ONE_ID);
    many2one.setValue(GRAPHQL_MANY2ONE_NAME);
    many2one.setDescription("多对1关系");
    EnumElement many2many = new EnumElement();
    many2many.setId(GRAPHQL_MANY2MANY_ID);
    many2many.setValue(GRAPHQL_MANY2MANY_NAME);
    many2many.setDescription("多对多关系");
    List enumRelations = new ArrayList();
    enumRelations.add(one2one);
    enumRelations.add(one2many);
    enumRelations.add(many2one);
    enumRelations.add(many2many);
    EnumTypeMetaData relationKinds = new EnumTypeMetaData();
    relationKinds.setId(GRAPHQL_RELATIONKIND_ID);
    relationKinds.setName(GRAPHQL_RELATIONKIND_ENUM_NAME);
    relationKinds.setValues(enumRelations);
    schemaDBOBJ.getEnumtypes().add(relationKinds);


    //EventType
    EnumTypeMetaData enumEventTypes = new EnumTypeMetaData();
    enumEventTypes.setName(GRAPHQL_EVENT_TYPENAME);
    List<EnumElement> eventEnumList = new ArrayList<>();
    EnumElement eventtype_insert = new EnumElement();
    eventtype_insert.setValue(GRAPHQL_EVENT_INSERT);
    eventEnumList.add(eventtype_insert);
    EnumElement eventtype_delete = new EnumElement();
    eventtype_delete.setValue(GRAPHQL_EVENT_DELETE);
    eventEnumList.add(eventtype_delete);
    EnumElement eventtype_update = new EnumElement();
    eventtype_update.setValue(GRAPHQL_EVENT_UPDATE);
    eventEnumList.add(eventtype_update);
    enumEventTypes.setValues(eventEnumList);
    schemaDBOBJ.getEnumtypes().add(enumEventTypes);
    //ThirdAPIQuery
    ThirdPartAPIMetaData thirdAPIMany = new ThirdPartAPIMetaData();
    thirdAPIMany.setApiName("ThirdAPIMany");
    thirdAPIMany.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(thirdAPIMany);
    // 增加SchemaRestart的ThirdAPI
    ThirdPartAPIMetaData schemaRestart = new ThirdPartAPIMetaData();
    schemaRestart.setApiName("SchemaRestart");
    schemaRestart.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(schemaRestart);
    // 增加SchemaPublish的ThirdAPI
    ThirdPartAPIMetaData schemaPublish = new ThirdPartAPIMetaData();
    schemaPublish.setApiName("SchemaPublish");
    schemaPublish.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(schemaPublish);
    // 增加SchemaStart的ThirdAPI
    ThirdPartAPIMetaData schemaStart = new ThirdPartAPIMetaData();
    schemaStart.setApiName("SchemaStart");
    schemaStart.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(schemaStart);
    // 增加SchemaStop的ThirdAPI
    ThirdPartAPIMetaData schemaStop = new ThirdPartAPIMetaData();
    schemaStop.setApiName("SchemaStop");
    schemaStop.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(schemaStop);
    ThirdPartAPIMetaData schemaCreate = new ThirdPartAPIMetaData();
    schemaCreate.setApiName("SchemaCreate");
    schemaCreate.getDisabledRoles().add(ROLE_GUEST);
    schemaDBOBJ.getThirdapis().add(schemaCreate);
    ThirdPartAPIMetaData remoteSchemaLoad = new ThirdPartAPIMetaData();
    remoteSchemaLoad.setApiName("RemoteSchemaLoad");
    return schemaDBOBJ;
  }

  /**
   * @param schemaID
   * @param typeName
   * @param filterValue
   * @return
   */
  public CompletableFuture<Object> FilterCheck(
      String schemaID, String typeName, HashMap filterValue) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          if (null == schemaID || schemaID.trim().equalsIgnoreCase("")) {
            FilterResult filterResult = new FilterResult();
            filterResult.setValid(false);
            filterResult.setHints("Schema ID 不能为空！！");
            future.complete(filterResult);
          } else if (null == typeName || typeName.trim().equalsIgnoreCase("")) {
            FilterResult filterResult = new FilterResult();
            filterResult.setValid(false);
            filterResult.setHints("Type Name 不能为空！！");
            future.complete(filterResult);
          } else if (null == filterValue || filterValue.size() == 0) {
            FilterResult filterResult = new FilterResult();
            filterResult.setValid(false);
            filterResult.setHints("过滤参数不能为空！！");
            future.complete(filterResult);
          } else {
            future.complete(true);
          }
        });
    return future;
  }



  public static ObjectTypeInfo getObjectType() {
    ObjectTypeInfo objectTypeInfo = new ObjectTypeInfo();
    objectTypeInfo.setId(IDTools.getID());
    return objectTypeInfo;
  }

  /** @return */
  public static ScalarFieldInfo getIDField() {
    ScalarFieldInfo idField = new ScalarFieldInfo();
    idField.setId(IDTools.getID());
    idField.setName(GRAPHQL_ID_FIELDNAME);
    idField.setType(GRAPHQL_ID_TYPENAME);
    idField.setDescription("唯一标识");
    idField.setNotNull(true);
    idField.setFieldKind("SystemDefined");
    return idField;
  }

  /** @return */
  public static ScalarFieldInfo getNameField() {
    ScalarFieldInfo nameField = new ScalarFieldInfo();
    nameField.setId(IDTools.getID());
    nameField.setName(GRAPHQL_NAME_FIELDNAME);
    nameField.setType(GRAPHQL_STRING_TYPENAME);
    nameField.setDescription("名称");
    nameField.setNotNull(true);
    return nameField;
  }
}

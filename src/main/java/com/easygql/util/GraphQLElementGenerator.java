package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.BusinessException;
import com.easygql.thirdapis.*;
import graphql.Assert;
import graphql.Scalars;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.SchemaPrinter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static com.easygql.component.ConfigurationProperties.*;
import static com.easygql.util.RemoteSchemaLoader.createTypeDefinition;
import static com.easygql.util.Validator.isValidTypeName;

/**
 * @author guofen
 * @date 2019/11/19 17:04
 */
@Slf4j
public class GraphQLElementGenerator {
  private static GraphQLEnumType conflictEnum =
      GraphQLEnumType.newEnum()
          .name(GRAPHQL_CONFLICT_TYPENAME + GRAPHQL_ENUM_POSTFIX)
          .value(GRAPHQL_CONFLICT_ERROR)
          .value(GRAPHQL_CONFLICT_REPLACE)
          .build();
  private static GraphQLArgument conflictArgument =
      GraphQLArgument.newArgument()
          .name(GRAPHQL_CONFLICT_ARGUMENT)
          .type(conflictEnum)
          .defaultValue(GRAPHQL_CONFLICT_REPLACE)
          .build();
  private static Pattern urlPattern =
      Pattern.compile(
          "^(http:\\/\\/|https:\\/\\/)?(www.)?([a-zA-Z0-9]+).[a-zA-Z0-9]*.[a-z]{3}.?([a-z]+)?$");
  private static Pattern emailPattern = Pattern.compile("^(\\w)+(\\.\\w+)*@(\\w)+((\\.\\w+)+)$");
  public static final GraphQLScalarType urlType =
      ExtendedScalars.newRegexScalar("URL").addPattern(urlPattern).build();
  public static final GraphQLScalarType emailType =
      ExtendedScalars.newRegexScalar("Email").addPattern(emailPattern).build();
  public static GraphQLObjectType deleteReturn =
      GraphQLObjectType.newObject()
          .name(GRAPHQL_DELETERESULT_POSTFIX)
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_AFFECTEDROW_FIELDNAME)
                  .type(Scalars.GraphQLInt))
          .build();
  public static GraphQLObjectType updateReturn =
      GraphQLObjectType.newObject()
          .name(GRAPHQL_UPDATERESULT_POSTFIX)
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_AFFECTEDROW_FIELDNAME)
                  .type(Scalars.GraphQLInt))
          .build();
  public static GraphQLObjectType insertReturn =
      GraphQLObjectType.newObject()
          .name(GRAPHQL_INSERTRESULT_POSTFIX)
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_AFFECTEDROW_FIELDNAME)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_IDLIST_FIELDNAME)
                  .type(GraphQLList.list(Scalars.GraphQLID)))
          .build();
  public static ScalarFieldInfo defaultIDField = new ScalarFieldInfo();
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  static {
    defaultIDField.setDescription("ID Field");
    defaultIDField.setType(GRAPHQL_ID_TYPENAME);
    defaultIDField.setName(GRAPHQL_ID_FIELDNAME);
  }

  public static final GraphQLObjectType nestInputResult =
      GraphQLObjectType.newObject()
          .name(GRAPHQL_NESTINPUTRESULT_TYPENAME)
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_NESTINPUT_OBJECTID_FIELDNAME)
                  .type(GraphQLList.list(Scalars.GraphQLID)))
          .build();
  public static final GraphQLObjectType nestRemoveResult =
      GraphQLObjectType.newObject()
          .name(GRAPHQL_NESTREMOVERESULT_TYPENAME)
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GRAPHQL_AFFECTEDROW_FIELDNAME)
                  .type(Scalars.GraphQLInt))
          .build();
  private static final SchemaPrinter sp = new SchemaPrinter();
  private static final String directive_data =
      "scalar Object \n"
          + "scalar JSON\n"
          + "scalar Date\n"
          + "scalar DateTime\n"
          + "scalar Time \n"
          + "scalar Email \n"
          + "scalar LastUpdate \n"
          + "scalar CreatedAt \n"
          + "scalar URL\n"
          + "directive @object_metadata(metadata:String) on OBJECT\n"
          + "directive @objectinput_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @objectwhereinput_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @objectfieldfilter_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @objectupdateinput_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @enumfilter_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @enumlistfilter_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @subscription_node_metadata(metadata:String) on OBJECT\n"
          + "directive @insertresult_metadata(metadata:String) on OBJECT\n"
          + "directive @updateresult_metadata(metadata:String) on OBJECT\n"
          + "directive @delteresult_metadata(metadata:String) on OBJECT\n"
          + "directive @loginresult_metadata(metadata:String) on OBJECT\n"
          + "directive @fieldsmap_metadata(metadata:String) on INPUT_OBJECT\n"
          + "directive @enumtype(metadata:String) on ENUM\n"
          + "directive @selectbyid_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @selectbycondition_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @thirdapi_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @objectcreate_api(metadata:String) on FIELD_DEFINITION\n"
          + "directive @objectdestory_api(metadata:String) on FIELD_DEFINITION\n"
          + "directive @objectupdate_api(metadata:String) on FIELD_DEFINITION\n"
          + "directive @nest_fromobjectinput_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @nest_fromidinput_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @nest_toidinput_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @nest_toremove_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @nest_fromremove_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @subscription_metadata(metadata:String) on FIELD_DEFINITION\n"
          + "directive @field_in_contenttype_remove(metadata:String) on FIELD_DEFINITION\n"
          //          + "directive @remote_query(metadata:String) on FIELD_DEFINITION\n"
          //          + "directive @remote_mutation(metadata:String) on FIELD_DEFINITION\n"
          //          + "directive @remote_subscription(metadata:String) on FIELD_DEFINITION\n"
          //          + "directive @remote_object(metadata:String) on FIELD_DEFINITION\n"
          //          + "directive @remote_input(metadata:String) on INPUT_OBJECT"
          + "directive @schemadata(metadata:String) on SCHEMA\n"
          + "directive @triggers(createtriggers:String,updatetriggers:String,destroytriggers:String) on OBJECT\n"
          + "directive @byreference(referenceinfo:String) on OBJECT\n"
          + "directive @metadata(objectinfo:String) on OBJECT\n"
          + "directive @uniquefields(fields:String) on OBJECT\n";

  /**
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLObjectType getInsertResult(ObjectTypeMetaData objectTypeMetaData) {
    HashMap hashMap = new HashMap();
    hashMap.put("type", objectTypeMetaData.getOutPutName());
    GraphQLObjectType.Builder objectTypeBuilder =
        GraphQLObjectType.newObject()
            .name(objectTypeMetaData.getOutPutName() + GRAPHQL_INSERTRESULT_POSTFIX);
    objectTypeBuilder
        .field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(GRAPHQL_IDLIST_FIELDNAME)
                .type(Scalars.GraphQLString))
        .field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(GRAPHQL_NODES_FIELDNAME)
                .type(GraphQLList.list(getRefObjectType(objectTypeMetaData.getOutPutName()))))
        .withDirective(
            GraphQLDirective.newDirective()
                .name(GRAPHQL_INSERTRESULT_METADATA_DIRECTIVE)
                .argument(
                    GraphQLArgument.newArgument()
                        .name(GRAPHQL_DIRECTIVE_METADATA)
                        .type(Scalars.GraphQLString)
                        .value(JSONObject.toJSONString(hashMap))
                        .build())
                .build());
    return objectTypeBuilder.build();
  }

  /**
   * 生成对应类型的ObjectType
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLObjectType objectTypeGenerator(ObjectTypeMetaData objectTypeMetaData) {
    GraphQLObjectType.Builder objectTypeBuilder =
        GraphQLObjectType.newObject().name(objectTypeMetaData.getOutPutName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> scalarIterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (scalarIterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = scalarIterator.next();
      objectTypeBuilder.field(getScalarFieldDef(entry.getValue()));
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      objectTypeBuilder.field(getEnumFieldDef(entry.getValue()));
    }
    Iterator<Map.Entry<String, RelationField>> fromRelationIterator =
        objectTypeMetaData.getFromRelationFieldData().entrySet().iterator();
    while (fromRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> entry = fromRelationIterator.next();
      objectTypeBuilder.field(getFromRelationFieldDef(entry.getValue()));
    }
    Iterator<Map.Entry<String, RelationField>> toRelationIterator =
        objectTypeMetaData.getToRelationFieldData().entrySet().iterator();
    while (toRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> entry = toRelationIterator.next();
      objectTypeBuilder.field(getToRelationFieldDef(entry.getValue()));
    }
    return objectTypeBuilder.withDirective(getObjectDirective(objectTypeMetaData)).build();
  };

  /**
   * 生成对应对象的InputType;
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType objectTypeInput(ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder objectTypeInputBuilder =
        GraphQLInputObjectType.newInputObject().name(objectTypeMetaData.getInputObjectName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> scalarIterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (scalarIterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = scalarIterator.next();
      objectTypeInputBuilder.field(getInputField(entry.getValue()));
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      objectTypeInputBuilder.field(getInputEnumField(entry.getValue()));
    }
    return objectTypeInputBuilder
        .withDirective(getObjectInputDirective(objectTypeMetaData))
        .build();
  }

  /**
   * 生成对应对象的WhereInputType
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType objectTypeWhereInput(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLInputObjectType.newInputObject()
        .name(objectTypeMetaData.getWhereInputObjectName())
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_AND_OPERATOR)
                .type(
                    GraphQLList.list(
                        GraphQLTypeReference.typeRef(
                            objectTypeMetaData.getWhereInputObjectName()))))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_OR_OPERATOR)
                .type(
                    GraphQLList.list(
                        GraphQLTypeReference.typeRef(
                            objectTypeMetaData.getWhereInputObjectName()))))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_NOT_OPERATOR)
                .type(GraphQLTypeReference.typeRef(objectTypeMetaData.getWhereInputObjectName())))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_FIELDCONTAIN_OPERATOR)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_FILTER_OPERATOR)
                .type(GraphQLTypeReference.typeRef(objectTypeMetaData.getFieldFilterName())))
        .withDirective(getObjectWhereInputDirective(objectTypeMetaData))
        .build();
  }

  /**
   * 生成对应对象的FieldFilter
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType objectTypeFieldFilter(
      ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder objectTypeFieldFilterBuilder =
        GraphQLInputObjectType.newInputObject().name(objectTypeMetaData.getFieldFilterName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = iterator.next();
      GraphQLInputObjectField fieldinputFilter = getFieldFilterInputScalarField(entry.getValue());
      if (null != fieldinputFilter) {
        objectTypeFieldFilterBuilder.field(fieldinputFilter);
      }
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      EnumField enumField = entry.getValue();
      if (!enumField.isList()) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(enumField.getName())
                .type(
                    GraphQLTypeReference.typeRef(
                        enumField.getType() + GRAPHQL_ENUM_FILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(enumField.getName())
                .type(
                    GraphQLTypeReference.typeRef(
                        enumField.getType() + GRAPHQL_ENUM_LISTFILTER_POSTFIX)));
      }
    }
    Iterator<Map.Entry<String, RelationField>> fromRelationIterator =
        objectTypeMetaData.getFromRelationFieldData().entrySet().iterator();
    while (fromRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> fromRelation = fromRelationIterator.next();
      RelationField relationField = fromRelation.getValue();
      if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
          || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_FIELDFILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_LISTMATCH_POSTFIX)));
      }
    }
    Iterator<Map.Entry<String, RelationField>> toRelationIterator =
        objectTypeMetaData.getToRelationFieldData().entrySet().iterator();
    while (toRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> toRelation = toRelationIterator.next();
      RelationField relationField = toRelation.getValue();
      if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
          || relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_FIELDFILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_LISTMATCH_POSTFIX)));
      }
    }
    return objectTypeFieldFilterBuilder
        .withDirective(getObjectFieldFilterDirective(objectTypeMetaData))
        .build();
  }
  /**
   * 生成对应对象的FieldFilter
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType userFieldFilter(ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder objectTypeFieldFilterBuilder =
        GraphQLInputObjectType.newInputObject().name(objectTypeMetaData.getFieldFilterName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = iterator.next();
      if (entry.getKey().equals(GRAPHQL_PASSWORD_FIELD)) {
        continue;
      }
      GraphQLInputObjectField fieldinputFilter = getFieldFilterInputScalarField(entry.getValue());
      if (null != fieldinputFilter) {
        objectTypeFieldFilterBuilder.field(fieldinputFilter);
      }
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      EnumField enumField = entry.getValue();
      if (!enumField.isList()) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(enumField.getName())
                .type(
                    GraphQLTypeReference.typeRef(
                        enumField.getType() + GRAPHQL_ENUM_FILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(enumField.getName())
                .type(
                    GraphQLTypeReference.typeRef(
                        enumField.getType() + GRAPHQL_ENUM_LISTFILTER_POSTFIX)));
      }
    }
    Iterator<Map.Entry<String, RelationField>> fromRelationIterator =
        objectTypeMetaData.getFromRelationFieldData().entrySet().iterator();
    while (fromRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> fromRelation = fromRelationIterator.next();
      RelationField relationField = fromRelation.getValue();
      if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
          || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_FIELDFILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_LISTMATCH_POSTFIX)));
      }
    }
    Iterator<Map.Entry<String, RelationField>> toRelationIterator =
        objectTypeMetaData.getToRelationFieldData().entrySet().iterator();
    while (toRelationIterator.hasNext()) {
      Map.Entry<String, RelationField> toRelation = toRelationIterator.next();
      RelationField relationField = toRelation.getValue();
      if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
          || relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_FIELDFILTER_POSTFIX)));
      } else {
        objectTypeFieldFilterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(relationField.getFromField())
                .type(
                    GraphQLTypeReference.typeRef(
                        relationField.getToObject() + GRAPHQL_LISTMATCH_POSTFIX)));
      }
    }
    return objectTypeFieldFilterBuilder
        .withDirective(getObjectFieldFilterDirective(objectTypeMetaData))
        .build();
  }

  /**
   * 生成对应对象的UpdateInput
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType objectTypeUpdateInput(
      ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder objectTypeUpdateBuilder =
        GraphQLInputObjectType.newInputObject().name(objectTypeMetaData.getUpdateObjectName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = iterator.next();
      if (entry.getKey().equals(GRAPHQL_ID_FIELDNAME)) {
        continue;
      }
      objectTypeUpdateBuilder.field(getUpdateInputScalarField(entry.getValue()));
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      objectTypeUpdateBuilder.field(getUpdateInputEnumField(entry.getValue()));
    }
    return objectTypeUpdateBuilder
        .withDirective(getObjectUpdateInputDirective(objectTypeMetaData))
        .build();
  }
  /**
   * 生成对应对象的UpdateInput
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLInputObjectType userTypeUpdateInput(ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder objectTypeUpdateBuilder =
        GraphQLInputObjectType.newInputObject().name(objectTypeMetaData.getUpdateObjectName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
        objectTypeMetaData.getScalarFieldData().entrySet().iterator();
    int fieldSize =0;
    while (iterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = iterator.next();
      if (entry.getKey().equals(GRAPHQL_ID_FIELDNAME)) {
        continue;
      }
      objectTypeUpdateBuilder.field(getUpdateInputScalarField(entry.getValue()));
      fieldSize++;
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        objectTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      if(entry.equals(GRAPHQL_ROLE_FIELDNAME)) {
        continue;
      }
      objectTypeUpdateBuilder.field(getUpdateInputEnumField(entry.getValue()));
    }
    if(fieldSize==0) {
      return null;
    }
    return objectTypeUpdateBuilder
        .withDirective(getObjectUpdateInputDirective(objectTypeMetaData))
        .build();
  }
  /**
   * 生成对应对象的UpdateInput
   *
   * @param userTypeMetaData
   * @return
   */
  public static GraphQLObjectType userType(ObjectTypeMetaData userTypeMetaData) {
    GraphQLObjectType.Builder userTypeBuilder =
        GraphQLObjectType.newObject().name(userTypeMetaData.getOutPutName());
    Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
        userTypeMetaData.getScalarFieldData().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, ScalarFieldInfo> entry = iterator.next();
      if (entry.getKey().equals(GRAPHQL_ID_FIELDNAME)
          || entry.getKey().equals(GRAPHQL_PASSWORD_FIELD)) {
        continue;
      }
      userTypeBuilder.field(getScalarFieldDef(entry.getValue()));
    }
    Iterator<Map.Entry<String, EnumField>> enumIterator =
        userTypeMetaData.getEnumFieldData().entrySet().iterator();
    while (enumIterator.hasNext()) {
      Map.Entry<String, EnumField> entry = enumIterator.next();
      userTypeBuilder.field(getEnumFieldDef(entry.getValue()));
    }
    return userTypeBuilder.withDirective(getObjectDirective(userTypeMetaData)).build();
  }

  /**
   * 生成对应对象的SelectById的API
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLFieldDefinition getAPISelectById(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(getNameSelectById(objectTypeMetaData.getOutPutName()))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_ID_FIELDNAME)
                .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
        .type(GraphQLTypeReference.typeRef(objectTypeMetaData.getOutPutName()))
        .withDirective(getAPISelectByIdDirective(objectTypeMetaData))
        .build();
  }

  /**
   * 生成对应对象的条件查询API
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLFieldDefinition getAPISelectByCondition(
      ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(getNameSelectByCondition(objectTypeMetaData.getOutPutName()))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_WHERE_ARGUMENT)
                .type(getRefObjectType(objectTypeMetaData.getWhereInputObjectName())))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_SKIP_ARGUMENT)
                .type(Scalars.GraphQLInt)
                .defaultValue(0))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_LIMIT_ARGUMENT)
                .type(Scalars.GraphQLInt)
                .defaultValue(Integer.MAX_VALUE))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_ORDERBY_ARGUMENT)
                .type(Scalars.GraphQLString))
        .type(GraphQLList.list(GraphQLTypeReference.typeRef(objectTypeMetaData.getOutPutName())))
        .withDirective(getAPISelectByConditionDirective(objectTypeMetaData))
        .build();
  }

  /**
   * 生成销毁对象的API
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLFieldDefinition getAPIDestory(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(getNameDestory(objectTypeMetaData.getOutPutName()))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_WHERE_ARGUMENT)
                .type(getRefObjectType(objectTypeMetaData.getWhereInputObjectName())))
        .withDirective(getAPIObjectDestoryDirective(objectTypeMetaData))
        .type(deleteReturn)
        .build();
  }

  /**
   * 生成创建对象的API
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLFieldDefinition getAPICreate(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(getNameCreate(objectTypeMetaData.getOutPutName()))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_OBJECTS_ARGUMENT)
                .type(
                    GraphQLNonNull.nonNull(
                        GraphQLList.list(
                            getRefObjectTypeInput(objectTypeMetaData.getOutPutName())))))
        .argument(conflictArgument)
        .withDirective(getAPIObjectCreateDirective(objectTypeMetaData))
        .type(insertReturn)
        .build();
  }

  /**
   * 生成更新对象的API
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLFieldDefinition getAPIUpdate(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(getNameUpadte(objectTypeMetaData.getOutPutName()))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_WHERE_ARGUMENT)
                .type(getRefObjectTypeWhereInput(objectTypeMetaData.getOutPutName())))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_OBJECT_ARGUMENT)
                .type(
                    GraphQLNonNull.nonNull(
                        getRefObjectTypeUpdateInput(objectTypeMetaData.getOutPutName()))))
        .withDirective(getAPIObjectUpdateDirective(objectTypeMetaData))
        .type(updateReturn)
        .build();
  }

  /**
   * 建立ID关联
   *
   * @param relationField
   * @return
   */
  public static GraphQLFieldDefinition getAPIFieldFromIDInput(RelationField relationField) {
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .type(nestInputResult)
          .withDirective(getAPINestFromIDInputDirective(relationField))
          .build();
    } else if (relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_RESET_FIELDNAME)
                  .type(Scalars.GraphQLBoolean)
                  .defaultValue(false))
          .type(nestInputResult)
          .withDirective(getAPINestFromIDInputDirective(relationField))
          .build();
    } else {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(GraphQLList.list(Scalars.GraphQLID))))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_RESET_FIELDNAME)
                  .type(Scalars.GraphQLBoolean)
                  .defaultValue(false))
          .type(nestInputResult)
          .withDirective(getAPINestFromIDInputDirective(relationField))
          .build();
    }
  }
  /**
   * 建立ID关联
   *
   * @param relationField
   * @return
   */
  public static GraphQLFieldDefinition getAPIFieldToIDInput(RelationField relationField) {
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameToIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .type(nestInputResult)
          .withDirective(getAPINestToIDInputDirective(relationField))
          .build();
    } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameToIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_RESET_FIELDNAME)
                  .type(Scalars.GraphQLBoolean)
                  .defaultValue(false))
          .type(nestInputResult)
          .withDirective(getAPINestToIDInputDirective(relationField))
          .build();
    } else {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameToIDInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(GraphQLList.list(Scalars.GraphQLID))))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_RESET_FIELDNAME)
                  .type(Scalars.GraphQLBoolean)
                  .defaultValue(false))
          .type(nestInputResult)
          .withDirective(getAPINestToIDInputDirective(relationField))
          .build();
    }
  }

  public static Boolean remoteEndPointConstruct(
      String endPoint,
      HashMap headers,
      List<String> queryAPIList,
      List<String> mutationAPIList,
      List<String> subscriptionList,
      List<GraphQLFieldDefinition> queryFields,
      List<GraphQLFieldDefinition> mutationFields,
      List<GraphQLFieldDefinition> subscriptionFields,
      List<GraphQLType> addtionTypes) {
    try {
      Map<String, Object> remoteInfo =
          (Map<String, Object>) RemoteSchemaLoader.loadRemoteSchema(endPoint, headers).get();
      Map<String, Object> schema = (Map) remoteInfo.get("__schema");
      Map<String, Object> queryType = (Map) schema.get("queryType");
      Map<String, List<String>> apiMap = new HashMap<>();
      Assert.assertNotNull(queryType, "queryType expected", new Object[0]);
      TypeName query = TypeName.newTypeName().name((String) queryType.get("name")).build();
      String queryName = query.getName();
      apiMap.put(queryName, new ArrayList<>());
      Map<String, Object> mutationType = (Map) schema.get("mutationType");
      String mutationName = null;
      if (mutationType != null) {
        TypeName mutation = TypeName.newTypeName().name((String) mutationType.get("name")).build();
        mutationName = mutation.getName();
        apiMap.put(mutationName, new ArrayList<>());
      }
      Map<String, Object> subscriptionType = (Map) schema.get("subscriptionType");
      String subscriptionName = null;
      if (subscriptionType != null) {
        TypeName subscription =
            TypeName.newTypeName().name((String) subscriptionType.get("name")).build();
        subscriptionName = subscription.getName();
        apiMap.put(subscriptionName, new ArrayList<>());
      }
      List<Map<String, Object>> types = (List) schema.get("types");
      Iterator var13 = types.iterator();
      Map<String, TypeDefinition> typeMap = new HashMap<>();
      while (var13.hasNext()) {
        Map<String, Object> type = (Map) var13.next();
        TypeDefinition typeDefinition = createTypeDefinition(type);
        typeMap.put(typeDefinition.getName(), typeDefinition);
      }
      if (null != queryAPIList && queryAPIList.size() > 0) {
        ObjectTypeDefinition queryTypeDef = (ObjectTypeDefinition) typeMap.get(queryName);
        List<FieldDefinition> fieldDefinitions = queryTypeDef.getFieldDefinitions();
      }
      if (null != mutationAPIList && mutationAPIList.size() > 0) {}

      if (null != subscriptionList && subscriptionList.size() > 0) {}

      return true;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        HashMap errorMap = new HashMap();
        errorMap.put(GRAPHQL_ENDPOINT_FIELDNAME, endPoint);
        errorMap.put(GRAPHQL_HEADERS_FIELDNAME, headers);
        log.error("{}", LogData.getErrorLog("E10085", errorMap, e));
      }
      return false;
    }
  }

  /**
   * 建立关联关系
   *
   * @param relationField
   * @return
   */
  public static GraphQLFieldDefinition getAPIFieldFromObjectInput(RelationField relationField) {
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromObjectInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_OBJECT)
                  .type(
                      GraphQLNonNull.nonNull(
                          getRefObjectType(getTypeNameInput(relationField.getToObject())))))
          .type(nestInputResult)
          .withDirective(getAPINestFromObjectInputDirective(relationField))
          .build();
    } else if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromObjectInput(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_OBJECT)
                  .type(
                      GraphQLNonNull.nonNull(
                          GraphQLList.list(
                              getRefObjectType(getTypeNameInput(relationField.getToObject()))))))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_RESET_FIELDNAME)
                  .type(Scalars.GraphQLBoolean)
                  .defaultValue(false))
          .type(nestInputResult)
          .withDirective(getAPINestFromObjectInputDirective(relationField))
          .build();
    } else {
      return null;
    }
  }

  public static GraphQLFieldDefinition getAPIFromRemove(RelationField relationField) {
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
        || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromRemove(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .type(nestRemoveResult)
          .withDirective(getAPINestFromRemoveDirective(relationField))
          .build();
    } else {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromRemove(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(GraphQLList.list(Scalars.GraphQLID))))
          .type(nestRemoveResult)
          .withDirective(getAPINestFromIDInputDirective(relationField))
          .build();
    }
  }

  public static GraphQLFieldDefinition getAPIToRemove(RelationField relationField) {
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
        || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameToRemove(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .type(nestRemoveResult)
          .withDirective(getAPINestToRemoveDirective(relationField))
          .build();
    } else {
      return GraphQLFieldDefinition.newFieldDefinition()
          .name(getNestFieldNameFromRemove(relationField))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_FROM_ID)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLID)))
          .argument(
              GraphQLArgument.newArgument()
                  .name(GRAPHQL_TO_ID)
                  .type(GraphQLNonNull.nonNull(GraphQLList.list(Scalars.GraphQLID))))
          .type(nestRemoveResult)
          .withDirective(getAPINestToIDInputDirective(relationField))
          .build();
    }
  }

  public static GraphQLFieldDefinition getAPISubscription(ObjectTypeMetaData objectTypeMetaData) {
    String fieldSelectMap = getNameFieldSelectMap(objectTypeMetaData);
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(objectTypeMetaData.getApiNameSubscription())
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_WHERE_ARGUMENT)
                .type(getRefObjectTypeWhereInput(objectTypeMetaData.getOutPutName())))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_SELECTFIELDS_FIELDNAME)
                .type(getRefObjectType(fieldSelectMap)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_WATCHFIELDS_FIELDNAME)
                .type(getRefObjectType(fieldSelectMap)))
        .type(ExtendedScalars.Object)
        .withDirective(getSubscriptionDirective(objectTypeMetaData))
        .build();
  }

  public static GraphQLInputType getFieldsSelectMapInput(ObjectTypeMetaData objectTypeMetaData) {
    GraphQLInputObjectType.Builder mapInputTypeBuilder =
        GraphQLInputObjectType.newInputObject().name(getNameFieldSelectMap(objectTypeMetaData));
    if (null != objectTypeMetaData.getScalarFieldData()) {
      objectTypeMetaData
          .getScalarFieldData()
          .forEach(
              (fieldName, scalarType) -> {
                if (!fieldName.equals(GRAPHQL_ID_FIELDNAME)) {
                  mapInputTypeBuilder.field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name(fieldName)
                          .type(Scalars.GraphQLBoolean)
                          .defaultValue(false));
                }
              });
    }
    if (null != objectTypeMetaData.getEnumFieldData()) {
      objectTypeMetaData
          .getEnumFieldData()
          .forEach(
              (fieldName, enumType) -> {
                mapInputTypeBuilder.field(
                    GraphQLInputObjectField.newInputObjectField()
                        .name(fieldName)
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(false));
              });
    }
    if (null != objectTypeMetaData.getFromRelationFieldData()) {
      objectTypeMetaData
          .getFromRelationFieldData()
          .forEach(
              (fieldName, fromRelation) -> {
                if (fromRelation.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
                  mapInputTypeBuilder.field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name(fieldName)
                          .type(Scalars.GraphQLBoolean)
                          .defaultValue(false));
                }
              });
    }
    if (null != objectTypeMetaData.getToRelationFieldData()) {
      objectTypeMetaData
          .getToRelationFieldData()
          .forEach(
              (fieldName, toRelation) -> {
                if (toRelation.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
                    || toRelation.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
                  mapInputTypeBuilder.field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name(fieldName)
                          .type(Scalars.GraphQLBoolean)
                          .defaultValue(false));
                }
              });
    }
    return mapInputTypeBuilder
        .withDirective(getFieldsSelectMapDirective(objectTypeMetaData))
        .build();
  }

  public static String getNameFieldSelectMap(ObjectTypeMetaData objectTypeMetaData) {
    return objectTypeMetaData.getOutPutName() + GRAPHQL_SELECTFIELDSMAP_INPUT_POSTFIX;
  }

  public static GraphQLDirective getFieldsSelectMapDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap typeInfo = new HashMap();
    typeInfo.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_SELECTFIELDSMAP_METADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(typeInfo)))
        .build();
  }

  public static GraphQLDirective getSubscriptionDirective(ObjectTypeMetaData objectTypeMetaData) {
    HashMap subscriptionMap = new HashMap();
    subscriptionMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_SUBSCRIPTION_METADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(subscriptionMap)))
        .build();
  }

  /**
   * 获取对象的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getObjectDirective(ObjectTypeMetaData objectTypeMetaData) {
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_OBJECT_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectTypeMetaData)))
        .build();
  }

  /**
   * 获取Input对象的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getObjectInputDirective(ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectInputMap = new HashMap();
    objectInputMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_OBJECTINPUT_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectInputMap)))
        .build();
  }

  /**
   * 获取Update对象的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getObjectUpdateInputDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectUpdateInputMap = new HashMap();
    objectUpdateInputMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_OBJECTUPDATEINPUT_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectUpdateInputMap)))
        .build();
  }

  /**
   * 获取WhereInput对象的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getObjectWhereInputDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectWhereInputMap = new HashMap();
    objectWhereInputMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_OBJECTWHEREINPUT_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectWhereInputMap)))
        .build();
  }

  /**
   * 获取SelectByID 的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getAPISelectByIdDirective(ObjectTypeMetaData objectTypeMetaData) {
    HashMap selectByIDMap = new HashMap();
    selectByIDMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_SELECTBYID_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(selectByIDMap)))
        .build();
  }

  /**
   * 获取enumFilter的注解
   *
   * @param enumType
   */
  public static GraphQLDirective getEnumFilterDirective(EnumTypeMetaData enumType) {
    HashMap enumMap = new HashMap();
    enumMap.put(GRAPHQL_ENUMNAME_FILEDNAME, enumType.getName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_ENUMFILTER_METADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(enumMap)))
        .build();
  }

  /**
   * 获取SelectByCondition的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getAPISelectByConditionDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap selectByConditionMap = new HashMap();
    selectByConditionMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_SELECTBYCONDITION_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(selectByConditionMap)))
        .build();
  }

  /**
   * 获取ObjectCreate注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getAPIObjectCreateDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectCreateMap = new HashMap();
    objectCreateMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_API_OBJECTCREATE_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectCreateMap)))
        .build();
  }

  /**
   * 获取ObjectDestory注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getAPIObjectDestoryDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectDestoryMap = new HashMap();
    objectDestoryMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_API_OBJECTDESTORY_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectDestoryMap)))
        .build();
  }

  /**
   * 获取对象更新方法的注解
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getAPIObjectUpdateDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap objectUpdateMap = new HashMap();
    objectUpdateMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_API_OBJECTUPDATE_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(objectUpdateMap)))
        .build();
  }

  /**
   * 获取新增字段内容的方法的注解
   *
   * @param relationField
   * @return
   */
  public static GraphQLDirective getAPINestFromIDInputDirective(RelationField relationField) {
    HashMap<String, String> relationFieldMap = new HashMap<>();
    relationFieldMap.put(GRAPHQL_FIELDNAME_FIELDNAME, relationField.getFromField());
    relationFieldMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, relationField.getFromObject());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_NESTFROMIDINPUT_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(relationFieldMap)))
        .build();
  }

  /**
   * 获取新增字段内容的方法的注解
   *
   * @param relationField
   * @return
   */
  public static GraphQLDirective getAPINestFromObjectInputDirective(RelationField relationField) {
    HashMap<String, String> relationFieldMap = new HashMap<>();
    relationFieldMap.put(GRAPHQL_FIELDNAME_FIELDNAME, relationField.getFromField());
    relationFieldMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, relationField.getFromObject());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_NESTFROMOBJECTINPUT_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(relationFieldMap)))
        .build();
  }
  /**
   * 获取新增字段内容的方法的注解
   *
   * @param relationField
   * @return
   */
  public static GraphQLDirective getAPINestToIDInputDirective(RelationField relationField) {
    HashMap<String, String> relationFieldMap = new HashMap<>();
    relationFieldMap.put(GRAPHQL_FIELDNAME_FIELDNAME, relationField.getToField());
    relationFieldMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, relationField.getToObject());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_NESTTOIDINPUT_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(relationFieldMap)))
        .build();
  }

  /**
   * 获取新增字段内容的方法的注解
   *
   * @param relationField
   * @return
   */
  public static GraphQLDirective getAPINestFromRemoveDirective(RelationField relationField) {
    HashMap<String, String> relationFieldMap = new HashMap<>();
    relationFieldMap.put(GRAPHQL_FIELDNAME_FIELDNAME, relationField.getFromField());
    relationFieldMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, relationField.getFromObject());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_NESTFROMREMOVE_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(relationFieldMap)))
        .build();
  }
  /**
   * 获取新增字段内容的方法的注解
   *
   * @param relationField
   * @return
   */
  public static GraphQLDirective getAPINestToRemoveDirective(RelationField relationField) {
    HashMap<String, String> relationFieldMap = new HashMap<>();
    relationFieldMap.put(GRAPHQL_FIELDNAME_FIELDNAME, relationField.getToField());
    relationFieldMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, relationField.getToObject());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_NESTTOREMOVE_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(relationFieldMap)))
        .build();
  }

  /**
   * 获取第三方API的注解
   *
   * @param thirdPartAPIMetaData
   * @return
   */
  public static GraphQLDirective getThirdAPIMetaData(ThirdPartAPIMetaData thirdPartAPIMetaData) {
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_THIRDAPI_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(thirdPartAPIMetaData)))
        .build();
  }

  /**
   * 获取FieldFilter注解的对象
   *
   * @param objectTypeMetaData
   * @return
   */
  public static GraphQLDirective getObjectFieldFilterDirective(
      ObjectTypeMetaData objectTypeMetaData) {
    HashMap fieldFilterMap = new HashMap();
    fieldFilterMap.put(GRAPHQL_OBJECTNAME_FIELDNAME, objectTypeMetaData.getOutPutName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_OBJECTFIELDFILTER_METADATA_DIRECTIVE)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(fieldFilterMap)))
        .build();
  }

  /**
   * 获取标量字段定义
   *
   * @param scalarFieldInfo
   * @return
   */
  public static GraphQLFieldDefinition getScalarFieldDef(ScalarFieldInfo scalarFieldInfo) {
    GraphQLFieldDefinition.Builder fieldDefBuilder =
        GraphQLFieldDefinition.newFieldDefinition().name(scalarFieldInfo.getName());
    if (scalarFieldInfo.isList()) {
      fieldDefBuilder.type(GraphQLList.list(getScalarType(scalarFieldInfo.getType())));
    } else {
      fieldDefBuilder.type(getScalarType(scalarFieldInfo.getType()));
    }
    return fieldDefBuilder.build();
  }

  /**
   * 获取枚举字段的定义
   *
   * @param enumField
   * @return
   */
  public static GraphQLFieldDefinition getEnumFieldDef(EnumField enumField) {
    GraphQLFieldDefinition.Builder fieldDefBuilder =
        GraphQLFieldDefinition.newFieldDefinition().name(enumField.getName());
    if (enumField.isList()) {
      fieldDefBuilder.type(GraphQLList.list(GraphQLTypeReference.typeRef(enumField.getType())));
    } else {
      fieldDefBuilder.type(GraphQLTypeReference.typeRef(enumField.getType()));
    }
    return fieldDefBuilder.build();
  }

  /**
   * 获取from关联字段
   *
   * @param relationField
   * @return
   */
  public static GraphQLFieldDefinition getFromRelationFieldDef(RelationField relationField) {
    GraphQLFieldDefinition.Builder fieldDefBuilder =
        GraphQLFieldDefinition.newFieldDefinition().name(relationField.getFromField());
    if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
        || relationField.getRelationType().equals(GRAPHQL_MANY2ONE_NAME)) {
      fieldDefBuilder.type(GraphQLTypeReference.typeRef(relationField.getToObject()));
    } else {
      fieldDefBuilder.type(
          GraphQLList.list(GraphQLTypeReference.typeRef(relationField.getToObject())));
    }
    return fieldDefBuilder.build();
  }

  /**
   * 获取to关联字段
   *
   * @param relationField
   * @return
   */
  public static GraphQLFieldDefinition getToRelationFieldDef(RelationField relationField) {
    GraphQLFieldDefinition.Builder fieldDefBuilder =
        GraphQLFieldDefinition.newFieldDefinition().name(relationField.getToField());
    if (relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)
        || relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)) {
      fieldDefBuilder.type(GraphQLTypeReference.typeRef(relationField.getFromObject()));
    } else {
      fieldDefBuilder.type(
          GraphQLList.list(GraphQLTypeReference.typeRef(relationField.getFromObject())));
    }
    return fieldDefBuilder.build();
  }

  public static Object getDefaultValue(ScalarFieldInfo scalarFieldInfo) {
    if (null == scalarFieldInfo) {
      throw new BusinessException("E10099");
    }
    if (!scalarFieldInfo.isList()) {
      switch (scalarFieldInfo.getType()) {
        case "ID":
        case "String":
        case "Date":
        case "DateTime":
        case "Time":
        case "URL":
        case "Email":
          return scalarFieldInfo.getDefaultValue();
        case "Float":
          return (Float) scalarFieldInfo.getDefaultValue();
        case "BigDecimal":
          return (BigDecimal) scalarFieldInfo.getDefaultValue();
        case "Boolean":
          return (Boolean) scalarFieldInfo.getDefaultValue();
        case "Char":
          if (((String) scalarFieldInfo.getDefaultValue()).length() != 1) {
            throw new BusinessException("E10098");
          } else {
            return ((String) scalarFieldInfo.getDefaultValue()).charAt(0);
          }
        case "Byte":
          return (Byte) scalarFieldInfo.getDefaultValue();
        case "Int":
          return (Integer) scalarFieldInfo.getDefaultValue();
        case "Short":
          return (Short) scalarFieldInfo.getDefaultValue();
        case "Long":
        case "BigInteger":
          return (Long) scalarFieldInfo.getDefaultValue();
        case "Object":
        case "JSON":
          return JSONObject.parseObject(JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()))
              .getInnerMap();
        default:
          throw new BusinessException("E10050");
      }
    } else {
      switch (scalarFieldInfo.getType()) {
        case "ID":
        case "String":
        case "Date":
        case "DateTime":
        case "Time":
        case "URL":
        case "Email":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), String.class);
        case "Float":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Float.class);
        case "BigDecimal":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), BigDecimal.class);
        case "Boolean":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Boolean.class);
        case "Char":
          if ((((String) scalarFieldInfo.getDefaultValue()).length()) != 1) {
            throw new BusinessException("E10098");
          } else {
            return JSONObject.parseArray(
                JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Character.class);
          }
        case "Byte":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Byte.class);
        case "Int":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Integer.class);
        case "Short":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Short.class);
        case "Long":
        case "BigInteger":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Long.class);
        case "Object":
        case "JSON":
          return JSONObject.parseArray(
              JSONObject.toJSONString(scalarFieldInfo.getDefaultValue()), Map.class);
        default:
          throw new BusinessException("E10050");
      }
    }
  }

  public static Object getDefaultValue(ThirdAPIField thirdAPIField) {
    if (null == thirdAPIField) {
      throw new BusinessException("E10099");
    }
    if (thirdAPIField.getKind().equals(GRAPHQL_TYPEKIND_SCALAR)) {
      if (!thirdAPIField.isIslist()) {
        switch (thirdAPIField.getType()) {
          case "ID":
          case "String":
          case "Date":
          case "DateTime":
          case "Time":
          case "URL":
          case "Email":
            return thirdAPIField.getDefaultValue();
          case "Float":
            return Float.parseFloat(thirdAPIField.getDefaultValue());
          case "BigDecimal":
            return new BigDecimal(thirdAPIField.getDefaultValue());
          case "Boolean":
            return Boolean.valueOf(thirdAPIField.getDefaultValue());
          case "Char":
            if (thirdAPIField.getDefaultValue().length() != 1) {
              throw new BusinessException("E10098");
            } else {
              return thirdAPIField.getDefaultValue().charAt(0);
            }
          case "Byte":
            return Byte.valueOf(thirdAPIField.getDefaultValue());
          case "Int":
            return Integer.valueOf(thirdAPIField.getDefaultValue());
          case "Short":
            return Short.valueOf(thirdAPIField.getDefaultValue());
          case "Long":
          case "BigInteger":
            return Long.valueOf(thirdAPIField.getDefaultValue());
          case "Object":
          case "JSON":
            return JSONObject.parseObject(thirdAPIField.getDefaultValue()).getInnerMap();
          default:
            throw new BusinessException("E10050");
        }
      } else {
        switch (thirdAPIField.getType()) {
          case "ID":
          case "String":
          case "Date":
          case "DateTime":
          case "Time":
          case "URL":
          case "Email":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), String.class);
          case "Float":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Float.class);
          case "BigDecimal":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), BigDecimal.class);
          case "Boolean":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Boolean.class);
          case "Char":
            if (thirdAPIField.getDefaultValue().length() != 1) {
              throw new BusinessException("E10098");
            } else {
              return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Character.class);
            }
          case "Byte":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Byte.class);
          case "Int":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Integer.class);
          case "Short":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Short.class);
          case "Long":
          case "BigInteger":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Long.class);
          case "Object":
          case "JSON":
            return JSONObject.parseArray(thirdAPIField.getDefaultValue(), Map.class);
          default:
            throw new BusinessException("E10050");
        }
      }
    } else if (thirdAPIField.getKind().equals(GRAPHQL_TYPEKIND_ENUM)) {
      if (thirdAPIField.isIslist()) {
        return JSONObject.parseArray(thirdAPIField.getDefaultValue(), String.class);
      } else {
        return thirdAPIField.getDefaultValue();
      }
    } else {
      throw new BusinessException("E10050");
    }
  }

  /**
   * 获取标量输入字段定义
   *
   * @param scalarFieldInfo
   * @return
   */
  public static GraphQLInputObjectField getInputField(ScalarFieldInfo scalarFieldInfo) {
    GraphQLInputObjectField.Builder inputFieldBuilder =
        GraphQLInputObjectField.newInputObjectField().name(scalarFieldInfo.getName());
    if (scalarFieldInfo.isNotNull() && !scalarFieldInfo.getName().equals(GRAPHQL_ID_FIELDNAME)) {
      if (scalarFieldInfo.isList()) {
        inputFieldBuilder.type(
            GraphQLNonNull.nonNull(GraphQLList.list(getScalarType(scalarFieldInfo.getType()))));
      } else {
        inputFieldBuilder.type(GraphQLNonNull.nonNull(getScalarType(scalarFieldInfo.getType())));
      }
    } else {
      if (scalarFieldInfo.isList()) {
        inputFieldBuilder.type(GraphQLList.list(getScalarType(scalarFieldInfo.getType())));
      } else {
        inputFieldBuilder.type(getScalarType(scalarFieldInfo.getType()));
      }
    }
    if (null != scalarFieldInfo.getDefaultValue()) {
      inputFieldBuilder.defaultValue(getDefaultValue(scalarFieldInfo));
    }
    return inputFieldBuilder.build();
  }

  /**
   * 获取标量输入字段定义
   *
   * @param enumField
   * @return
   */
  public static GraphQLInputObjectField getInputEnumField(EnumField enumField) {
    GraphQLInputObjectField.Builder inputFieldBuilder =
        GraphQLInputObjectField.newInputObjectField().name(enumField.getName());
    if (enumField.isNotNull()) {
      if (enumField.isList()) {
        inputFieldBuilder.type(
            GraphQLNonNull.nonNull(
                GraphQLList.list(GraphQLTypeReference.typeRef(enumField.getType()))));
      } else {
        inputFieldBuilder.type(
            GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef(enumField.getType())));
      }
    } else {
      if (enumField.isList()) {
        inputFieldBuilder.type(GraphQLList.list(GraphQLTypeReference.typeRef(enumField.getType())));
      } else {
        inputFieldBuilder.type(GraphQLTypeReference.typeRef(enumField.getType()));
      }
    }
    if (null != enumField.getDefaultValue()) {
      inputFieldBuilder.defaultValue(enumField.getDefaultValue());
    }
    return inputFieldBuilder.build();
  }

  /**
   * 生成枚举类型
   *
   * @param enumType
   * @return
   */
  public static GraphQLEnumType getEnumType(EnumTypeMetaData enumType) {
    GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(enumType.getName());
    for (EnumElement enumElement : enumType.getValues()) {
      enumBuilder.value(enumElement.getValue());
    }
    enumBuilder.withDirective(getEnumDirective(enumType));
    return enumBuilder.build();
  }

  /**
   * 生成枚举类型注解
   *
   * @param enumType
   * @return
   */
  public static GraphQLDirective getEnumDirective(EnumTypeMetaData enumType) {
    HashMap enumMap = new HashMap();
    enumMap.put(GRAPHQL_ENUMNAME_FILEDNAME, enumType.getName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_ENUMMETADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(enumMap)))
        .build();
  }

  /**
   * 生成枚举类型注解
   *
   * @param enumType
   * @return
   */
  public static GraphQLDirective getEnumListDirective(EnumTypeMetaData enumType) {
    HashMap enumMap = new HashMap();
    enumMap.put(GRAPHQL_ENUMNAME_FILEDNAME, enumType.getName());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_ENUMLISTFILTER_METADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(enumMap)))
        .build();
  }

  /**
   * 获取更新标量字段定义
   *
   * @param scalarFieldInfo
   * @return
   */
  public static GraphQLInputObjectField getUpdateInputScalarField(ScalarFieldInfo scalarFieldInfo) {
    GraphQLInputObjectField.Builder inputFieldBuilder =
        GraphQLInputObjectField.newInputObjectField().name(scalarFieldInfo.getName());
    if (scalarFieldInfo.isList()) {
      inputFieldBuilder.type(GraphQLList.list(getScalarType(scalarFieldInfo.getType())));
    } else {
      inputFieldBuilder.type(getScalarType(scalarFieldInfo.getType()));
    }
    return inputFieldBuilder.build();
  }

  /**
   * 获取更新枚举字段定义
   *
   * @param enumField
   * @return
   */
  public static GraphQLInputObjectField getUpdateInputEnumField(EnumField enumField) {
    GraphQLInputObjectField.Builder inputFieldBuilder =
        GraphQLInputObjectField.newInputObjectField().name(enumField.getName());
    if (enumField.isList()) {
      inputFieldBuilder.type(GraphQLList.list(GraphQLTypeReference.typeRef(enumField.getType())));
    } else {
      inputFieldBuilder.type(GraphQLTypeReference.typeRef(enumField.getType()));
    }
    return inputFieldBuilder.build();
  }

  /**
   * 获取FieldFilter标量字段定义
   *
   * @param thidAPIField
   * @return
   */
  public static GraphQLInputObjectField getFieldFilterInputScalarField(
      ScalarFieldInfo thidAPIField) {
    GraphQLInputObjectField.Builder inputFieldBuilder =
        GraphQLInputObjectField.newInputObjectField().name(thidAPIField.getName());
    GraphQLInputType inputType = null;
    if (thidAPIField.isList()) {
      inputType = getScalarListFilter(thidAPIField.getType());
    } else {
      inputType = getScalarFilter(thidAPIField.getType());
    }
    if (null == inputType) {
      return null;
    }
    return inputFieldBuilder.type(inputType).build();
  }

  /**
   * 获取FieldFilter枚举字段定义
   *
   * @param enumType
   * @return
   */
  public static GraphQLInputObjectType getFieldFilterInputEnumField(EnumTypeMetaData enumType) {
    GraphQLInputObjectType.Builder enumFilter =
        GraphQLInputObjectType.newInputObject()
            .name(enumType.getName() + GRAPHQL_ENUM_FILTER_POSTFIX);
    enumFilter
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_EQ_OPERATOR)
                .type(GraphQLTypeReference.typeRef(enumType.getName())))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_NE_OPERATOR)
                .type(GraphQLTypeReference.typeRef(enumType.getName())))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_IN_OPERATOR)
                .type(GraphQLList.list(GraphQLTypeReference.typeRef(enumType.getName()))))
        .withDirective(getEnumFilterDirective(enumType));
    return enumFilter.build();
  }

  /**
   * 枚举
   *
   * @param enumType
   * @return
   */
  public static GraphQLInputObjectType getEnumListFilter(EnumTypeMetaData enumType) {
    GraphQLInputObjectType.Builder enumListFilter =
        GraphQLInputObjectType.newInputObject()
            .name(enumType.getName() + GRAPHQL_ENUM_LISTFILTER_POSTFIX);
    enumListFilter
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_CONTAIN_OPERATOR)
                .type(GraphQLList.list(GraphQLTypeReference.typeRef(enumType.getName()))))
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_IN_OPERATOR)
                .type(GraphQLList.list(GraphQLTypeReference.typeRef(enumType.getName()))))
        .withDirective(getEnumListDirective(enumType));
    return enumListFilter.build();
  }

  /**
   * 获取标量类型
   *
   * @param typeName
   * @return
   */
  public static GraphQLScalarType getScalarType(String typeName) {
    switch (typeName) {
      case "ID":
        return Scalars.GraphQLID;
      case "Float":
      case "BigDecimal":
        return Scalars.GraphQLBigDecimal;
      case "Boolean":
        return Scalars.GraphQLBoolean;
      case "String":
      case "Char":
      case "Byte":
        return Scalars.GraphQLString;
      case "Int":
      case "Short":
        return Scalars.GraphQLInt;
      case "Long":
      case "BigInteger":
        return Scalars.GraphQLLong;
      case "Object":
        return ExtendedScalars.Object;
      case "JSON":
        return ExtendedScalars.Json;
      case "Date":
        return ExtendedScalars.Date;
      case "DateTime":
        return ExtendedScalars.DateTime;
      case "Time":
        return ExtendedScalars.Time;
      case "URL":
        return urlType;
      case "Email":
        return emailType;
      case "LastUpdate":
        return LastUpdateScalar.lastUpdateScalar;
      case "CreatedAt":
        return CreatedAtScalar.createdatscalar;
      default:
        throw new BusinessException("E10050");
    }
  }

  /**
   * 获取标量类型对应的过滤器
   *
   * @param typeName
   * @return
   */
  public static GraphQLInputObjectType getScalarFilter(String typeName) {
    switch (typeName) {
      case "ID":
        return OperatorType.getIdFilterInput();
      case "Float":
        return OperatorType.getFloatFilterInput();
      case "Boolean":
        return OperatorType.getBooleanFilterInput();
      case "String":
      case "Date":
      case "DateTime":
      case "Time":
      case "URL":
      case "Email":
      case "LastUpdate":
      case "CreatedAt":
        return OperatorType.getStringFilterInput();
      case "Int":
        return OperatorType.getIntFilterInput();
      case "Long":
        return OperatorType.getLongFilterInput();
      case "Short":
        return OperatorType.getShortFilterInput();
      case "BigInteger":
        return OperatorType.getStringEqFilterInput();
      case "BigDecimal":
        return OperatorType.getBigDecimalFilterInput();
      case "Char":
      case "Byte":
      case "Object":
      case "JSON":
        return null;
      default:
        throw new BusinessException("E10050");
    }
  }

  /**
   * 获取标量类型列表对应的过滤器
   *
   * @param typeName
   * @return
   */
  public static GraphQLInputObjectType getScalarListFilter(String typeName) {
    switch (typeName) {
      case "ID":
        return OperatorType.getIDListWhereInput();
      case "Float":
        return OperatorType.getFloatListWhereInput();
      case "Boolean":
      case "Char":
      case "Object":
      case "JSON":
      case "BigInteger":
      case "Byte":
        return null;
      case "String":
      case "Date":
      case "DateTime":
      case "Time":
      case "URL":
      case "Email":
      case "LastUpdate":
      case "CreatedAt":
        return OperatorType.getStringListWhereInput();
      case "Int":
        return OperatorType.getIntListWhereInput();
      case "Long":
        return OperatorType.getLongListWhereInput();
      case "Short":
        return OperatorType.getShortListWhereInput();
      case "BigDecimal":
        return OperatorType.getBigDecimalListWhereInput();
      default:
        throw new BusinessException("E10050");
    }
  }

  /**
   * 获取内嵌对象列表的过滤器
   *
   * @param typeName
   * @return
   */
  public static GraphQLInputObjectType getRefObjectListFilter(@NonNull String typeName) {
    return GraphQLInputObjectType.newInputObject()
        .name(typeName + GRAPHQL_LISTMATCH_POSTFIX)
        .field(
            GraphQLInputObjectField.newInputObjectField()
                .name(GRAPHQL_FILTER_HASONE_OPERATOR)
                .type(GraphQLTypeReference.typeRef(getTypeNameWhereInput(typeName))))
        .build();
  }

  /**
   * 获取内嵌对象的过滤器
   *
   * @param typeName
   * @return
   */
  public static GraphQLTypeReference getRefObjectType(@NonNull String typeName) {
    return GraphQLTypeReference.typeRef(typeName);
  }

  /**
   * 获取对象的Input引用
   *
   * @param typeName
   * @return
   */
  public static GraphQLTypeReference getRefObjectTypeInput(@NonNull String typeName) {
    return GraphQLTypeReference.typeRef(typeName + GRAPHQL_INPUT_POSTFIX);
  }

  /**
   * 获取对象的UpdateInput引用
   *
   * @param typeName
   * @return
   */
  public static GraphQLTypeReference getRefObjectTypeUpdateInput(@NonNull String typeName) {
    return GraphQLTypeReference.typeRef(typeName + GRAPHQL_UPDATEINPUT_POSTFIX);
  }

  /**
   * 获取对象的WhereInput引用
   *
   * @param typeName
   * @return
   */
  public static GraphQLTypeReference getRefObjectTypeWhereInput(@NonNull String typeName) {
    return GraphQLTypeReference.typeRef(typeName + GRAPHQL_WHEREINPUT_POSTFIX);
  }

  /**
   * 构造单实例查询的查询API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameSelectById(@NonNull String objectName) {
    return objectName;
  }

  /**
   * 构造条件查询的API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameSelectByCondition(@NonNull String objectName) {
    return objectName + MANY_POSTFIX;
  }

  /**
   * 构造 更新的API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameUpadte(@NonNull String objectName) {
    return objectName + UPDATE_POSTFIX;
  }

  /**
   * 构造 创建接口的API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameCreate(@NonNull String objectName) {
    return objectName + CREATE_POSTFIX;
  }

  /**
   * 构造销毁接口的 API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameDestory(@NonNull String objectName) {
    return objectName + DESTROY_POSTFIX;
  }

  /**
   * 构造监听接口的API名字
   *
   * @param objectName
   * @return
   */
  public static String getNameSubscription(@NonNull String objectName) {
    return objectName + SUBSCRIPTION_POSTFIX;
  }

  /**
   * 构造添加自定义字段类型参数的接口名字
   *
   * @param relationField
   * @return
   */
  public static String getNestFieldNameFromIDInput(@NonNull RelationField relationField) {
    return relationField.getFromField()
        + "In"
        + relationField.getFromObject()
        + GRAPHQL_NESTIDINPUT_POSTFIX;
  }
  /**
   * 构造添加自定义字段类型参数的接口名字
   *
   * @param relationField
   * @return
   */
  public static String getNestFieldNameFromObjectInput(@NonNull RelationField relationField) {
    return relationField.getFromField()
        + "In"
        + relationField.getFromObject()
        + GRAPHQL_NESTOBJECTINPUT_POSTFIX;
  }

  /**
   * 构造添加自定义字段类型参数的接口名字
   *
   * @param relationField
   * @return
   */
  public static String getNestFieldNameToIDInput(@NonNull RelationField relationField) {
    return relationField.getToField()
        + "In"
        + relationField.getToObject()
        + GRAPHQL_NESTIDINPUT_POSTFIX;
  }

  public static String getNestFieldNameFromRemove(@NonNull RelationField relationField) {
    return relationField.getFromField()
        + "In"
        + relationField.getFromObject()
        + GRAPHQL_NESTREMOVE_POSTFIX;
  }

  public static String getNestFieldNameToRemove(@NonNull RelationField relationField) {
    return relationField.getToField()
        + "In"
        + relationField.getToObject()
        + GRAPHQL_NESTREMOVE_POSTFIX;
  }

  /**
   * 构造删除自定义字段类型参数的接口名字
   *
   * @param relationField
   * @return
   */
  public static String getNameFieldInContentTypeRemove(@NonNull RelationField relationField) {
    return relationField.getFromField()
        + "In"
        + relationField.getFromObject()
        + GRAPHQL_REMOVEINPUT_POSTFIX;
  }

  /**
   * 输入类型名
   *
   * @param objectName
   * @return
   */
  public static String getTypeNameInput(@NonNull String objectName) {
    return objectName + GRAPHQL_INPUT_POSTFIX;
  }

  /**
   * 过滤条件Where类型名
   *
   * @param objectName
   * @return
   */
  public static String getTypeNameWhereInput(@NonNull String objectName) {
    return objectName + GRAPHQL_WHEREINPUT_POSTFIX;
  }

  /**
   * 更新输入对象名
   *
   * @param objectName
   * @return
   */
  public static String getTypeNameUpdateInput(@NonNull String objectName) {
    return objectName + GRAPHQL_UPDATEINPUT_POSTFIX;
  }

  /**
   * 过滤条件FieldFilter名字
   *
   * @param objectName
   * @return
   */
  public static String getTypeNameFieldFilter(@NonNull String objectName) {
    return objectName + GRAPHQL_FIELDFILTER_POSTFIX;
  }

  /**
   * 类型的ID字段
   *
   * @param objectName
   * @return
   */
  public static String getIDFieldOfObject(@NonNull String objectName) {
    return (objectName + FIELD_WORD_SEPERATOR + GRAPHQL_ID_FIELDNAME).toLowerCase();
  }

  public static GraphQLDirective getSchemaMetaData(SchemaObject schemaObject) {
    HashMap schemaMetaInfo = new HashMap(4);
    schemaMetaInfo.put(GRAPHQL_ID_FIELDNAME, schemaObject.getId());
    schemaMetaInfo.put(GRAPHQL_NAME_FIELDNAME, schemaObject.getName());
    schemaMetaInfo.put(GRAPHQL_DATABASEKIND_FIELDNAME, schemaObject.getDatabasekind());
    schemaMetaInfo.put(GRAPHQL_DATASOURCE_FIELDNAME, schemaObject.getDatasourceinfo());
    return GraphQLDirective.newDirective()
        .name(GRAPHQL_SCHEMAMETADATA)
        .argument(
            GraphQLArgument.newArgument()
                .name(GRAPHQL_DIRECTIVE_METADATA)
                .type(Scalars.GraphQLString)
                .value(JSONObject.toJSONString(schemaMetaInfo)))
        .build();
  }

  public static SchemaData transferSchema(SchemaObject schemaObject) {
    if (schemaObject.getId().equals(GRAPHQL_SCHEMA_ID_DEFAULT)) {
      return transferSchemaforSchemaDb(schemaObject);
    }
    SchemaData schemaData = new SchemaData();
    schemaData.setSchemaid(schemaObject.getId());
    schemaData.setSchemaname(schemaObject.getName());
    schemaData.setDatabasekind(schemaObject.getDatabasekind());
    schemaData.setDatasourceinfo(schemaObject.getDatasourceinfo());
    HashMap enumInfoMap = new HashMap();
    for (EnumTypeMetaData enumType : schemaObject.getEnumtypes()) {
      if (!isValidTypeName(enumType.getName())) {
        throw new BusinessException("E10057");
      }
      if (enumType.getValues().size() < 1) {
        throw new BusinessException("E10058");
      }
      enumInfoMap.put(enumType.getName(), enumType);
    }
    if (null == enumInfoMap.get(GRAPHQL_ROLE_ENUMNAME)) {
      throw new BusinessException("E10059");
    }
    HashSet<String> roles = new HashSet<>();
    EnumTypeMetaData roleMetaData = (EnumTypeMetaData) enumInfoMap.get(GRAPHQL_ROLE_ENUMNAME);
    for (EnumElement roleEnum : roleMetaData.getValues()) {
      roles.add(roleEnum.getValue());
    }
    schemaData.setEnuminfo(enumInfoMap);
    HashMap<String, ObjectTypeMetaData> objectTypeMap = new HashMap();
    HashMap queryAPIMap = new HashMap();
    HashMap mutationAPIMap = new HashMap();
    HashMap subscriptionAPIMap = new HashMap();
    schemaData.setMutationMetaData(mutationAPIMap);
    schemaData.setQueryMetaData(queryAPIMap);
    schemaData.setSubscriptionMetaData(subscriptionAPIMap);
    schemaData.setObjectMetaData(objectTypeMap);
    HashMap<String, HashMap<String, RelationField>> fromRelationFields = new HashMap<>();
    HashMap<String, HashMap<String, RelationField>> toRelationFields = new HashMap<>();
    if (null != schemaObject.getRelations()) {
      for (RelationField relationField : schemaObject.getRelations()) {
        if (null == fromRelationFields.get(relationField.getFromObject())) {
          fromRelationFields.put(relationField.getFromObject(), new HashMap<>());
        }
        if (null == toRelationFields.get(relationField.getToObject())) {
          toRelationFields.put(relationField.getToObject(), new HashMap<>());
        }
        fromRelationFields
            .get(relationField.getFromObject())
            .put(relationField.getFromField(), relationField);
        toRelationFields
            .get(relationField.getToObject())
            .put(relationField.getToField(), relationField);
      }
    }
    for (ObjectTypeInfo objectTypeInfo : schemaObject.getObjecttypes()) {
      ObjectTypeMetaData objectTypeMetaData = new ObjectTypeMetaData();
      objectTypeMetaData.setReadConstraints(new HashMap<>());
      objectTypeMetaData.setUpdateConstraints(new HashMap<>());
      objectTypeMetaData.setDeleteConstraints(new HashMap<>());
      objectTypeMetaData.setUniqueConstraints(new ArrayList<>());
      String objectName = objectTypeInfo.getName();
      objectTypeMap.put(objectName, objectTypeMetaData);
      objectTypeMetaData.setId(objectTypeInfo.getId());
      objectTypeMetaData.setTableName(GRAPHQL_TABLENAME_PREFIX + objectName.toLowerCase());
      objectTypeMetaData.setFieldFilterName(getTypeNameFieldFilter(objectName));
      objectTypeMetaData.setWhereInputObjectName(getTypeNameWhereInput(objectName));
      objectTypeMetaData.setInputObjectName(getTypeNameInput(objectName));
      objectTypeMetaData.setUpdateObjectName(getTypeNameUpdateInput(objectName));
      objectTypeMetaData.setOutPutName(objectName);
      List<String> unReadableRoles = new ArrayList<>();
      objectTypeMetaData.setUnreadableRoles(new ArrayList<>());
      objectTypeMetaData.setUndeletableRoles(new ArrayList<>());
      objectTypeMetaData.setUninsertableRoles(new ArrayList<>());
      objectTypeMetaData.setUnupdatableRoles(new ArrayList<>());
      if (null != objectTypeInfo.getUnreadableRoles()) {
        HashSet rolesSet = new HashSet();
        rolesSet.addAll(objectTypeInfo.getUnreadableRoles());
        if (!roles.containsAll(rolesSet)) {
          throw new BusinessException("E10103");
        }
        rolesSet.addAll(objectTypeInfo.getUnreadableRoles());
        objectTypeMetaData.getUnreadableRoles().addAll(rolesSet);
        unReadableRoles.addAll(rolesSet);
        objectTypeMetaData.getUndeletableRoles().addAll(rolesSet);
        objectTypeMetaData.getUnupdatableRoles().addAll(rolesSet);
        objectTypeMetaData.getUninsertableRoles().addAll(rolesSet);
      }
      if (null != objectTypeInfo.getUndeletableRoles()) {
        HashSet rolesSet = new HashSet();
        rolesSet.addAll(objectTypeInfo.getUndeletableRoles());
        if (!roles.containsAll(rolesSet)) {
          throw new BusinessException("E10103");
        }
        objectTypeMetaData.getUndeletableRoles().addAll(rolesSet);
      }
      if (null != objectTypeInfo.getUnupdatableRoles()) {
        HashSet rolesSet = new HashSet();
        rolesSet.addAll(objectTypeInfo.getUnupdatableRoles());
        if (!roles.containsAll(rolesSet)) {
          throw new BusinessException("E10103");
        }
        objectTypeMetaData.getUnupdatableRoles().addAll(rolesSet);
      }
      if (null != objectTypeInfo.getUninsertableRoles()) {
        HashSet rolesSet = new HashSet();
        rolesSet.addAll(objectTypeInfo.getUninsertableRoles());
        if (!roles.containsAll(rolesSet)) {
          throw new BusinessException("E10103");
        }
        objectTypeMetaData.getUninsertableRoles().addAll(rolesSet);
      }
      if (null != objectTypeInfo.getReadConstraints()) {
        objectTypeMetaData.setReadConstraints(
            HashMap.class.cast(objectTypeInfo.getReadConstraints()));
      }
      if (null != objectTypeInfo.getDeleteConstraints()) {
        objectTypeMetaData.setDeleteConstraints(
            HashMap.class.cast(objectTypeInfo.getDeleteConstraints()));
      }
      if (null != objectTypeInfo.getUpdateConstraints()) {
        objectTypeMetaData.setUpdateConstraints(
            HashMap.class.cast(objectTypeInfo.getUpdateConstraints()));
      }
      if (null != objectTypeInfo.getUniqueConstraints()) {
        objectTypeMetaData.setUniqueConstraints(objectTypeInfo.getUniqueConstraints());
      }
      HashMap<String, ScalarFieldInfo> scalarFields = new HashMap<>();
      HashMap<String, String> fields = new HashMap<>();
      for (ScalarFieldInfo scalarFieldInfo : objectTypeInfo.getScalarFields()) {
        if (fields.containsKey(scalarFieldInfo.getName())) {
          throw new BusinessException("E10060");
        }
        if (null != scalarFieldInfo.getInvisibleRoles()) {
          if (!roles.containsAll(scalarFieldInfo.getInvisibleRoles())) {
            throw new BusinessException("E10103");
          }
        }
        if (null != scalarFieldInfo.getUnmodifiableRoles()) {
          if (!roles.containsAll(scalarFieldInfo.getUnmodifiableRoles())) {
            throw new BusinessException("E10103");
          }
        }
        scalarFields.put(scalarFieldInfo.getName(), scalarFieldInfo);
        fields.put(scalarFieldInfo.getName(), GRAPHQL_SCALARFIELD_TYPENAME);
      }
      scalarFields.put(GRAPHQL_ID_FIELDNAME, defaultIDField);
      fields.put(GRAPHQL_ID_FIELDNAME, GRAPHQL_SCALARFIELD_TYPENAME);
      HashMap<String, EnumField> enumFields = new HashMap<>();
      for (EnumField enumField : objectTypeInfo.getEnumFields()) {
        if (fields.containsKey(enumField.getName())) {
          throw new BusinessException("E10060");
        }
        if (null != enumField.getInvisibleRoles()) {
          if (!roles.containsAll(enumField.getInvisibleRoles())) {
            throw new BusinessException("E10103");
          }
        }
        if (null != enumField.getUnmodifiableRoles()) {
          if (!roles.containsAll(enumField.getUnmodifiableRoles())) {
            throw new BusinessException("E10103");
          }
        }
        enumFields.put(enumField.getName(), enumField);
        fields.put(enumField.getName(), GRAPHQL_ENUMTYPE_TYPENAME);
      }
      objectTypeMetaData.setFromRelationFieldData(fromRelationFields.get(objectName));
      objectTypeMetaData.setToRelationFieldData(toRelationFields.get(objectName));
      if (null != objectTypeMetaData.getFromRelationFieldData()) {
        for (RelationField relationField : objectTypeMetaData.getFromRelationFieldData().values()) {
          fields.put(relationField.getFromField(), GRAPHQL_FROMRELATION_TYPENAME);
          if (null == relationField.getInvisibleRoles()) {
            relationField.setInvisibleRoles(new ArrayList<>());
          } else {
            if (!roles.containsAll(relationField.getInvisibleRoles())) {
              throw new BusinessException("E10103");
            }
          }
          if (null == relationField.getUnmodifiableRoles()) {
            relationField.setUnmodifiableRoles(new ArrayList<>());
          } else {
            if (!roles.containsAll(relationField.getUnmodifiableRoles())) {
              throw new BusinessException("E10103");
            }
          }
          relationField.getUnmodifiableRoles().addAll(relationField.getInvisibleRoles());
        }
      } else {
        objectTypeMetaData.setFromRelationFieldData(new HashMap<>());
      }
      if (null != objectTypeMetaData.getToRelationFieldData()) {
        for (RelationField relationField : objectTypeMetaData.getToRelationFieldData().values()) {
          fields.put(relationField.getToField(), GRAPHQL_TORELATION_TYPENAME);
        }
      } else {
        objectTypeMetaData.setToRelationFieldData(new HashMap<>());
      }
      objectTypeMetaData.setEnumFieldData(enumFields);
      objectTypeMetaData.setScalarFieldData(scalarFields);
      objectTypeMetaData.setFields(fields);
    }
    GraphQLObjectType.Builder queryObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_QUERY_TYPENAME);
    GraphQLObjectType.Builder mutationObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_MUTATION_TYPENAME);
    GraphQLObjectType.Builder listenerObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_SUBSCRIPTION_TYPENAME);
    if (null != schemaObject.getThirdapis()) {
      for (ThirdPartAPIMetaData thirdPartAPIMetaData : schemaObject.getThirdapis()) {
        APIMetaData thirdAPIData = new APIMetaData();
        ThirdAPI thirdAPI = ThirdAPIPool.getThirdAPI(thirdPartAPIMetaData.getApiName());
        thirdAPIData.setApiname(thirdPartAPIMetaData.getApiName());
        thirdAPIData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
        thirdAPIData.setObjectname(null);
        thirdAPIData.setDisabled_roles(thirdPartAPIMetaData.getDisabledRoles());
        mutationAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        GraphQLFieldDefinition.Builder thirdAPIFieldBuilder =
            GraphQLFieldDefinition.newFieldDefinition().name(thirdPartAPIMetaData.getApiName());
        HashMap<String, ThirdAPIField> inputFieldsMap = thirdAPI.inputFields();
        HashMap<String, ThirdAPIField> outputFieldsMap = thirdAPI.outputFields();
        if (null != inputFieldsMap && 0 != inputFieldsMap.size()) {
          Iterator<Map.Entry<String, ThirdAPIField>> inputFieldsIterator =
              inputFieldsMap.entrySet().iterator();
          while (inputFieldsIterator.hasNext()) {
            Map.Entry<String, ThirdAPIField> entry = inputFieldsIterator.next();
            GraphQLArgument.Builder tmpArgBuilder =
                GraphQLArgument.newArgument().name(entry.getKey());
            ThirdAPIField thirdAPIField = entry.getValue();
            GraphQLInputType inputFieldType = null;
            if (null == thirdAPIField.getKind()) {
              throw new BusinessException("E10064");
            }
            if (GRAPHQL_TYPEKIND_SCALAR.equals(thirdAPIField.getKind())) {
              inputFieldType = getScalarType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_ENUM.equals(thirdAPIField.getKind())) {
              inputFieldType = getRefObjectType(thirdAPIField.getType());
            } else {
              throw new BusinessException("E10065");
            }

            if (thirdAPIField.isNotnull()) {
              if (thirdAPIField.isIslist()) {
                tmpArgBuilder.type(GraphQLNonNull.nonNull(GraphQLList.list(inputFieldType)));
              } else {
                tmpArgBuilder.type(GraphQLNonNull.nonNull(inputFieldType));
              }
            } else {
              if (thirdAPIField.isIslist()) {
                tmpArgBuilder.type(GraphQLList.list(inputFieldType));
              } else {
                tmpArgBuilder.type(inputFieldType);
              }
            }
            if (null != thirdAPIField.getDefaultValue()) {
              tmpArgBuilder.defaultValue(getDefaultValue(thirdAPIField));
            }
            thirdAPIFieldBuilder.argument(tmpArgBuilder);
          }
        }

        GraphQLObjectType.Builder thirdAPIOutputBuilder =
            GraphQLObjectType.newObject()
                .name(thirdAPIData.getApiname() + GRAPHQL_THIRDAPI_OUTPUT_POSTFIX);
        if (null != outputFieldsMap) {
          Iterator<Map.Entry<String, ThirdAPIField>> outputFieldsIterator =
              outputFieldsMap.entrySet().iterator();
          while (outputFieldsIterator.hasNext()) {
            Map.Entry<String, ThirdAPIField> entry = outputFieldsIterator.next();
            GraphQLFieldDefinition.Builder tmpThirdField =
                GraphQLFieldDefinition.newFieldDefinition().name(entry.getKey());
            ThirdAPIField thirdAPIField = entry.getValue();
            GraphQLOutputType outputType = null;
            if (null == thirdAPIField.getKind()) {
              throw new BusinessException("E10064");
            }
            if (GRAPHQL_TYPEKIND_SCALAR.equals(thirdAPIField.getKind())) {
              outputType = getScalarType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_ENUM.equals(thirdAPIField.getKind())) {
              outputType = getRefObjectType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_RELATION.equals(thirdAPIField.getKind())) {
              outputType = getRefObjectType(thirdAPIField.getKind());
            } else {
              throw new BusinessException("E10065");
            }
            if (thirdAPIField.isIslist()) {
              tmpThirdField.type((GraphQLList.list(outputType)));
            } else {
              tmpThirdField.type(outputType);
            }
            thirdAPIOutputBuilder.field(tmpThirdField);
          }
          thirdAPIFieldBuilder.type(thirdAPIOutputBuilder);
        } else {
          thirdAPIFieldBuilder.type(ExtendedScalars.Object);
        }
        String thirdAPIKind = ThirdAPIPool.getAPIKind(thirdAPIData.getApiname());
        if (null == thirdAPIKind) {
          if (log.isErrorEnabled()) {
            HashMap errorMap = new HashMap();
            errorMap.put(GRAPHQL_APINAME_FIELDNAME, thirdAPIData.getApiname());
            log.error(
                "{}", LogData.getErrorLog("E10062", errorMap, new BusinessException("E10062")));
          }
        } else if (GRAPHQL_QUERYAPI_FIELDNAME.equals(thirdAPIKind)) {
          queryObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          queryAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        } else if (GRAPHQL_MUTATIONAPI_FIELDNAME.equals(thirdAPIKind)) {
          mutationObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          mutationAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        } else {
          listenerObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          subscriptionAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        }
      }
    }
    ThirdPartAPIMetaData userLoginMetaData = new ThirdPartAPIMetaData();
    APIMetaData userLoginData = new APIMetaData();
    userLoginMetaData.setApiName(GRAPHQL_USERLOGIN_APINAME);
    userLoginData.setApiname(GRAPHQL_USERLOGIN_APINAME);
    userLoginMetaData.setDisabledRoles(new ArrayList<>());
    userLoginData.setDisabled_roles(new ArrayList<>());
    userLoginData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(UserLogin.getDef().withDirective(getThirdAPIMetaData(userLoginMetaData)));
    mutationAPIMap.put(userLoginData.getApiname(),userLoginData);
    ThirdPartAPIMetaData passwordChangeMetaData = new ThirdPartAPIMetaData();
    APIMetaData passwordChangeData = new APIMetaData();
    passwordChangeMetaData.setApiName(GRAPHQL_PASSWORDCHANGE_APINAME);
    passwordChangeData.setApiname(GRAPHQL_PASSWORDCHANGE_APINAME);
    List<String> guestDisableRole = new ArrayList<>();
    guestDisableRole.add(ROLE_GUEST);
    passwordChangeMetaData.setDisabledRoles(guestDisableRole);
    passwordChangeData.setDisabled_roles(guestDisableRole);
    passwordChangeData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(PasswordChange.getDef().withDirective(getThirdAPIMetaData(passwordChangeMetaData)));
    mutationAPIMap.put(passwordChangeData.getApiname(),passwordChangeData);
    ThirdPartAPIMetaData passwordResetMetaData = new ThirdPartAPIMetaData();
    APIMetaData passwordResetData = new APIMetaData();
    passwordResetMetaData.setApiName(GRAPHQL_PASSWORDRESET_APINAME);
    passwordResetData.setApiname(GRAPHQL_PASSWORDRESET_APINAME);
    List<String> adminOnlyRole = new ArrayList<>();
    for(String roleStr:roles) {
      if(!roleStr.equals(ROLE_ADMIN)) {
        adminOnlyRole.add(roleStr);
      }
    }
    passwordResetMetaData.setDisabledRoles(adminOnlyRole);
    passwordResetData.setDisabled_roles(adminOnlyRole);
    passwordChangeData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(PasswordReset.getDef().withDirective(getThirdAPIMetaData(passwordResetMetaData)));
    mutationAPIMap.put(passwordResetData.getApiname(),passwordResetData);
    ThirdPartAPIMetaData rolePromotedMetaData = new ThirdPartAPIMetaData();
    APIMetaData rolePromotedData = new APIMetaData();
    rolePromotedMetaData.setApiName(GRAPHQL_ROLEPROMOTED_APINAME);
    rolePromotedData.setApiname(GRAPHQL_ROLEPROMOTED_APINAME);
    rolePromotedMetaData.setDisabledRoles(adminOnlyRole);
    rolePromotedData.setDisabled_roles(adminOnlyRole);
    rolePromotedData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(
            RolePromoted.getDef()
                    .argument(
                            GraphQLArgument.newArgument()
                                    .name(GRAPHQL_ROLE_FIELDNAME)
                                    .type(GraphQLNonNull.nonNull(getRefObjectType(GRAPHQL_ROLE_ENUMNAME))))
                    .withDirective(getThirdAPIMetaData(rolePromotedMetaData)));
    mutationAPIMap.put(rolePromotedData.getApiname(),rolePromotedData);
    ThirdPartAPIMetaData userCreateMetaData = new ThirdPartAPIMetaData();
    APIMetaData userCreateData = new APIMetaData();
    userCreateMetaData.setApiName(GRAPHQL_USERCreate_APINAME);
    userCreateData.setApiname(GRAPHQL_USERCreate_APINAME);
    userCreateMetaData.setDisabledRoles(new ArrayList<>());
    userCreateData.setDisabled_roles(new ArrayList<>());
    userCreateData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(UserCreate.getDef().withDirective(getThirdAPIMetaData(userCreateMetaData)));
    mutationAPIMap.put(userCreateData.getApiname(),userCreateData);
    GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
    for (EnumTypeMetaData enumType : schemaObject.getEnumtypes()) {
      schemaBuilder.additionalType(getEnumType(enumType));
      schemaBuilder.additionalType(getFieldFilterInputEnumField(enumType));
      schemaBuilder.additionalType(getEnumListFilter(enumType));
    }
    Iterator<Map.Entry<String, ObjectTypeMetaData>> objectTypeIterator =
        schemaData.getObjectMetaData().entrySet().iterator();
    while (objectTypeIterator.hasNext()) {
      Map.Entry<String, ObjectTypeMetaData> entry = objectTypeIterator.next();
      String objectName = entry.getKey();
      ObjectTypeMetaData objecttypetmp = entry.getValue();
      if (objectName.equals(GRAPHQL_USER_TYPENAME)) {
        schemaBuilder.additionalType(userType(objecttypetmp));
        GraphQLInputObjectType userUpdateType =    userTypeUpdateInput(objecttypetmp);
        if(null!=userUpdateType) {
          schemaBuilder.additionalType(userTypeUpdateInput(objecttypetmp));
        }
        schemaBuilder.additionalType(userFieldFilter(objecttypetmp));
      } else {
        schemaBuilder.additionalType(objectTypeGenerator(objecttypetmp));
        schemaBuilder.additionalType(objectTypeFieldFilter(objecttypetmp));
        schemaBuilder.additionalType(objectTypeUpdateInput(objecttypetmp));
      }
      schemaBuilder.additionalType(objectTypeInput(objecttypetmp));
      schemaBuilder.additionalType(objectTypeWhereInput(objecttypetmp));
      schemaBuilder.additionalType(getRefObjectListFilter(objecttypetmp.getOutPutName()));
      schemaBuilder.additionalType(getInsertResult(objecttypetmp));
      schemaBuilder.additionalType(getFieldsSelectMapInput(objecttypetmp));
      if (!objecttypetmp.getUnreadableRoles().containsAll(roles)) {
        String selectByIDAPIName = getNameSelectById(objectName);
        objecttypetmp.setApiNameSelectByID(selectByIDAPIName);
        String subscriptionAPIName = getNameSubscription(objectName);
        objecttypetmp.setApiNameSubscription(subscriptionAPIName);
        String selectAllAPIName = getNameSelectByCondition(objectName);
        objecttypetmp.setApiNameSelectAll(selectAllAPIName);
        if (null == queryAPIMap.get(selectByIDAPIName)) {
          queryObjectTypeBuilder.field(getAPISelectById(objecttypetmp));
          APIMetaData selectByID = new APIMetaData();
          selectByID.setApikind(GRAPHQL_API_KIND_QUERYONE);
          selectByID.setApiname(selectByIDAPIName);
          selectByID.setObjectname(objectName);
          queryAPIMap.put(selectByIDAPIName, selectByID);
        }
        if (null == queryAPIMap.get(selectAllAPIName)) {
          queryObjectTypeBuilder.field(getAPISelectByCondition(objecttypetmp));
          APIMetaData selectByCondition = new APIMetaData();
          selectByCondition.setApikind(GRAPHQL_API_KIND_QUERYMANY);
          selectByCondition.setApiname(selectAllAPIName);
          selectByCondition.setObjectname(objectName);
          queryAPIMap.put(selectAllAPIName, selectByCondition);
        }
        if (null == subscriptionAPIMap.get(subscriptionAPIName)) {
          listenerObjectTypeBuilder.field(getAPISubscription(objecttypetmp));
          APIMetaData subscriptionAPI = new APIMetaData();
          subscriptionAPI.setApikind(GRAPHQL_API_KIND_SUBSCRIPTION);
          subscriptionAPI.setApiname(subscriptionAPIName);
          subscriptionAPI.setObjectname(objectName);
          subscriptionAPIMap.put(subscriptionAPIName, subscriptionAPI);
        }
      }
      if (!objecttypetmp.getUnupdatableRoles().containsAll(roles)) {
        String updateAPIName = getNameUpadte(objectName);
        objecttypetmp.setApiNameUpdate(updateAPIName);
        if (null == mutationAPIMap.get(updateAPIName)) {
          mutationObjectTypeBuilder.field(getAPIUpdate(objecttypetmp));
          APIMetaData updateAPI = new APIMetaData();
          updateAPI.setApikind(GRAPHQL_API_KIND_UPDATE);
          updateAPI.setApiname(updateAPIName);
          updateAPI.setObjectname(objectName);
          mutationAPIMap.put(updateAPIName, updateAPI);
        }
      }
      if (!objecttypetmp.getUninsertableRoles().containsAll(roles) && canCreate(objecttypetmp)) {
        String createAPIName = getNameCreate(objectName);
        objecttypetmp.setApiNameInsert(createAPIName);
        if (null == mutationAPIMap.get(createAPIName)) {
          mutationObjectTypeBuilder.field(getAPICreate(objecttypetmp));
          APIMetaData createAPI = new APIMetaData();
          createAPI.setApikind(GRAPHQL_API_KIND_INSERT);
          createAPI.setApiname(createAPIName);
          createAPI.setObjectname(objectName);
          mutationAPIMap.put(createAPIName, createAPI);
        }
      }
      if (!objecttypetmp.getUndeletableRoles().containsAll(roles)) {
        String destroyAPIName = getNameDestory(objectName);
        objecttypetmp.setApiNameDelete(destroyAPIName);
        if (null == mutationAPIMap.get(destroyAPIName)) {
          APIMetaData deleteAPI = new APIMetaData();
          deleteAPI.setApikind(GRAPHQL_API_KIND_DELETE);
          deleteAPI.setApiname(destroyAPIName);
          deleteAPI.setObjectname(objectName);
          mutationAPIMap.put(destroyAPIName, deleteAPI);
          mutationObjectTypeBuilder.field(getAPIDestory(objecttypetmp));
        }
      }
      for (RelationField relationField : objecttypetmp.getFromRelationFieldData().values()) {
        if (!relationField.getUnmodifiableRoles().containsAll(roles)) {
          String fromIDInputAPIName = getNestFieldNameFromIDInput(relationField);
          if (null == mutationAPIMap.get(fromIDInputAPIName) && !relationField.getIfCascade()) {
            mutationObjectTypeBuilder.field(getAPIFieldFromIDInput(relationField));
            APIMetaData fromIDInputAPIMetaData = new APIMetaData();
            fromIDInputAPIMetaData.setApiname(fromIDInputAPIName);
            fromIDInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_IDINPUT);
            fromIDInputAPIMetaData.setObjectname(objectName);
            mutationAPIMap.put(fromIDInputAPIName, fromIDInputAPIMetaData);
          }
          String toIDInputAPIName = getNestFieldNameToIDInput(relationField);
          if (null == mutationAPIMap.get(toIDInputAPIName) && !relationField.getIfCascade()) {
            mutationObjectTypeBuilder.field(getAPIFieldToIDInput(relationField));
            APIMetaData toIDInputAPIMetaData = new APIMetaData();
            toIDInputAPIMetaData.setObjectname(objectName);
            toIDInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_IDINPUT);
            toIDInputAPIMetaData.setApiname(toIDInputAPIName);
            mutationAPIMap.put(toIDInputAPIName, toIDInputAPIMetaData);
          }
          String fromRemoveAPIName = getNestFieldNameFromRemove(relationField);
          if (null == mutationAPIMap.get(fromRemoveAPIName)) {
            mutationObjectTypeBuilder.field(getAPIFromRemove(relationField));
            APIMetaData fromRemoveAPIMetaData = new APIMetaData();
            fromRemoveAPIMetaData.setObjectname(objectName);
            fromRemoveAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_REMOVE);
            fromRemoveAPIMetaData.setApiname(fromRemoveAPIName);
            mutationAPIMap.put(fromRemoveAPIName, fromRemoveAPIMetaData);
          }
          String toRemoveAPIName = getNestFieldNameToRemove(relationField);
          if (null == mutationAPIMap.get(toRemoveAPIName)) {
            mutationObjectTypeBuilder.field(getAPIToRemove(relationField));
            APIMetaData toRemoveAPIMetaData = new APIMetaData();
            toRemoveAPIMetaData.setObjectname(relationField.getToObject());
            toRemoveAPIMetaData.setApiname(toRemoveAPIName);
            toRemoveAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_REMOVE);
            mutationAPIMap.put(toRemoveAPIName, toRemoveAPIMetaData);
          }
          if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
              || relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
            String fromObjectInputAPIName = getNestFieldNameFromObjectInput(relationField);
            if (null == mutationAPIMap.get(fromObjectInputAPIName)) {
              mutationObjectTypeBuilder.field(getAPIFieldFromObjectInput(relationField));
              APIMetaData fromObjectInputAPIMetaData = new APIMetaData();
              fromObjectInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_OBJECTINPUT);
              fromObjectInputAPIMetaData.setApiname(fromObjectInputAPIName);
              fromObjectInputAPIMetaData.setObjectname(objectName);
              mutationAPIMap.put(fromObjectInputAPIName, fromObjectInputAPIMetaData);
            }
          }
        }
      }
    }
    schemaBuilder
        .query(queryObjectTypeBuilder)
        .mutation(mutationObjectTypeBuilder)
        .subscription(listenerObjectTypeBuilder);
    schemaData.setIdl(directive_data + "\n" + sp.print(schemaBuilder.build()));
    return schemaData;
  }

  public static SchemaData transferSchemaforSchemaDb(SchemaObject schemaObject) {
    SchemaData schemaData = new SchemaData();
    schemaData.setSchemaid(schemaObject.getId());
    schemaData.setSchemaname(schemaObject.getName());
    schemaData.setDatabasekind(schemaObject.getDatabasekind());
    schemaData.setDatasourceinfo(schemaObject.getDatasourceinfo());
    HashMap enumInfoMap = new HashMap();
    for (EnumTypeMetaData enumType : schemaObject.getEnumtypes()) {
      if (!isValidTypeName(enumType.getName())) {
        throw new BusinessException("E10057");
      }
      if (enumType.getValues().size() < 1) {
        throw new BusinessException("E10058");
      }
      enumInfoMap.put(enumType.getName(), enumType);
    }
    if (null == enumInfoMap.get(GRAPHQL_ROLE_ENUMNAME)) {
      throw new BusinessException("E10059");
    }
    List<String> roles = new ArrayList<>();
    EnumTypeMetaData roleMetaData = (EnumTypeMetaData) enumInfoMap.get(GRAPHQL_ROLE_ENUMNAME);
    for (EnumElement roleEnum : roleMetaData.getValues()) {
      roles.add(roleEnum.getValue());
    }
    schemaData.setEnuminfo(enumInfoMap);
    HashMap<String, ObjectTypeMetaData> objectTypeMap = new HashMap();
    HashMap queryAPIMap = new HashMap();
    HashMap mutationAPIMap = new HashMap();
    HashMap subscriptionAPIMap = new HashMap();
    schemaData.setMutationMetaData(mutationAPIMap);
    schemaData.setQueryMetaData(queryAPIMap);
    schemaData.setSubscriptionMetaData(subscriptionAPIMap);
    schemaData.setObjectMetaData(objectTypeMap);
    HashMap<String, HashMap<String, RelationField>> fromRelationFields = new HashMap<>();
    HashMap<String, HashMap<String, RelationField>> toRelationFields = new HashMap<>();
    for (RelationField relationField : schemaObject.getRelations()) {
      if (null == fromRelationFields.get(relationField.getFromObject())) {
        fromRelationFields.put(relationField.getFromObject(), new HashMap<>());
      }
      if (null == toRelationFields.get(relationField.getToObject())) {
        toRelationFields.put(relationField.getToObject(), new HashMap<>());
      }
      fromRelationFields
          .get(relationField.getFromObject())
          .put(relationField.getFromField(), relationField);
      toRelationFields
          .get(relationField.getToObject())
          .put(relationField.getToField(), relationField);
    }
    for (ObjectTypeInfo objectTypeInfo : schemaObject.getObjecttypes()) {
      ObjectTypeMetaData objectTypeMetaData = new ObjectTypeMetaData();
      objectTypeMetaData.setReadConstraints(new HashMap<>());
      objectTypeMetaData.setUpdateConstraints(new HashMap<>());
      objectTypeMetaData.setDeleteConstraints(new HashMap<>());
      objectTypeMetaData.setUniqueConstraints(new ArrayList<>());
      String objectName = objectTypeInfo.getName();
      objectTypeMap.put(objectName, objectTypeMetaData);
      objectTypeMetaData.setId(objectTypeInfo.getId());
      objectTypeMetaData.setTableName(GRAPHQL_TABLENAME_PREFIX + objectName.toLowerCase());
      objectTypeMetaData.setFieldFilterName(getTypeNameFieldFilter(objectName));
      objectTypeMetaData.setWhereInputObjectName(getTypeNameWhereInput(objectName));
      objectTypeMetaData.setInputObjectName(getTypeNameInput(objectName));
      objectTypeMetaData.setUpdateObjectName(getTypeNameUpdateInput(objectName));
      objectTypeMetaData.setOutPutName(objectName);
      List<String> unReadableRoles = new ArrayList<>();
      objectTypeMetaData.setUnreadableRoles(new ArrayList<>());
      objectTypeMetaData.setUndeletableRoles(new ArrayList<>());
      objectTypeMetaData.setUninsertableRoles(new ArrayList<>());
      objectTypeMetaData.setUnupdatableRoles(new ArrayList<>());
      if (null != objectTypeInfo.getUnreadableRoles()) {
        objectTypeMetaData.getUnreadableRoles().addAll(objectTypeInfo.getUnreadableRoles());
        unReadableRoles.addAll(objectTypeInfo.getUnreadableRoles());
        objectTypeMetaData.getUndeletableRoles().addAll(unReadableRoles);
        objectTypeMetaData.getUnupdatableRoles().addAll(unReadableRoles);
        objectTypeMetaData.getUninsertableRoles().addAll(unReadableRoles);
      }
      if (null != objectTypeInfo.getUndeletableRoles()) {
        objectTypeMetaData.getUndeletableRoles().addAll(objectTypeInfo.getUndeletableRoles());
      }
      if (null != objectTypeInfo.getUnupdatableRoles()) {
        objectTypeMetaData.getUnupdatableRoles().addAll(objectTypeInfo.getUnupdatableRoles());
      }
      if (null != objectTypeInfo.getUninsertableRoles()) {
        objectTypeMetaData.getUninsertableRoles().addAll(objectTypeInfo.getUninsertableRoles());
      }
      if (null != objectTypeInfo.getReadConstraints()) {
        objectTypeMetaData.setReadConstraints(
            HashMap.class.cast(objectTypeInfo.getReadConstraints()));
      }
      if (null != objectTypeInfo.getDeleteConstraints()) {
        objectTypeMetaData.setDeleteConstraints(
            HashMap.class.cast(objectTypeInfo.getDeleteConstraints()));
      }
      if (null != objectTypeInfo.getUpdateConstraints()) {
        objectTypeMetaData.setUpdateConstraints(
            HashMap.class.cast(objectTypeInfo.getUpdateConstraints()));
      }
      if (null != objectTypeInfo.getUniqueConstraints()) {
        objectTypeMetaData.setUniqueConstraints(objectTypeInfo.getUniqueConstraints());
      }
      HashMap<String, ScalarFieldInfo> scalarFields = new HashMap<>();
      HashMap<String, String> fields = new HashMap<>();
      for (ScalarFieldInfo scalarFieldInfo : objectTypeInfo.getScalarFields()) {
        if (fields.containsKey(scalarFieldInfo.getName())) {
          throw new BusinessException("E10060");
        }
        scalarFields.put(scalarFieldInfo.getName(), scalarFieldInfo);
        fields.put(scalarFieldInfo.getName(), GRAPHQL_SCALARFIELD_TYPENAME);
      }
      scalarFields.put(GRAPHQL_ID_FIELDNAME, defaultIDField);
      fields.put(GRAPHQL_ID_FIELDNAME, GRAPHQL_SCALARFIELD_TYPENAME);
      HashMap<String, EnumField> enumFields = new HashMap<>();
      for (EnumField enumField : objectTypeInfo.getEnumFields()) {
        if (fields.containsKey(enumField.getName())) {
          throw new BusinessException("E10060");
        }
        enumFields.put(enumField.getName(), enumField);
        fields.put(enumField.getName(), GRAPHQL_ENUMTYPE_TYPENAME);
      }
      objectTypeMetaData.setFromRelationFieldData(fromRelationFields.get(objectName));
      objectTypeMetaData.setToRelationFieldData(toRelationFields.get(objectName));
      if (null != objectTypeMetaData.getFromRelationFieldData()) {
        for (RelationField relationField : objectTypeMetaData.getFromRelationFieldData().values()) {
          fields.put(relationField.getFromField(), GRAPHQL_FROMRELATION_TYPENAME);
          if (null == relationField.getInvisibleRoles()) {
            relationField.setInvisibleRoles(new ArrayList<>());
          }
          if (null == relationField.getUnmodifiableRoles()) {
            relationField.setUnmodifiableRoles(new ArrayList<>());
          }
          relationField.getUnmodifiableRoles().addAll(relationField.getInvisibleRoles());
        }
      } else {
        objectTypeMetaData.setFromRelationFieldData(new HashMap<>());
      }
      if (null != objectTypeMetaData.getToRelationFieldData()) {
        for (RelationField relationField : objectTypeMetaData.getToRelationFieldData().values()) {
          fields.put(relationField.getToField(), GRAPHQL_TORELATION_TYPENAME);
        }
      } else {
        objectTypeMetaData.setToRelationFieldData(new HashMap<>());
      }
      objectTypeMetaData.setEnumFieldData(enumFields);
      objectTypeMetaData.setScalarFieldData(scalarFields);
      objectTypeMetaData.setFields(fields);
    }
    GraphQLObjectType.Builder queryObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_QUERY_TYPENAME);
    GraphQLObjectType.Builder mutationObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_MUTATION_TYPENAME);
    GraphQLObjectType.Builder listenerObjectTypeBuilder =
        GraphQLObjectType.newObject().name(GRAPHQL_SUBSCRIPTION_TYPENAME);
    if (null != schemaObject.getThirdapis()) {
      for (ThirdPartAPIMetaData thirdPartAPIMetaData : schemaObject.getThirdapis()) {
        APIMetaData thirdAPIData = new APIMetaData();
        ThirdAPI thirdAPI = ThirdAPIPool.getThirdAPI(thirdPartAPIMetaData.getApiName());
        thirdAPIData.setApiname(thirdPartAPIMetaData.getApiName());
        thirdAPIData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
        thirdAPIData.setObjectname(null);
        thirdAPIData.setDisabled_roles(thirdPartAPIMetaData.getDisabledRoles());
        mutationAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        GraphQLFieldDefinition.Builder thirdAPIFieldBuilder =
            GraphQLFieldDefinition.newFieldDefinition().name(thirdPartAPIMetaData.getApiName());
        HashMap<String, ThirdAPIField> inputFieldsMap = thirdAPI.inputFields();
        HashMap<String, ThirdAPIField> outputFieldsMap = thirdAPI.outputFields();
        if (null != inputFieldsMap && 0 != inputFieldsMap.size()) {
          Iterator<Map.Entry<String, ThirdAPIField>> inputFieldsIterator =
              inputFieldsMap.entrySet().iterator();
          while (inputFieldsIterator.hasNext()) {
            Map.Entry<String, ThirdAPIField> entry = inputFieldsIterator.next();
            GraphQLArgument.Builder tmpArgBuilder =
                GraphQLArgument.newArgument().name(entry.getKey());
            ThirdAPIField thirdAPIField = entry.getValue();
            GraphQLInputType inputFieldType = null;
            if (null == thirdAPIField.getKind()) {
              throw new BusinessException("E10064");
            }
            if (GRAPHQL_TYPEKIND_SCALAR.equals(thirdAPIField.getKind())) {
              inputFieldType = getScalarType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_ENUM.equals(thirdAPIField.getKind())) {
              inputFieldType = getRefObjectType(thirdAPIField.getType());
            } else {
              throw new BusinessException("E10065");
            }

            if (thirdAPIField.isNotnull()) {
              if (thirdAPIField.isIslist()) {
                tmpArgBuilder.type(GraphQLNonNull.nonNull(GraphQLList.list(inputFieldType)));
              } else {
                tmpArgBuilder.type(GraphQLNonNull.nonNull(inputFieldType));
              }
            } else {
              if (thirdAPIField.isIslist()) {
                tmpArgBuilder.type(GraphQLList.list(inputFieldType));
              } else {
                tmpArgBuilder.type(inputFieldType);
              }
            }
            if (null != thirdAPIField.getDefaultValue()) {
              tmpArgBuilder.defaultValue(getDefaultValue(thirdAPIField));
            }
            thirdAPIFieldBuilder.argument(tmpArgBuilder);
          }
        }
        GraphQLObjectType.Builder thirdAPIOutputBuilder =
            GraphQLObjectType.newObject()
                .name(thirdAPIData.getApiname() + GRAPHQL_THIRDAPI_OUTPUT_POSTFIX);
        if (null != outputFieldsMap) {
          Iterator<Map.Entry<String, ThirdAPIField>> outputFieldsIterator =
              outputFieldsMap.entrySet().iterator();
          while (outputFieldsIterator.hasNext()) {
            Map.Entry<String, ThirdAPIField> entry = outputFieldsIterator.next();
            GraphQLFieldDefinition.Builder tmpThirdField =
                GraphQLFieldDefinition.newFieldDefinition().name(entry.getKey());
            ThirdAPIField thirdAPIField = entry.getValue();
            GraphQLOutputType outputType = null;
            if (null == thirdAPIField.getKind()) {
              throw new BusinessException("E10064");
            }
            if (GRAPHQL_TYPEKIND_SCALAR.equals(thirdAPIField.getKind())) {
              outputType = getScalarType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_ENUM.equals(thirdAPIField.getKind())) {
              outputType = getRefObjectType(thirdAPIField.getType());
            } else if (GRAPHQL_TYPEKIND_RELATION.equals(thirdAPIField.getKind())) {
              outputType = getRefObjectType(thirdAPIField.getKind());
            } else {
              throw new BusinessException("E10065");
            }
            if (thirdAPIField.isIslist()) {
              tmpThirdField.type((GraphQLList.list(outputType)));
            } else {
              tmpThirdField.type(outputType);
            }
            thirdAPIOutputBuilder.field(tmpThirdField);
          }
          thirdAPIFieldBuilder.type(thirdAPIOutputBuilder);
        } else {
          thirdAPIFieldBuilder.type(ExtendedScalars.Object);
        }
        String thirdAPIKind = ThirdAPIPool.getAPIKind(thirdAPIData.getApiname());
        if (null == thirdAPIKind) {
          if (log.isErrorEnabled()) {
            HashMap errorMap = new HashMap();
            errorMap.put(GRAPHQL_APINAME_FIELDNAME, thirdAPIData.getApiname());
            log.error(
                "{}", LogData.getErrorLog("E10062", errorMap, new BusinessException("E10062")));
          }
        } else if (GRAPHQL_QUERYAPI_FIELDNAME.equals(thirdAPIKind)) {
          queryObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          queryAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        } else if (GRAPHQL_MUTATIONAPI_FIELDNAME.equals(thirdAPIKind)) {
          mutationObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          mutationAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        } else {
          listenerObjectTypeBuilder.field(
              thirdAPIFieldBuilder.withDirective(getThirdAPIMetaData(thirdPartAPIMetaData)));
          subscriptionAPIMap.put(thirdAPIData.getApiname(), thirdAPIData);
        }
      }
    }
    ThirdPartAPIMetaData userLoginMetaData = new ThirdPartAPIMetaData();
    APIMetaData userLoginData = new APIMetaData();
    userLoginMetaData.setApiName(GRAPHQL_USERLOGIN_APINAME);
    userLoginData.setApiname(GRAPHQL_USERLOGIN_APINAME);
    userLoginMetaData.setDisabledRoles(new ArrayList<>());
    userLoginData.setDisabled_roles(new ArrayList<>());
    userLoginData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(UserLogin.getDef().withDirective(getThirdAPIMetaData(userLoginMetaData)));
    mutationAPIMap.put(userLoginData.getApiname(),userLoginData);
    ThirdPartAPIMetaData passwordChangeMetaData = new ThirdPartAPIMetaData();
    APIMetaData passwordChangeData = new APIMetaData();
    passwordChangeMetaData.setApiName(GRAPHQL_PASSWORDCHANGE_APINAME);
    passwordChangeData.setApiname(GRAPHQL_PASSWORDCHANGE_APINAME);
    List<String> guestDisableRole = new ArrayList<>();
    guestDisableRole.add(ROLE_GUEST);
    passwordChangeMetaData.setDisabledRoles(guestDisableRole);
    passwordChangeData.setDisabled_roles(guestDisableRole);
    passwordChangeData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(PasswordChange.getDef().withDirective(getThirdAPIMetaData(passwordChangeMetaData)));
    mutationAPIMap.put(passwordChangeData.getApiname(),passwordChangeData);
    ThirdPartAPIMetaData passwordResetMetaData = new ThirdPartAPIMetaData();
    APIMetaData passwordResetData = new APIMetaData();
    passwordResetMetaData.setApiName(GRAPHQL_PASSWORDRESET_APINAME);
    passwordResetData.setApiname(GRAPHQL_PASSWORDRESET_APINAME);
    List<String> adminOnlyRole = new ArrayList<>();
    for(String roleStr:roles) {
      if(!roleStr.equals(ROLE_ADMIN)) {
        adminOnlyRole.add(roleStr);
      }
    }
    passwordResetMetaData.setDisabledRoles(adminOnlyRole);
    passwordResetData.setDisabled_roles(adminOnlyRole);
    passwordChangeData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(PasswordReset.getDef().withDirective(getThirdAPIMetaData(passwordResetMetaData)));
    mutationAPIMap.put(passwordResetData.getApiname(),passwordResetData);
    ThirdPartAPIMetaData rolePromotedMetaData = new ThirdPartAPIMetaData();
    APIMetaData rolePromotedData = new APIMetaData();
    rolePromotedMetaData.setApiName(GRAPHQL_ROLEPROMOTED_APINAME);
    rolePromotedData.setApiname(GRAPHQL_ROLEPROMOTED_APINAME);
    rolePromotedMetaData.setDisabledRoles(adminOnlyRole);
    rolePromotedData.setDisabled_roles(adminOnlyRole);
    rolePromotedData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(
        RolePromoted.getDef()
            .argument(
                GraphQLArgument.newArgument()
                    .name(GRAPHQL_ROLE_FIELDNAME)
                    .type(GraphQLNonNull.nonNull(getRefObjectType(GRAPHQL_ROLE_ENUMNAME))))
            .withDirective(getThirdAPIMetaData(rolePromotedMetaData)));
    mutationAPIMap.put(rolePromotedData.getApiname(),rolePromotedData);
    ThirdPartAPIMetaData userCreateMetaData = new ThirdPartAPIMetaData();
    APIMetaData userCreateData = new APIMetaData();
    userCreateMetaData.setApiName(GRAPHQL_USERCreate_APINAME);
    userCreateData.setApiname(GRAPHQL_USERCreate_APINAME);
    userCreateMetaData.setDisabledRoles(new ArrayList<>());
    userCreateData.setDisabled_roles(new ArrayList<>());
    userCreateData.setApikind(GRAPHQL_API_KIND_THIRDAPI);
    mutationObjectTypeBuilder.field(UserCreate.getDef().withDirective(getThirdAPIMetaData(userCreateMetaData)));
    mutationAPIMap.put(userCreateData.getApiname(),userCreateData);
    GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();

    for (EnumTypeMetaData enumType : schemaObject.getEnumtypes()) {
      schemaBuilder.additionalType(getEnumType(enumType));
      schemaBuilder.additionalType(getFieldFilterInputEnumField(enumType));
      schemaBuilder.additionalType(getEnumListFilter(enumType));
    }
    Iterator<Map.Entry<String, ObjectTypeMetaData>> objectTypeIterator =
        schemaData.getObjectMetaData().entrySet().iterator();
    while (objectTypeIterator.hasNext()) {
      Map.Entry<String, ObjectTypeMetaData> entry = objectTypeIterator.next();
      String objectName = entry.getKey();
      ObjectTypeMetaData objecttypetmp = entry.getValue();
      schemaBuilder.additionalType(objectTypeInput(objecttypetmp));
      if (objectName.equals(GRAPHQL_USER_TYPENAME)) {
        schemaBuilder.additionalType(userType(objecttypetmp));
        schemaBuilder.additionalType(userFieldFilter(objecttypetmp));
        schemaBuilder.additionalType(userTypeUpdateInput(objecttypetmp));
      } else {
        schemaBuilder.additionalType(objectTypeGenerator(objecttypetmp));
        schemaBuilder.additionalType(objectTypeFieldFilter(objecttypetmp));
        if (objectName.equals(GRAPHQL_CONTENTTYPE_TYPENAME)
            || objectName.equals(GRAPHQL_SCALARFIELD_TYPENAME)
            || objectName.equals(GRAPHQL_ENUMFIELD_TYPENAME)) {
          GraphQLInputObjectType.Builder objectTypeUpdateBuilder =
              GraphQLInputObjectType.newInputObject().name(objecttypetmp.getUpdateObjectName());
          Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
              objecttypetmp.getScalarFieldData().entrySet().iterator();
          while (iterator.hasNext()) {
            Map.Entry<String, ScalarFieldInfo> entryContentType = iterator.next();
            if (entryContentType.getKey().equals(GRAPHQL_ID_FIELDNAME)
                || entryContentType.getKey().equals(GRAPHQL_NAME_FIELDNAME)) {
              continue;
            }
            objectTypeUpdateBuilder.field(getUpdateInputScalarField(entryContentType.getValue()));
          }
          Iterator<Map.Entry<String, EnumField>> enumIterator =
              objecttypetmp.getEnumFieldData().entrySet().iterator();
          while (enumIterator.hasNext()) {
            Map.Entry<String, EnumField> entryContentType = enumIterator.next();
            objectTypeUpdateBuilder.field(getUpdateInputEnumField(entryContentType.getValue()));
          }
          schemaBuilder.additionalType(
              objectTypeUpdateBuilder
                  .withDirective(getObjectUpdateInputDirective(objecttypetmp))
                  .build());
        } else if (objectName.equals(GRAPHQL_RELATIONFIELD_TYPENAME)) {
          GraphQLInputObjectType.Builder objectTypeUpdateBuilder =
              GraphQLInputObjectType.newInputObject().name(objecttypetmp.getUpdateObjectName());
          Iterator<Map.Entry<String, ScalarFieldInfo>> iterator =
              objecttypetmp.getScalarFieldData().entrySet().iterator();
          while (iterator.hasNext()) {
            Map.Entry<String, ScalarFieldInfo> entryContentType = iterator.next();
            if (entryContentType.getKey().equals(GRAPHQL_ID_FIELDNAME)
                || entryContentType.getKey().equals(GRAPHQL_FROMFIELD_FIELDNAME)
                || entryContentType.getKey().equals(GRAPHQL_FROMOBJECT_FIELDNAME)
                || entryContentType.getKey().equals(GRAPHQL_FROMFIELD_FIELDNAME)
                || entryContentType.getKey().equals(GRAPHQL_TOOBJECT_FIELDNAME)
                || entryContentType.equals(GRAPHQL_TOFIELD_FIELDNAME)
                || entryContentType.equals("relationtype")) {
              continue;
            }
            objectTypeUpdateBuilder.field(getUpdateInputScalarField(entryContentType.getValue()));
          }
          Iterator<Map.Entry<String, EnumField>> enumIterator =
              objecttypetmp.getEnumFieldData().entrySet().iterator();
          while (enumIterator.hasNext()) {
            Map.Entry<String, EnumField> entryContentType = enumIterator.next();
            objectTypeUpdateBuilder.field(getUpdateInputEnumField(entryContentType.getValue()));
          }
          schemaBuilder.additionalType(
              objectTypeUpdateBuilder
                  .withDirective(getObjectUpdateInputDirective(objecttypetmp))
                  .build());
        } else {
          schemaBuilder.additionalType(objectTypeUpdateInput(objecttypetmp));
        }
      }
      schemaBuilder.additionalType(objectTypeWhereInput(objecttypetmp));
      schemaBuilder.additionalType(getRefObjectListFilter(objecttypetmp.getOutPutName()));
      schemaBuilder.additionalType(getInsertResult(objecttypetmp));
      schemaBuilder.additionalType(getFieldsSelectMapInput(objecttypetmp));
      if (!objecttypetmp.getUnreadableRoles().containsAll(roles)) {
        String selectByIDAPIName = getNameSelectById(objectName);
        objecttypetmp.setApiNameSelectByID(selectByIDAPIName);
        String subscriptionAPIName = getNameSubscription(objectName);
        objecttypetmp.setApiNameSubscription(subscriptionAPIName);
        String selectAllAPIName = getNameSelectByCondition(objectName);
        objecttypetmp.setApiNameSelectAll(selectAllAPIName);
        if (null == queryAPIMap.get(selectByIDAPIName)) {
          queryObjectTypeBuilder.field(getAPISelectById(objecttypetmp));
          APIMetaData selectByID = new APIMetaData();
          selectByID.setApikind(GRAPHQL_API_KIND_QUERYONE);
          selectByID.setApiname(selectByIDAPIName);
          selectByID.setObjectname(objectName);
          queryAPIMap.put(selectByIDAPIName, selectByID);
        }
        if (null == queryAPIMap.get(selectAllAPIName)) {
          queryObjectTypeBuilder.field(getAPISelectByCondition(objecttypetmp));
          APIMetaData selectByCondition = new APIMetaData();
          selectByCondition.setApikind(GRAPHQL_API_KIND_QUERYMANY);
          selectByCondition.setApiname(selectAllAPIName);
          selectByCondition.setObjectname(objectName);
          queryAPIMap.put(selectAllAPIName, selectByCondition);
        }
        if (null == subscriptionAPIMap.get(subscriptionAPIName)) {
          listenerObjectTypeBuilder.field(getAPISubscription(objecttypetmp));
          APIMetaData subscriptionAPI = new APIMetaData();
          subscriptionAPI.setApikind(GRAPHQL_API_KIND_SUBSCRIPTION);
          subscriptionAPI.setApiname(subscriptionAPIName);
          subscriptionAPI.setObjectname(objectName);
          subscriptionAPIMap.put(subscriptionAPIName, subscriptionAPI);
        }
      }
      if (!objecttypetmp.getUnupdatableRoles().containsAll(roles)) {
        String updateAPIName = getNameUpadte(objectName);
        objecttypetmp.setApiNameUpdate(updateAPIName);
        if (null == mutationAPIMap.get(updateAPIName)) {
          mutationObjectTypeBuilder.field(getAPIUpdate(objecttypetmp));
          APIMetaData updateAPI = new APIMetaData();
          updateAPI.setApikind(GRAPHQL_API_KIND_UPDATE);
          updateAPI.setApiname(updateAPIName);
          updateAPI.setObjectname(objectName);
          mutationAPIMap.put(updateAPIName, updateAPI);
        }
      }
      if (!objecttypetmp.getUninsertableRoles().containsAll(roles) && canCreate(objecttypetmp)) {
        String createAPIName = getNameCreate(objectName);
        objecttypetmp.setApiNameInsert(createAPIName);
        if (null == mutationAPIMap.get(createAPIName)) {
          mutationObjectTypeBuilder.field(getAPICreate(objecttypetmp));
          APIMetaData createAPI = new APIMetaData();
          createAPI.setApikind(GRAPHQL_API_KIND_INSERT);
          createAPI.setApiname(createAPIName);
          createAPI.setObjectname(objectName);
          mutationAPIMap.put(createAPIName, createAPI);
        }
      }
      if (!objecttypetmp.getUndeletableRoles().containsAll(roles)) {
        String destroyAPIName = getNameDestory(objectName);
        objecttypetmp.setApiNameDelete(destroyAPIName);
        if (null == mutationAPIMap.get(destroyAPIName)) {
          APIMetaData deleteAPI = new APIMetaData();
          deleteAPI.setApikind(GRAPHQL_API_KIND_DELETE);
          deleteAPI.setApiname(destroyAPIName);
          deleteAPI.setObjectname(objectName);
          mutationAPIMap.put(destroyAPIName, deleteAPI);
          mutationObjectTypeBuilder.field(getAPIDestory(objecttypetmp));
        }
      }
      for (RelationField relationField : objecttypetmp.getFromRelationFieldData().values()) {
        if (!relationField.getUnmodifiableRoles().containsAll(roles)) {
          String fromIDInputAPIName = getNestFieldNameFromIDInput(relationField);
          if (null == mutationAPIMap.get(fromIDInputAPIName) && !relationField.getIfCascade()) {
            mutationObjectTypeBuilder.field(getAPIFieldFromIDInput(relationField));
            APIMetaData fromIDInputAPIMetaData = new APIMetaData();
            fromIDInputAPIMetaData.setApiname(fromIDInputAPIName);
            fromIDInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_IDINPUT);
            fromIDInputAPIMetaData.setObjectname(objectName);
            mutationAPIMap.put(fromIDInputAPIName, fromIDInputAPIMetaData);
          }
          String toIDInputAPIName = getNestFieldNameToIDInput(relationField);
          if (null == mutationAPIMap.get(toIDInputAPIName) && !relationField.getIfCascade()) {
            mutationObjectTypeBuilder.field(getAPIFieldToIDInput(relationField));
            APIMetaData toIDInputAPIMetaData = new APIMetaData();
            toIDInputAPIMetaData.setObjectname(objectName);
            toIDInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_IDINPUT);
            toIDInputAPIMetaData.setApiname(toIDInputAPIName);
            mutationAPIMap.put(toIDInputAPIName, toIDInputAPIMetaData);
          }
          String fromRemoveAPIName = getNestFieldNameFromRemove(relationField);
          if (null == mutationAPIMap.get(fromRemoveAPIName)) {
            mutationObjectTypeBuilder.field(getAPIFromRemove(relationField));
            APIMetaData fromRemoveAPIMetaData = new APIMetaData();
            fromRemoveAPIMetaData.setObjectname(objectName);
            fromRemoveAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_REMOVE);
            fromRemoveAPIMetaData.setApiname(fromRemoveAPIName);
            mutationAPIMap.put(fromRemoveAPIName, fromRemoveAPIMetaData);
          }
          String toRemoveAPIName = getNestFieldNameToRemove(relationField);
          if (null == mutationAPIMap.get(toRemoveAPIName)) {
            mutationObjectTypeBuilder.field(getAPIToRemove(relationField));
            APIMetaData toRemoveAPIMetaData = new APIMetaData();
            toRemoveAPIMetaData.setObjectname(relationField.getToObject());
            toRemoveAPIMetaData.setApiname(toRemoveAPIName);
            toRemoveAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_REMOVE);
            mutationAPIMap.put(toRemoveAPIName, toRemoveAPIMetaData);
          }
          if (relationField.getRelationType().equals(GRAPHQL_ONE2ONE_NAME)
              || relationField.getRelationType().equals(GRAPHQL_ONE2MANY_NAME)) {
            String fromObjectInputAPIName = getNestFieldNameFromObjectInput(relationField);
            if (null == mutationAPIMap.get(fromObjectInputAPIName)) {
              mutationObjectTypeBuilder.field(getAPIFieldFromObjectInput(relationField));
              APIMetaData fromObjectInputAPIMetaData = new APIMetaData();
              fromObjectInputAPIMetaData.setApikind(GRAPHQL_API_KIND_NESTFIELD_OBJECTINPUT);
              fromObjectInputAPIMetaData.setApiname(fromObjectInputAPIName);
              fromObjectInputAPIMetaData.setObjectname(objectName);
              mutationAPIMap.put(fromObjectInputAPIName, fromObjectInputAPIMetaData);
            }
          }
        }
      }
    }
    schemaBuilder
        .query(queryObjectTypeBuilder)
        .mutation(mutationObjectTypeBuilder)
        .subscription(listenerObjectTypeBuilder);
    schemaData.setIdl(directive_data + "\n" + sp.print(schemaBuilder.build()));
    return schemaData;
  }

  public static Map<String, FieldDefinition> fieldMapConstruct(
      List<FieldDefinition> fieldDefinitions) {
    Map<String, FieldDefinition> fieldMaps = new HashMap<>();
    if (null == fieldDefinitions || fieldDefinitions.size() < 1) {
      return fieldMaps;
    } else {
      for (FieldDefinition fieldDef : fieldDefinitions) {
        fieldMaps.put(fieldDef.getName(), fieldDef);
      }
    }
    return fieldMaps;
  }

  public static Boolean canCreate(ObjectTypeMetaData objectTypeMetaData) {
    Map<String, RelationField> toRelationFields = objectTypeMetaData.getToRelationFieldData();
    if (toRelationFields.size() > 0) {
      Iterator<Map.Entry<String, RelationField>> iterator = toRelationFields.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, RelationField> entry = iterator.next();
        RelationField relationField = entry.getValue();
        if (relationField.getIfCascade()) {
          return false;
        }
      }
    }
    return true;
  }
}

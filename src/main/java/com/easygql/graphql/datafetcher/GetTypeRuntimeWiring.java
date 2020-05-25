package com.easygql.graphql.datafetcher;

import com.alibaba.fastjson.JSONObject;
import com.easygql.util.*;
import graphql.Scalars;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.StringValue;
import graphql.language.TypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.*;

import java.util.HashMap;

import static com.easygql.component.ConfigurationProperties.*;

/**
 * @author ：fenyorome
 * @date ：Created in 2019/3/13/013 19:54
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
public class GetTypeRuntimeWiring {


  public static RuntimeWiring getSchemaWiring(SchemaData schemaData, String schemaid) {
    WiringFactory schemaWiringFactory = getSchemaWiringFactory(schemaData, schemaid);
    return RuntimeWiring.newRuntimeWiring().wiringFactory(schemaWiringFactory).build();
  }

  /**
   * @param schemaData
   * @return
   */
  public static WiringFactory getSchemaWiringFactory(SchemaData schemaData, String schemaid) {
    WiringFactory dynamicWiringFactory =
        new WiringFactory() {
          @Override
          public boolean providesScalar(ScalarWiringEnvironment environment) {
            switch (environment.getScalarTypeDefinition().getName()) {
              case GRAPHQL_ID_TYPENAME:
              case GRAPHQL_FLOAT_TYPENAME:
              case GRAPHQL_BOOLEAN_TYPENAME:
              case GRAPHQL_STRING_TYPENAME:
              case GRAPHQL_INT_TYPENAME:
              case GRAPHQL_LONG_TYPENAME:
              case GRAPHQL_SHORT_TYPENAME:
              case GRAPHQL_BYTE_TYPENAME:
              case GRAPHQL_BIGINTEGER_TYPENAME:
              case GRAPHQL_BIGDECIMAL_TYPENAME:
              case GRAPHQL_CHAR_TYPENAME:
              case GRAPHQL_OBJECT_TYPENAME:
              case GRAPHQL_JSON_TYPENAME:
              case GRAPHQL_DATE_TYPENAME:
              case GRAPHQL_DATETIME_TYPENAME:
              case GRAPHQL_TIME_TYPENAME:
              case GRAPHQL_URL_TYPENAME:
              case GRAPHQL_EMAIL_TYPENAME:
              case GRAPHQL_LASTUPDATE_TYPENAME:
              case GRAPHQL_CREATEDAT_TYPENAME:
                return true;
              default:
                return false;
            }
          }

          @Override
          public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
            switch (environment.getScalarTypeDefinition().getName()) {
              case GRAPHQL_ID_TYPENAME:
                return Scalars.GraphQLID;
              case GRAPHQL_FLOAT_TYPENAME:
                return Scalars.GraphQLBigDecimal;
              case GRAPHQL_BOOLEAN_TYPENAME:
                return Scalars.GraphQLBoolean;
              case GRAPHQL_STRING_TYPENAME:
                return Scalars.GraphQLString;
              case GRAPHQL_INT_TYPENAME:
                return Scalars.GraphQLInt;
              case GRAPHQL_LONG_TYPENAME:
                return Scalars.GraphQLLong;
              case GRAPHQL_SHORT_TYPENAME:
                return Scalars.GraphQLInt;
              case GRAPHQL_BYTE_TYPENAME:
                return Scalars.GraphQLString;
              case GRAPHQL_BIGINTEGER_TYPENAME:
                return Scalars.GraphQLLong;
              case GRAPHQL_BIGDECIMAL_TYPENAME:
                return Scalars.GraphQLBigDecimal;
              case GRAPHQL_CHAR_TYPENAME:
                return Scalars.GraphQLString;
              case GRAPHQL_OBJECT_TYPENAME:
                return ExtendedScalars.Object;
              case GRAPHQL_JSON_TYPENAME:
                return ExtendedScalars.Json;
              case GRAPHQL_DATE_TYPENAME:
                return ExtendedScalars.Date;
              case GRAPHQL_DATETIME_TYPENAME:
                return ExtendedScalars.DateTime;
              case GRAPHQL_TIME_TYPENAME:
                return ExtendedScalars.Time;
              case GRAPHQL_URL_TYPENAME:
                return GraphQLElementGenerator.urlType;
              case GRAPHQL_EMAIL_TYPENAME:
                return GraphQLElementGenerator.emailType;
              case GRAPHQL_LASTUPDATE_TYPENAME:
                return LastUpdateScalar.lastUpdateScalar;
              case GRAPHQL_CREATEDAT_TYPENAME:
                return CreatedAtScalar.createdatscalar;
              default:
                return null;
            }
          }

          @Override
          public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
            return false;
          }

          @Override
          public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
            return null;
          }

          @Override
          public boolean providesTypeResolver(UnionWiringEnvironment environment) {
            return false;
          }

          @Override
          public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
            return null;
          }

          @Override
          public boolean providesSchemaDirectiveWiring(
              SchemaDirectiveWiringEnvironment environment) {
            return false;
          }

          @Override
          public SchemaDirectiveWiring getSchemaDirectiveWiring(
              SchemaDirectiveWiringEnvironment environment) {
            return null;
          }

          @Override
          public boolean providesDataFetcher(FieldWiringEnvironment environment) {
            return false;
          }

          @Override
          public DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
            return null;
          }

          @Override
          public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
            TypeDefinition typeDefinition = environment.getParentType();
            if (null == typeDefinition) {
              return null;
            }
            String parentName = typeDefinition.getName();
            FieldDefinition fieldDefinition = environment.getFieldDefinition();
            String fieldName = fieldDefinition.getName();
            if (GRAPHQL_QUERY_TYPENAME.equals(parentName)) {
              APIMetaData apiMetaData = schemaData.getQueryMetaData().get(fieldName);
              switch (apiMetaData.getApikind()) {
                case GRAPHQL_API_KIND_QUERYONE:
                  return new DataFetcherQueryByID(
                      apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_QUERYMANY:
                  return new DataFetcherQueryMany(
                      apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_THIRDAPI:
                  Directive thirdApiDirective =
                      fieldDefinition.getDirective(GRAPHQL_THIRDAPI_METADATA_DIRECTIVE);
                  String thirdApiStr =
                      (StringValue.class.cast(
                              thirdApiDirective.getArgument(GRAPHQL_DIRECTIVE_METADATA).getValue()))
                          .getValue();
                  ThirdPartAPIMetaData thirdPartAPIMetaData =
                      JSONObject.parseObject(thirdApiStr, ThirdPartAPIMetaData.class);
                  return new DataFetcherThirdAPI(
                      schemaData, schemaid, thirdPartAPIMetaData.getApiname());
                default:
                  return null;
              }
            } else if (GRAPHQL_MUTATION_TYPENAME.equals(parentName)) {
              APIMetaData apiMetaData = schemaData.getMutationMetaData().get(fieldName);
              switch (apiMetaData.getApikind()) {
                case GRAPHQL_API_KIND_INSERT:
                  return new DataFetcherCreate(apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_DELETE:
                  return new DataFetcherDeleteMany(
                      apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_UPDATE:
                  return new DataFetcherUpdateMany(
                      apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_NESTFIELD_IDINPUT:
                  Directive fromIDInputDirective =
                      fieldDefinition.getDirective(GRAPHQL_NESTFROMIDINPUT_DIRECTIVE);
                  if (null != fromIDInputDirective) {
                    String nestFromIDStr =
                        (StringValue.class.cast(
                                fromIDInputDirective
                                    .getArgument(GRAPHQL_DIRECTIVE_METADATA)
                                    .getValue()))
                            .getValue();
                    HashMap<String, String> fromIDInputMap =
                        JSONObject.parseObject(nestFromIDStr, HashMap.class);
                    String fromIDObject = fromIDInputMap.get(GRAPHQL_OBJECTNAME_FIELDNAME);
                    String fromIDField = fromIDInputMap.get(GRAPHQL_FIELDNAME_FIELDNAME);
                    RelationField fromIDrelation =
                        schemaData
                            .getObjectMetaData()
                            .get(fromIDObject)
                            .getFromRelationFieldData()
                            .get(fromIDField);
                    if (fromIDrelation.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
                      return new RelationMany2ManyFromIDAdd(fromIDrelation, schemaid, schemaData);
                    } else if (fromIDrelation.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                      return new RelationMany2OneFromIDAdd(fromIDrelation, schemaid, schemaData);
                    } else if (fromIDrelation.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                      return new RelationOne2ManyFromIDAdd(fromIDrelation, schemaid, schemaData);
                    } else {
                      return new RelationOne2OneFromIDAdd(fromIDrelation, schemaid, schemaData);
                    }
                  }
                  Directive toIDInputDirective =
                      fieldDefinition.getDirective(GRAPHQL_NESTTOIDINPUT_DIRECTIVE);
                  if (null != toIDInputDirective) {
                    String nestToIDStr =
                        (StringValue.class.cast(
                                toIDInputDirective
                                    .getArgument(GRAPHQL_DIRECTIVE_METADATA)
                                    .getValue()))
                            .getValue();
                    HashMap<String, String> toIDInputMap =
                        JSONObject.parseObject(nestToIDStr, HashMap.class);
                    String toIDObject = toIDInputMap.get(GRAPHQL_OBJECTNAME_FIELDNAME);
                    String toIDField = toIDInputMap.get(GRAPHQL_FIELDNAME_FIELDNAME);
                    RelationField toIDRelation =
                        schemaData
                            .getObjectMetaData()
                            .get(toIDObject)
                            .getToRelationFieldData()
                            .get(toIDField);
                    if (toIDRelation.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
                      return new RelationMany2ManyToIDAdd(toIDRelation, schemaid, schemaData);
                    } else if (toIDRelation.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                      return new RelationMany2OneToIDAdd(toIDRelation, schemaid, schemaData);
                    } else if (toIDRelation.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                      return new RelationOne2ManyToIDAdd(toIDRelation, schemaid, schemaData);
                    } else {
                      return new RelationOne2OneToIDAdd(toIDRelation, schemaid, schemaData);
                    }
                  }
                  return null;
                case GRAPHQL_API_KIND_NESTFIELD_OBJECTINPUT:
                  Directive fromObjectInputDirective =
                      fieldDefinition.getDirective(GRAPHQL_NESTFROMOBJECTINPUT_DIRECTIVE);
                  if (null != fromObjectInputDirective) {
                    String nestFromObjectStr =
                        (StringValue.class.cast(
                                fromObjectInputDirective
                                    .getArgument(GRAPHQL_DIRECTIVE_METADATA)
                                    .getValue()))
                            .getValue();
                    HashMap<String, String> fromObjectInputMap =
                        JSONObject.parseObject(nestFromObjectStr, HashMap.class);
                    String fromObjectObject = fromObjectInputMap.get(GRAPHQL_OBJECTNAME_FIELDNAME);
                    String fromObjectField = fromObjectInputMap.get(GRAPHQL_FIELDNAME_FIELDNAME);
                    RelationField fromObjectRelation =
                        schemaData
                            .getObjectMetaData()
                            .get(fromObjectObject)
                            .getFromRelationFieldData()
                            .get(fromObjectField);
                    if (fromObjectRelation.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
                      return null;
                    } else if (fromObjectRelation.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                      return null;
                    } else if (fromObjectRelation.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                      return new RelationOne2ManyFromAdd(fromObjectRelation, schemaid, schemaData);
                    } else {
                      return new RelationOne2OneFromAdd(fromObjectRelation, schemaid, schemaData);
                    }
                  }
                  return null;
                case GRAPHQL_API_KIND_NESTFIELD_REMOVE:
                  Directive fromRemoveDirective =
                      fieldDefinition.getDirective(GRAPHQL_NESTFROMREMOVE_DIRECTIVE);
                  if (null != fromRemoveDirective) {
                    String fromRemoveStr =
                        (StringValue.class.cast(
                                fromRemoveDirective
                                    .getArgument(GRAPHQL_DIRECTIVE_METADATA)
                                    .getValue()))
                            .getValue();
                    HashMap<String, String> fromRemoveMap =
                        JSONObject.parseObject(fromRemoveStr, HashMap.class);
                    String fromRemoveObject = fromRemoveMap.get(GRAPHQL_OBJECTNAME_FIELDNAME);
                    String fromRemoveField = fromRemoveMap.get(GRAPHQL_FIELDNAME_FIELDNAME);
                    RelationField fromRemoveRelation =
                        schemaData
                            .getObjectMetaData()
                            .get(fromRemoveObject)
                            .getFromRelationFieldData()
                            .get(fromRemoveField);
                    if (fromRemoveRelation.getFromobject().equals(fromRemoveObject)) {
                      if (fromRemoveRelation.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
                        return new RelationMany2ManyFromRemover(
                            fromRemoveRelation, schemaid, schemaData);
                      } else if (fromRemoveRelation
                          .getRelationtype()
                          .equals(GRAPHQL_MANY2ONE_NAME)) {
                        return new RelationMany2OneFromRemover(
                            fromRemoveRelation, schemaid, schemaData);
                      } else if (fromRemoveRelation
                          .getRelationtype()
                          .equals(GRAPHQL_ONE2MANY_NAME)) {
                        return new RelationOne2ManyFromRemover(
                            fromRemoveRelation, schemaid, schemaData);
                      } else {
                        return new RelationOne2OneFromRemover(
                            fromRemoveRelation, schemaid, schemaData);
                      }
                    }
                  }
                  Directive toRemoveDirective =
                      fieldDefinition.getDirective(GRAPHQL_NESTTOREMOVE_DIRECTIVE);
                  if (null != toRemoveDirective) {
                    String toRemoveStr =
                        (StringValue.class.cast(
                                toRemoveDirective
                                    .getArgument(GRAPHQL_DIRECTIVE_METADATA)
                                    .getValue()))
                            .getValue();
                    HashMap<String, String> toRemoveMap =
                        JSONObject.parseObject(toRemoveStr, HashMap.class);
                    String toRemoveObject = toRemoveMap.get(GRAPHQL_OBJECTNAME_FIELDNAME);
                    String toRemoveField = toRemoveMap.get(GRAPHQL_FIELDNAME_FIELDNAME);
                    RelationField toRemoveRelation =
                        schemaData
                            .getObjectMetaData()
                            .get(toRemoveObject)
                            .getToRelationFieldData()
                            .get(toRemoveField);
                    if (toRemoveRelation.getFromobject().equals(toRemoveObject)) {
                      if (toRemoveRelation.getRelationtype().equals(GRAPHQL_MANY2MANY_NAME)) {
                        return new RelationMany2ManyFromRemover(
                            toRemoveRelation, schemaid, schemaData);
                      } else if (toRemoveRelation.getRelationtype().equals(GRAPHQL_MANY2ONE_NAME)) {
                        return new RelationMany2OneFromRemover(
                            toRemoveRelation, schemaid, schemaData);
                      } else if (toRemoveRelation.getRelationtype().equals(GRAPHQL_ONE2MANY_NAME)) {
                        return new RelationOne2ManyFromRemover(
                            toRemoveRelation, schemaid, schemaData);
                      } else {
                        return new RelationOne2OneFromRemover(
                            toRemoveRelation, schemaid, schemaData);
                      }
                    }
                  }
                  return null;
                case GRAPHQL_API_KIND_THIRDAPI:
                  Directive thirdApiDirective =
                      fieldDefinition.getDirective(GRAPHQL_THIRDAPI_METADATA_DIRECTIVE);
                  String thirdApiStr =
                      (StringValue.class.cast(
                              thirdApiDirective.getArgument(GRAPHQL_DIRECTIVE_METADATA).getValue()))
                          .getValue();
                  ThirdPartAPIMetaData thirdPartAPIMetaData =
                      JSONObject.parseObject(thirdApiStr, ThirdPartAPIMetaData.class);
                  return new DataFetcherThirdAPI(
                      schemaData, schemaid, thirdPartAPIMetaData.getApiname());
                default:
                  return null;
              }
            } else if (GRAPHQL_SUBSCRIPTION_TYPENAME.equals(parentName)) {
              APIMetaData apiMetaData = schemaData.getSubscriptionMetaData().get(fieldName);
              switch (apiMetaData.getApikind()) {
                case GRAPHQL_API_KIND_SUBSCRIPTION:
                  return new DataFetcherSub(apiMetaData.getObjectname(), schemaData, schemaid);
                case GRAPHQL_API_KIND_THIRDAPI:
                  Directive thirdApiDirective =
                      fieldDefinition.getDirective(GRAPHQL_THIRDAPI_METADATA_DIRECTIVE);
                  String thirdApiStr =
                      (StringValue.class.cast(
                              thirdApiDirective.getArgument(GRAPHQL_DIRECTIVE_METADATA).getValue()))
                          .getValue();
                  ThirdPartAPIMetaData thirdPartAPIMetaData =
                      JSONObject.parseObject(thirdApiStr, ThirdPartAPIMetaData.class);
                  return new DataFetcherThirdAPI(
                      schemaData, schemaid, thirdPartAPIMetaData.getApiname());
                default:
                  return null;
              }
            } else {
              return null;
            }
          }
        };
    return dynamicWiringFactory;
  }
}

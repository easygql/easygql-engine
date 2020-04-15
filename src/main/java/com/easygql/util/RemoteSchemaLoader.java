package com.easygql.util;

import com.alibaba.fastjson.JSONObject;
import com.easygql.exception.BusinessException;
import graphql.Assert;
import graphql.introspection.IntrospectionQuery;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.*;
import graphql.schema.idl.ScalarInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.easygql.component.ConfigurationProperties.GRAPHQL_ENDPOINT_FIELDNAME;
import static com.easygql.component.ConfigurationProperties.GRAPHQL_HEADERS_FIELDNAME;

@Slf4j
public class RemoteSchemaLoader {
  public static IntrospectionResultToSchema introspectionResultToSchema =
      new IntrospectionResultToSchema();

  public static CompletableFuture<Object> loadRemoteAPI(String endPoint, Object headers) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
        () -> {
          try {
            WebClient.Builder webClientBuilder = WebClient.builder();
            URL url = new URL(endPoint);
            String path = url.getFile().substring(0, url.getFile().lastIndexOf('/'));
            String baseURL = url.getProtocol() + "://" + url.getHost() + path;
            Integer urlIndex = url.getFile().lastIndexOf('/');
            String urlStr = url.getFile().substring(urlIndex + 1);
            webClientBuilder.baseUrl(baseURL);
            webClientBuilder.defaultHeader(
                HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            WebClient webClient = webClientBuilder.build();
            HashMap headerMap = new HashMap();
            if (null != headers) {
              Map headerTmpMap =
                  JSONObject.parseObject(JSONObject.toJSONString(headers), HashMap.class);
              headerMap.putAll(headerTmpMap);
            }
            Iterator<Map.Entry<String, String>> iterator = headerMap.keySet().iterator();
            Consumer<HttpHeaders> httpHeadersConsumer =
                (httpHeaders) -> {
                  while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    String headerKey = entry.getKey();
                    String headerVal = entry.getValue();
                    httpHeaders.add(headerKey, headerVal);
                  }
                };
            Map response =
                webClient
                    .post()
                    .uri(urlStr)
                    .headers(httpHeadersConsumer)
                    .body(Mono.just(IntrospectionQuery.INTROSPECTION_QUERY), String.class)
                    .exchange()
                    .block()
                    .bodyToMono(Map.class)
                    .block();
            if (null == response) {
              future.completeExceptionally(new BusinessException("E10085"));
            } else {
              Object errors = response.get("errors");
              List errorList = ArrayList.class.cast(errors);
              if (null != errorList && errorList.size() > 1) {
                future.completeExceptionally(new BusinessException("E10085"));
              } else {
                Map<String, Object> introspectionResult = (Map) response.get("data");
                Map<String, Object> schema = (Map) introspectionResult.get("__schema");
                Map<String, Object> queryType = (Map) schema.get("queryType");
                Map<String,List<String>> apiMap = new HashMap<>();
                Assert.assertNotNull(queryType, "queryType expected", new Object[0]);
                TypeName query =
                    TypeName.newTypeName().name((String) queryType.get("name")).build();
                String queryName = query.getName();
                apiMap.put(queryName,new ArrayList<>());
                Map<String, Object> mutationType = (Map) schema.get("mutationType");
                String mutationName = null;
                if (mutationType != null) {
                  TypeName mutation =
                      TypeName.newTypeName().name((String) mutationType.get("name")).build();
                  mutationName = mutation.getName();
                  apiMap.put(mutationName,new ArrayList<>());
                }
                Map<String, Object> subscriptionType = (Map) schema.get("subscriptionType");
                String subscriptionName = null;
                if (subscriptionType != null) {
                  TypeName subscription =
                      TypeName.newTypeName().name((String) subscriptionType.get("name")).build();
                  subscriptionName = subscription.getName();
                  apiMap.put(subscriptionName,new ArrayList<>());
                }
                List<Map<String, Object>> types = (List) schema.get("types");
                Iterator var13 = types.iterator();
                while (var13.hasNext()) {
                  Map<String, Object> type = (Map) var13.next();
                  TypeDefinition typeDefinition = createTypeDefinition(type);
                  if(typeDefinition.getName().equals(queryName)||typeDefinition.getName().equals(mutationName)||typeDefinition.getName().equals(subscriptionName)) {
                      ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition)typeDefinition;
                      List<FieldDefinition> fieldDefinitions= objectTypeDefinition.getFieldDefinitions();
                      for(FieldDefinition fieldDef:fieldDefinitions) {
                          apiMap.get(typeDefinition.getName()).add(fieldDef.getName());
                      }
                  }
                }
                future.complete(apiMap);
              }
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              HashMap triggerEventMap = new HashMap();
              triggerEventMap.put(GRAPHQL_ENDPOINT_FIELDNAME, endPoint);
              triggerEventMap.put(GRAPHQL_HEADERS_FIELDNAME, headers);
              log.error("{}", LogData.getErrorLog("E10085", triggerEventMap, e));
            }
            future.complete(false);
          }
        });
    return future;
  }
  public static CompletableFuture<Object> loadRemoteSchema(String endPoint, Object headers) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture.runAsync(
            () -> {
              try {
                WebClient.Builder webClientBuilder = WebClient.builder();
                URL url = new URL(endPoint);
                String path = url.getFile().substring(0, url.getFile().lastIndexOf('/'));
                String baseURL = url.getProtocol() + "://" + url.getHost() + path;
                Integer urlIndex = url.getFile().lastIndexOf('/');
                String urlStr = url.getFile().substring(urlIndex + 1);
                webClientBuilder.baseUrl(baseURL);
                webClientBuilder.defaultHeader(
                        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                WebClient webClient = webClientBuilder.build();
                HashMap headerMap = new HashMap();
                if (null != headers) {
                  Map headerTmpMap =
                          JSONObject.parseObject(JSONObject.toJSONString(headers), HashMap.class);
                  headerMap.putAll(headerTmpMap);
                }
                Iterator<Map.Entry<String, String>> iterator = headerMap.keySet().iterator();
                Consumer<HttpHeaders> httpHeadersConsumer =
                        (httpHeaders) -> {
                          while (iterator.hasNext()) {
                            Map.Entry<String, String> entry = iterator.next();
                            String headerKey = entry.getKey();
                            String headerVal = entry.getValue();
                            httpHeaders.add(headerKey, headerVal);
                          }
                        };
                Map response =
                        webClient
                                .post()
                                .uri(urlStr)
                                .headers(httpHeadersConsumer)
                                .body(Mono.just(IntrospectionQuery.INTROSPECTION_QUERY), String.class)
                                .exchange()
                                .block()
                                .bodyToMono(Map.class)
                                .block();
                if (null == response) {
                  future.completeExceptionally(new BusinessException("E10085"));
                } else {
                  Object errors = response.get("errors");
                  List errorList = ArrayList.class.cast(errors);
                  if (null != errorList && errorList.size() > 1) {
                    future.completeExceptionally(new BusinessException("E10085"));
                  } else {
                    Map<String, Object> introspectionResult = (Map) response.get("data");
                    future.complete(introspectionResult);
                  }
                }
              } catch (Exception e) {
                if (log.isErrorEnabled()) {
                  HashMap triggerEventMap = new HashMap();
                  triggerEventMap.put(GRAPHQL_ENDPOINT_FIELDNAME, endPoint);
                  triggerEventMap.put(GRAPHQL_HEADERS_FIELDNAME, headers);
                  log.error("{}", LogData.getErrorLog("E10085", triggerEventMap, e));
                }
                future.complete(false);
              }
            });
    return future;
  }
  public static TypeDefinition createTypeDefinition(Map<String, Object> type) {
    String kind = (String) type.get("kind");
    String name = (String) type.get("name");
    if (name.startsWith("__")) {
      return null;
    } else {
      byte var5 = -1;
      switch (kind.hashCode()) {
        case -1970038977:
          if (kind.equals("OBJECT")) {
            var5 = 1;
          }
          break;
        case -1854860308:
          if (kind.equals("SCALAR")) {
            var5 = 5;
          }
          break;
        case -1005748967:
          if (kind.equals("INTERFACE")) {
            var5 = 0;
          }
          break;
        case 2133249:
          if (kind.equals("ENUM")) {
            var5 = 3;
          }
          break;
        case 80895663:
          if (kind.equals("UNION")) {
            var5 = 2;
          }
          break;
        case 1659445620:
          if (kind.equals("INPUT_OBJECT")) {
            var5 = 4;
          }
      }

      switch (var5) {
        case 0:
          return createInterface(type);
        case 1:
          return createObject(type);
        case 2:
          return createUnion(type);
        case 3:
          return createEnum(type);
        case 4:
          return createInputObject(type);
        case 5:
          return createScalar(type);
        default:
          return (TypeDefinition)
              Assert.assertShouldNeverHappen("unexpected kind %s", new Object[] {kind});
      }
    }
  }

  private static TypeDefinition createScalar(Map<String, Object> input) {
    String name = (String) input.get("name");
    return ScalarInfo.isStandardScalar(name)
        ? null
        : ScalarTypeDefinition.newScalarTypeDefinition().name(name).build();
  }

  private static UnionTypeDefinition createUnion(Map<String, Object> input) {
    Assert.assertTrue(input.get("kind").equals("UNION"), "wrong input", new Object[0]);
    graphql.language.UnionTypeDefinition.Builder unionTypeDefinition =
        UnionTypeDefinition.newUnionTypeDefinition();
    unionTypeDefinition.name((String) input.get("name"));
    unionTypeDefinition.comments(toComment((String) input.get("description")));
    List<Map<String, Object>> possibleTypes = (List) input.get("possibleTypes");
    Iterator var4 = possibleTypes.iterator();

    while (var4.hasNext()) {
      Map<String, Object> possibleType = (Map) var4.next();
      TypeName typeName = TypeName.newTypeName().name((String) possibleType.get("name")).build();
      unionTypeDefinition.memberType(typeName);
    }

    return unionTypeDefinition.build();
  }

  private static  EnumTypeDefinition createEnum(Map<String, Object> input) {
    Assert.assertTrue(input.get("kind").equals("ENUM"), "wrong input", new Object[0]);
    graphql.language.EnumTypeDefinition.Builder enumTypeDefinition =
        EnumTypeDefinition.newEnumTypeDefinition().name((String) input.get("name"));
    enumTypeDefinition.comments(toComment((String) input.get("description")));
    List<Map<String, Object>> enumValues = (List) input.get("enumValues");
    Iterator var4 = enumValues.iterator();

    while (var4.hasNext()) {
      Map<String, Object> enumValue = (Map) var4.next();
      graphql.language.EnumValueDefinition.Builder enumValueDefinition =
          EnumValueDefinition.newEnumValueDefinition().name((String) enumValue.get("name"));
      enumValueDefinition.comments(toComment((String) enumValue.get("description")));
      createDeprecatedDirective(enumValue, enumValueDefinition);
      enumTypeDefinition.enumValueDefinition(enumValueDefinition.build());
    }
    return enumTypeDefinition.build();
  }

  private static  InterfaceTypeDefinition createInterface(Map<String, Object> input) {
    Assert.assertTrue(input.get("kind").equals("INTERFACE"), "wrong input", new Object[0]);
    graphql.language.InterfaceTypeDefinition.Builder interfaceTypeDefinition =
        InterfaceTypeDefinition.newInterfaceTypeDefinition().name((String) input.get("name"));
    interfaceTypeDefinition.comments(toComment((String) input.get("description")));
    List<Map<String, Object>> fields = (List) input.get("fields");
    interfaceTypeDefinition.definitions(createFields(fields));
    return interfaceTypeDefinition.build();
  }

  private static InputObjectTypeDefinition createInputObject(Map<String, Object> input) {
    Assert.assertTrue(input.get("kind").equals("INPUT_OBJECT"), "wrong input", new Object[0]);
    graphql.language.InputObjectTypeDefinition.Builder inputObjectTypeDefinition =
        InputObjectTypeDefinition.newInputObjectDefinition()
            .name((String) input.get("name"))
            .comments(toComment((String) input.get("description")));
    List<Map<String, Object>> fields = (List) input.get("inputFields");
    List<InputValueDefinition> inputValueDefinitions = createInputValueDefinitions(fields);
    inputObjectTypeDefinition.inputValueDefinitions(inputValueDefinitions);
    return inputObjectTypeDefinition.build();
  }

  private static  ObjectTypeDefinition createObject(Map<String, Object> input) {
    Assert.assertTrue(input.get("kind").equals("OBJECT"), "wrong input", new Object[0]);
    graphql.language.ObjectTypeDefinition.Builder objectTypeDefinition =
        ObjectTypeDefinition.newObjectTypeDefinition().name((String) input.get("name"));
    objectTypeDefinition.comments(toComment((String) input.get("description")));
    if (input.containsKey("interfaces")) {
      objectTypeDefinition.implementz(
          (List)
              ((List) input.get("interfaces"))
                  .stream()
                      .map(
                          obj -> {
                            Map<String, Object> objDef = (Map) obj;
                            return createObject(objDef);
                          })
                      .collect(Collectors.toList()));
    }

    List<Map<String, Object>> fields = (List) input.get("fields");
    objectTypeDefinition.fieldDefinitions(createFields(fields));
    return objectTypeDefinition.build();
  }

  private static List<FieldDefinition> createFields(List<Map<String, Object>> fields) {
    List<FieldDefinition> result = new ArrayList();
    Iterator var3 = fields.iterator();

    while (var3.hasNext()) {
      Map<String, Object> field = (Map) var3.next();
      graphql.language.FieldDefinition.Builder fieldDefinition =
          FieldDefinition.newFieldDefinition().name((String) field.get("name"));
      fieldDefinition.comments(toComment((String) field.get("description")));
      fieldDefinition.type(createTypeIndirection((Map) field.get("type")));
      createDeprecatedDirective(field, fieldDefinition);
      List<Map<String, Object>> args = (List) field.get("args");
      List<InputValueDefinition> inputValueDefinitions = createInputValueDefinitions(args);
      fieldDefinition.inputValueDefinitions(inputValueDefinitions);
      result.add(fieldDefinition.build());
    }

    return result;
  }

  private static void createDeprecatedDirective(
      Map<String, Object> field, NodeDirectivesBuilder nodeDirectivesBuilder) {
    List<Directive> directives = new ArrayList();
    if ((Boolean) field.get("isDeprecated")) {
      String reason = (String) field.get("deprecationReason");
      if (reason == null) {
        reason = "No longer supported";
      }

      Argument reasonArg =
          Argument.newArgument()
              .name("reason")
              .value(StringValue.newStringValue().value(reason).build())
              .build();
      Directive deprecated =
          Directive.newDirective()
              .name("deprecated")
              .arguments(Collections.singletonList(reasonArg))
              .build();
      directives.add(deprecated);
    }

    nodeDirectivesBuilder.directives(directives);
  }

  private static  List<InputValueDefinition> createInputValueDefinitions(List<Map<String, Object>> args) {
    List<InputValueDefinition> result = new ArrayList();

    graphql.language.InputValueDefinition.Builder inputValueDefinition;
    for (Iterator var3 = args.iterator();
        var3.hasNext();
        result.add(inputValueDefinition.build())) {
      Map<String, Object> arg = (Map) var3.next();
      Type argType = createTypeIndirection((Map) arg.get("type"));
      inputValueDefinition =
          InputValueDefinition.newInputValueDefinition()
              .name((String) arg.get("name"))
              .type(argType);
      inputValueDefinition.comments(toComment((String) arg.get("description")));
      String valueLiteral = (String) arg.get("defaultValue");
      if (valueLiteral != null) {
        Value defaultValue = AstValueHelper.valueFromAst(valueLiteral);
        inputValueDefinition.defaultValue(defaultValue);
      }
    }

    return result;
  }

  private static  Type createTypeIndirection(Map<String, Object> type) {
    String kind = (String) type.get("kind");
    byte var4 = -1;
    switch (kind.hashCode()) {
      case -2078184487:
        if (kind.equals("NON_NULL")) {
          var4 = 6;
        }
        break;
      case -1970038977:
        if (kind.equals("OBJECT")) {
          var4 = 1;
        }
        break;
      case -1854860308:
        if (kind.equals("SCALAR")) {
          var4 = 5;
        }
        break;
      case -1005748967:
        if (kind.equals("INTERFACE")) {
          var4 = 0;
        }
        break;
      case 2133249:
        if (kind.equals("ENUM")) {
          var4 = 3;
        }
        break;
      case 2336926:
        if (kind.equals("LIST")) {
          var4 = 7;
        }
        break;
      case 80895663:
        if (kind.equals("UNION")) {
          var4 = 2;
        }
        break;
      case 1659445620:
        if (kind.equals("INPUT_OBJECT")) {
          var4 = 4;
        }
    }

    switch (var4) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        return TypeName.newTypeName().name((String) type.get("name")).build();
      case 6:
        return NonNullType.newNonNullType()
            .type(createTypeIndirection((Map) type.get("ofType")))
            .build();
      case 7:
        return ListType.newListType()
            .type(createTypeIndirection((Map) type.get("ofType")))
            .build();
      default:
        return (Type) Assert.assertShouldNeverHappen("Unknown kind %s", new Object[] {kind});
    }
  }

  private static List<Comment> toComment(String description) {
    if (description == null) {
      return Collections.emptyList();
    } else {
      List<Comment> comments = new ArrayList();
      String[] lines = description.split("\n");
      int lineNumber = 0;
      String[] var5 = lines;
      int var6 = lines.length;

      for (int var7 = 0; var7 < var6; ++var7) {
        String line = var5[var7];
        ++lineNumber;
        Comment comment = new Comment(line, new SourceLocation(lineNumber, 1));
        comments.add(comment);
      }
      return comments;
    }
  }
}

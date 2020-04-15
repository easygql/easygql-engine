package com.easygql;

import com.easygql.handler.GraphQLWebSocketHandler;
import com.easygql.util.GraphQLParameters;
import com.easygql.util.IDTools;
import com.easygql.util.ObjectTypeMetaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SchemaApiControllerTest {
  @Autowired private WebTestClient webTestClient;
  private WebSocketClient webSocketClient;
  private GraphQLWebSocketHandler graphQLWebSocketHandler;
  @Before
  public void setUp(){
    webSocketClient = new ReactorNettyWebSocketClient();
    graphQLWebSocketHandler = new GraphQLWebSocketHandler();
  }


  @Test(timeout = 1000000L)
  public void cpostGraphQL() {
    try {
      TimeUnit.SECONDS.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    String queryIDL =
        "\n  query IntrospectionQuery {\n    __schema {\n      queryType { name }\n      mutationType { name }\n      subscriptionType { name }\n      types {\n        ...FullType\n      }\n      directives {\n        name\n        description\n        locations\n        args {\n          ...InputValue\n        }\n      }\n    }\n  }\n\n  fragment FullType on __Type {\n    kind\n    name\n    description\n    fields(includeDeprecated: true) {\n      name\n      description\n      args {\n        ...InputValue\n      }\n      type {\n        ...TypeRef\n      }\n      isDeprecated\n      deprecationReason\n    }\n    inputFields {\n      ...InputValue\n    }\n    interfaces {\n      ...TypeRef\n    }\n    enumValues(includeDeprecated: true) {\n      name\n      description\n      isDeprecated\n      deprecationReason\n    }\n    possibleTypes {\n      ...TypeRef\n    }\n  }\n\n  fragment InputValue on __InputValue {\n    name\n    description\n    type { ...TypeRef }\n    defaultValue\n  }\n\n  fragment TypeRef on __Type {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n  }\n";
    GraphQLParameters IntrospectGraphQLParameters =
        new GraphQLParameters(queryIDL, "IntrospectionQuery", new HashMap<>());
    webTestClient
        .post()
        .uri("/api/graphql/schemadb")
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .body(BodyInserters.fromObject(IntrospectGraphQLParameters))
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody();
    String mutationSchemaCreate =
        "mutation schemaCreate {\n  SchemaCreate(name:\"test1\",databasekind:PostgreSQL,description:\"test1\"){\n    id_list \n    affected_rows \n }\n}";
    GraphQLParameters schemaCreateParameters =
        new GraphQLParameters(mutationSchemaCreate, "schemaCreate", new HashMap<>());
    Object result =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(schemaCreateParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(result);
    HashMap dataHashMap = (HashMap) result;
    HashMap schemaCreateHashMap = (HashMap) dataHashMap.get("SchemaCreate");
    Object idList = schemaCreateHashMap.get("id_list");
    List<String> idListInfo = (List<String>) idList;
    String schemaID = idListInfo.get(0);
    String chatRoomTypeID = IDTools.getID();
    String messageTypeID = IDTools.getID();
    String mutationContentCreateFormat =
        " mutation objecttypesInSchema_ObjectInput {\n  objecttypesInSchema_ObjectInput(from_id:\"%s\",to_object:[\n  {\n id:\"%s\",   name:\"chatroom\",\n   description:\"chatroom information\"  \n},{\n id:\"%s\",  name:\"message\",\n   description:\" message information\" \n  }\n  ]){\n   inputNestIDList  \n}\n}";
    String mutationContentCreate =
        String.format(mutationContentCreateFormat, schemaID, chatRoomTypeID, messageTypeID);
    GraphQLParameters contentCreateParameters =
        new GraphQLParameters(
            mutationContentCreate, "objecttypesInSchema_ObjectInput", new HashMap<>());
    Object contentCreateResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(contentCreateParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(contentCreateResult);
    String updateSchema =
        " mutation schemaupdate {\n  SchemaUpdate(object:{\n    description:\"test for description update\"\n  }) {\n    affected_rows\n  }\n}";
    GraphQLParameters updateParameters =
        new GraphQLParameters(updateSchema, "schemaupdate", new HashMap<>());
    Object updateResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(updateParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(updateResult);
    String roomNameFieldID = IDTools.getID();
    String roomInfoFieldID = IDTools.getID();
    String membersFieldID = IDTools.getID();
    String mutationChatRoomFieldAddFormat =
        " mutation chatRoomFieldAdd {\n scalarfieldsInContentType_ObjectInput(from_id:\"%s\",to_object:[\n  {\n id:\"%s\",   name:\"roomname\",\n type:String,   description:\"room name\"\n  },{\n  id:\"%s\",  name:\"roominfo\",\n type:String  , description:\" room information\"  \n},{\n  id:\"%s\",  name:\"members\",\n type:Int  , description:\" room information\" \n }  \n]){\n    inputNestIDList \n}\n}";
    String mutationChatRoomFieldAdd =
        String.format(
            mutationChatRoomFieldAddFormat,
            chatRoomTypeID,
            roomNameFieldID,
            roomInfoFieldID,
            membersFieldID);
    GraphQLParameters chatRoomFieldAddParameters =
        new GraphQLParameters(mutationChatRoomFieldAdd, "chatRoomFieldAdd", new HashMap<>());
    Object chatRoomFieldAddResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(chatRoomFieldAddParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(chatRoomFieldAddResult);
    String contentFieldID = IDTools.getID();
    String fromFieldID = IDTools.getID();
    String mutationMessageFieldAddFormat =
        "mutation messageFieldAdd { scalarfieldsInContentType_ObjectInput(from_id:\"%s\",to_object:[  { id:\"%s\",   name:\"content\", type:String,   description:\"content\"  },{id:\"%s\",  name:\"from\", type:ID  , description:\" room information\"  } ]){    inputNestIDList  } }";
    String mutationMessageFieldAdd =
        String.format(mutationMessageFieldAddFormat, messageTypeID, contentFieldID, fromFieldID);
    GraphQLParameters messageFieldAddParameters =
        new GraphQLParameters(mutationMessageFieldAdd, "messageFieldAdd", new HashMap<>());
    Object messageFieldAddResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(messageFieldAddParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(messageFieldAddResult);
    String messageInChatRoomRelationID = IDTools.getID();
    String mutationMessageInChatRoomAddFormat =
        "mutation messageInChatRoomAdd {\n relationsInSchema_ObjectInput(from_id:\"%s\",to_object:[{id:\"%s\",fromfield:\"messages\",fromobject:\"chatroom\",toobject:\"message\",tofield:\"chatroom\",relationtype:One2Many,ifcascade:true }])\n{\n inputNestIDList \n}\n}";
    String mutationMessageInChatRoomAdd =
        String.format(mutationMessageInChatRoomAddFormat, schemaID, messageInChatRoomRelationID);
    GraphQLParameters messageInChatRoomAdd =
        new GraphQLParameters(
            mutationMessageInChatRoomAdd, "messageInChatRoomAdd", new HashMap<>());
    Object messageInChatRoomAddResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(messageInChatRoomAdd))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(messageInChatRoomAddResult);
    String querySchema =
        " query allschemas {\n  SchemaMany {\n    name \n    databasekind\n    thirdapis {\n      apiname\n    }\n    objecttypes {\n      id\n      name\n    }\n  }\n}";
    GraphQLParameters allSchemaParameters =
        new GraphQLParameters(querySchema, "allschemas", new HashMap<>());
    Object allSchemaResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(allSchemaParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(allSchemaResult);
    String mutationSchemaPublishedFormat =
        "mutation schemaPublished{\n SchemaPublish(schemaid:\"%s\") {\n OperationResult \n} \n}";
    String mutationSchemaPublished = String.format(mutationSchemaPublishedFormat, schemaID);
    GraphQLParameters schemaPublished =
        new GraphQLParameters(mutationSchemaPublished, "schemaPublished", new HashMap<>());
    Object schemaPublishedResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(schemaPublished))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(schemaPublishedResult);
    String queryThirdMany = "query thirdAPIQuery \n{\n ThirdAPIMany {\n thirdapis \n} \n}";
    GraphQLParameters thirdAPIMany =
        new GraphQLParameters(queryThirdMany, "thirdAPIQuery", new HashMap<>());
    Object thirdAPIManyResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(thirdAPIMany))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(thirdAPIManyResult);
    String mutationSchemaStartFormat =
        "mutation schemaStarted{\n SchemaStart(schemaid:\"%s\") {\n OperationResult \n} \n}";
    String mutationSchemaStart = String.format(mutationSchemaStartFormat, schemaID);
    GraphQLParameters schemaStarted =
        new GraphQLParameters(mutationSchemaStart, "schemaStarted", new HashMap<>());
    Object schemaStartResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(schemaStarted))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(schemaStartResult);
    String mutationSchemaStopFormat =
        "mutation schemaStoped { SchemaStop(schemaid:\"%s\") { \n OperationResult  \n} \n}";
    String mutationSchemaStop = String.format(mutationSchemaStopFormat, schemaID);
    GraphQLParameters schemaStop =
        new GraphQLParameters(mutationSchemaStop, "schemaStoped", new HashMap<>());
    Object schemaStopResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(schemaStop))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
    assertNotNull(schemaStopResult);
    String deleteSchemaFormat =
        " mutation schemadestroy {\n  SchemaDestroy(where:\n    {\n      filter:{\n        id:{\n          eq:\"%s\"\n        }\n      }\n    }\n  ) {\n    affected_rows\n  }\n}";
    String deleteSchema = String.format(deleteSchemaFormat, schemaID);
    GraphQLParameters deleteParameters =
        new GraphQLParameters(deleteSchema, "schemadestroy", new HashMap<>());
    Object deleteResult =
        webTestClient
            .post()
            .uri("/api/graphql/schemadb")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .body(BodyInserters.fromObject(deleteParameters))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(HashMap.class)
            .returnResult()
            .getResponseBody()
            .get("data");
  }

  @Test
  public void agetGraphIQL() {
    webTestClient
        .get()
        .uri("/dataexplorer/schemadb")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.TEXT_HTML_VALUE);
  }
}

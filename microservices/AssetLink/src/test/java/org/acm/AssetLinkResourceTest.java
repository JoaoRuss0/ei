package org.acm;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.inject.Inject;
import org.acme.KafkaTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AssetLinkResourceTest {

    @Inject MySQLPool client;
    @InjectMock KafkaTopicService topicService;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM AssetLink").execute()
                .flatMap(r -> client.query("ALTER TABLE AssetLink AUTO_INCREMENT = 1").execute()) // Reset ID counter
                .flatMap(r -> client.query("INSERT INTO AssetLink (id, idProsumer, idUtilityOperator) VALUES (1, 1, 1)").execute())
                .flatMap(r -> client.query("INSERT INTO AssetLink (id, idProsumer, idUtilityOperator) VALUES (2, 2, 1)").execute())
                .flatMap(r -> client.query("INSERT INTO AssetLink (id, idProsumer, idUtilityOperator) VALUES (3, 1, 3)").execute())
                .flatMap(r -> client.query("INSERT INTO AssetLink (id, idProsumer, idUtilityOperator) VALUES (4, 4, 2)").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAll() {
        given()
                .when().get("/AssetLink")
                .then()
                .statusCode(200)
                .body("size()", is(4))
                .body("id", hasItems(1, 2, 3, 4));
    }

    @Test
    void testGetSingleSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/AssetLink/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("idProsumer", is(1))
                .body("idUtilityOperator", is(1));
    }

    @Test
    void testGetSingleNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/AssetLink/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetDualSuccess() {
        given()
                .pathParam("idProsumer", 4)
                .pathParam("idUtilityOperator", 2)
                .when().get("/AssetLink/{idProsumer}/{idUtilityOperator}")
                .then()
                .statusCode(200)
                .body("id", is(4));
    }

    @Test
    void testGetDualNotFound() {
        given()
                .pathParam("idProsumer", 999)
                .pathParam("idUtilityOperator", 999)
                .when().get("/AssetLink/{idProsumer}/{idUtilityOperator}")
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateSuccess() {
        String newLink = """
            {
                "idProsumer": 9,
                "idUtilityOperator": 9
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(newLink)
                .when().post("/AssetLink")
                .then()
                .statusCode(201)
                // Relies on the Uni<Long> fix mentioned above!
                .header("Location", containsString("/AssetLink/"));
    }

    @Test
    void testDeleteSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/AssetLink/{id}")
                .then()
                .statusCode(204);

        given().pathParam("id", 1).when().get("/AssetLink/{id}").then().statusCode(404);
    }

    @Test
    void testDeleteNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/AssetLink/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateTopic() {
        given()
                .pathParam("assetLinkId", 1)
                .pathParam("utilityOperatorName", "ArcoCegoLisbon")
                .when().post("/AssetLink/topic/{prosumerId}/{utilityOperatorId}")
                .then()
                .statusCode(204);

        Mockito.verify(topicService).createAssetLinkTopic(1L, "asd");
    }

    @Test
    void testDeleteTopic() {
        given()
                .pathParam("assetLinkId", 1)
                .pathParam("utilityOperatorName", "ArcoCegoLisbon")
                .when().delete("/AssetLink/topic/{prosumerId}/{utilityOperatorId}")
                .then()
                .statusCode(204);

        Mockito.verify(topicService).deleteAssetLinkTopic(1L, "ArcoCegoLisbon");
    }

    @Test
    void testFindByProsumerIdMultiple() {
        // Seed: (1,1,1) and (3,1,3) both have idProsumer=1
        given()
                .pathParam("idProsumer", 1)
                .when().get("/AssetLink/by-prosumer-id/{idProsumer}")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("id", hasItems(1, 3))
                .body("idUtilityOperator", hasItems(1, 3));
    }

    @Test
    void testFindByProsumerIdSingle() {
        // Seed: only (2,2,1) has idProsumer=2
        given()
                .pathParam("idProsumer", 2)
                .when().get("/AssetLink/by-prosumer-id/{idProsumer}")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(2));
    }

    @Test
    void testFindByProsumerIdEmpty() {
        given()
                .pathParam("idProsumer", 999)
                .when().get("/AssetLink/by-prosumer-id/{idProsumer}")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testFindByUtilityOperatorIdMultiple() {
        // Seed: (1,1,1) and (2,2,1) both have idUtilityOperator=1
        given()
                .pathParam("idUtilityOperator", 1)
                .when().get("/AssetLink/by-utilityoperator-id/{idUtilityOperator}")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("id", hasItems(1, 2))
                .body("idProsumer", hasItems(1, 2));
    }

    @Test
    void testFindByUtilityOperatorIdSingle() {
        // Seed: only (3,1,3) has idUtilityOperator=3
        given()
                .pathParam("idUtilityOperator", 3)
                .when().get("/AssetLink/by-utilityoperator-id/{idUtilityOperator}")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(3));
    }

    @Test
    void testFindByUtilityOperatorIdEmpty() {
        given()
                .pathParam("idUtilityOperator", 999)
                .when().get("/AssetLink/by-utilityoperator-id/{idUtilityOperator}")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }
}
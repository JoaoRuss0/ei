package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UtilityOperatorResourceTest {

    @Inject MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM UtilityOperator").execute()
                .flatMap(r -> client.query("ALTER TABLE UtilityOperator AUTO_INCREMENT = 1").execute())
                .flatMap(r -> client.query("INSERT INTO UtilityOperator (id, name, location, iban) VALUES (1, 'ArcoCegoLisbon', 'Lisboa', '123123123')").execute())
                .flatMap(r -> client.query("INSERT INTO UtilityOperator (id, name, location, iban) VALUES (2, 'PracadeBocage', 'Setubal', '123123123')").execute())
                .flatMap(r -> client.query("INSERT INTO UtilityOperator (id, name, location, iban) VALUES (3, 'PracadaBoavista', 'Porto', '123123123')").execute())
                .flatMap(r -> client.query("INSERT INTO UtilityOperator (id, name, location, iban) VALUES (4, 'PracaDomFranciscoGomes', 'Faro', '123123123')").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAllOperators() {
        given()
                .when().get("/UtilityOperator")
                .then()
                .statusCode(200)
                .body("size()", is(4))
                .body("name", hasItems("ArcoCegoLisbon", "PracadeBocage", "PracadaBoavista", "PracaDomFranciscoGomes"));
    }

    @Test
    void testGetSingleOperatorSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/UtilityOperator/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("name", is("ArcoCegoLisbon"))
                .body("location", is("Lisboa"))
                .body("iban", is("123123123"));
    }

    @Test
    void testGetSingleOperatorNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/UtilityOperator/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateOperator() {
        String newOperatorJson = """
                {
                    "name": "CoimbraEnergy",
                    "location": "Coimbra",
                    "iban": "987654321"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(newOperatorJson)
                .when().post("/UtilityOperator")
                .then()
                .statusCode(201)
                .header("Location", containsString("/UtilityOperator/"))
                .body("id", notNullValue())
                .body("id", greaterThan(0));
    }

    @Test
    void testUpdateOperatorSuccess() {
        // Assuming OperatorUpdateRequest is a Record/DTO with these fields
        String updateJson = """
                {
                    "name": "LisbonHQ_Updated",
                    "location": "Lisboa_Centro",
                    "iban": "111222333"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .pathParam("id", 1)
                .when().put("/UtilityOperator/{id}")
                .then()
                .statusCode(204);

        given()
                .pathParam("id", 1)
                .when().get("/UtilityOperator/{id}")
                .then()
                .statusCode(200)
                .body("name", is("LisbonHQ_Updated"))
                .body("location", is("Lisboa_Centro"))
                .body("iban", is("111222333"));
    }

    @Test
    void testUpdateOperatorNotFound() {
        String updateJson = """
                {
                    "name": "GhostOperator",
                    "location": "Nowhere",
                    "iban": "000000000"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .pathParam("id", 999)
                .when().put("/UtilityOperator/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteOperatorSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/UtilityOperator/{id}")
                .then()
                .statusCode(204);

        given()
                .pathParam("id", 1)
                .when().get("/UtilityOperator/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteOperatorNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/UtilityOperator/{id}")
                .then()
                .statusCode(404);
    }
}

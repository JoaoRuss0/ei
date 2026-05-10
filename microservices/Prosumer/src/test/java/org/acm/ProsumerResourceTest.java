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
class ProsumerResourceTest {

    @Inject
    MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM Prosumer").execute()
                .flatMap(r -> client.query("ALTER TABLE Prosumer AUTO_INCREMENT = 1").execute())
                .flatMap(r -> client.query("INSERT INTO Prosumer (id, name, FiscalNumber, location) VALUES (1, 'client1', 123456, 'Lisbon')").execute())
                .flatMap(r -> client.query("INSERT INTO Prosumer (id, name, FiscalNumber, location) VALUES (2, 'client2', 987654, 'Setúbal')").execute())
                .flatMap(r -> client.query("INSERT INTO Prosumer (id, name, FiscalNumber, location) VALUES (3, 'client3', 123987, 'OPorto')").execute())
                .flatMap(r -> client.query("INSERT INTO Prosumer (id, name, FiscalNumber, location) VALUES (4, 'client4', 987123, 'Faro')").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAllProsumers() {
        given()
                .when().get("/Prosumer")
                .then()
                .statusCode(200)
                .body("size()", is(4))
                .body("name", hasItems("client1", "client2", "client3", "client4"));
    }

    @Test
    void testGetSingleProsumerSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/Prosumer/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("name", is("client1"))
                .body("location", is("Lisbon"))
                .body("FiscalNumber", is(123456));
    }

    @Test
    void testGetSingleProsumerNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/Prosumer/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateProsumer() {
        String newProsumerJson = """
                {
                    "name": "client5",
                    "FiscalNumber": 555555,
                    "location": "Coimbra"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(newProsumerJson)
                .when().post("/Prosumer")
                .then()
                .statusCode(201)
                .header("Location", containsString("/Prosumer/"));
    }

    @Test
    void testUpdateProsumerSuccess() {
        given()
                .pathParam("id", 2)
                .pathParam("name", "client2_updated")
                .pathParam("FiscalNumber", 111222)
                .pathParam("location", "Braga")
                .when().put("/Prosumer/{id}/{name}/{FiscalNumber}/{location}")
                .then()
                .statusCode(204);

        // Verify the update worked
        given()
                .pathParam("id", 2)
                .when().get("/Prosumer/{id}")
                .then()
                .statusCode(200)
                .body("name", is("client2_updated"))
                .body("location", is("Braga"))
                .body("FiscalNumber", is(111222));
    }

    @Test
    void testUpdateProsumerNotFound() {
        given()
                .pathParam("id", 999)
                .pathParam("name", "ghost_client")
                .pathParam("FiscalNumber", 0)
                .pathParam("location", "Nowhere")
                .when().put("/Prosumer/{id}/{name}/{FiscalNumber}/{location}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteProsumerSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/Prosumer/{id}")
                .then()
                .statusCode(204);

        given()
                .pathParam("id", 1)
                .when().get("/Prosumer/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteProsumerNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/Prosumer/{id}")
                .then()
                .statusCode(404);
    }
}
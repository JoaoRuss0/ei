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
class AssetResourceTest {

    @Inject
    MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM Asset").execute()
                .flatMap(r -> client.query("ALTER TABLE Asset AUTO_INCREMENT = 1").execute())
                .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, grid_cell_id, asset_type) VALUES (1, 'asset-1', 1, 'PORTO_NORTH', 'BATTERY')").execute())
                .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, grid_cell_id, asset_type) VALUES (2, 'asset-2', 1, 'PORTO_NORTH', 'BATTERY')").execute())
                .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, grid_cell_id, asset_type) VALUES (3, 'asset-3', 2, 'LISBON_SOUTH', 'SOLAR')").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAllAssets() {
        given()
                .when().get("/Asset")
                .then()
                .statusCode(200)
                .body("size()", is(3))
                .body("name", hasItems("asset-1", "asset-2", "asset-3"));
    }


    @Test
    void testGetSingleAssetSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/Asset/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("name", is("asset-1"))
                .body("type", is("BATTERY"));
    }

    @Test
    void testGetSingleAssetNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/Asset/{id}")
                .then()
                .statusCode(404);
    }


    @Test
    void testCreateAsset() {
        String newAssetJson = """
                {
                    "name": "new-ev-charger",
                    "prosumerId": 5,
                    "gridCellId": "COIMBRA_CENTER",
                    "type": "EV_CHARGER"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(newAssetJson)
                .when().post("/Asset")
                .then()
                .statusCode(201)
                .header("Location", containsString("/Asset/"));
    }


    @Test
    void testUpdateAssetSuccess() {
        String updateJson = """
                {
                    "name": "updated-solar-panel",
                    "prosumerId": 2,
                    "gridCellId": "LISBON_SOUTH",
                    "type": "SOLAR"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .pathParam("id", 3)
                .when().post("/Asset/{id}") // Note: Using POST as defined in your code
                .then()
                .statusCode(204);

        given()
                .pathParam("id", 3)
                .when().get("/Asset/{id}")
                .then()
                .body("name", is("updated-solar-panel"));
    }

    @Test
    void testUpdateAssetNotFound() {
        String updateJson = """
                {
                    "name": "ghost-asset",
                    "prosumerId": 1,
                    "gridCellId": "NONE",
                    "type": "BATTERY"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateJson)
                .pathParam("id", 999)
                .when().post("/Asset/{id}")
                .then()
                .statusCode(404);
    }


    @Test
    void testDeleteAssetSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/Asset/{id}")
                .then()
                .statusCode(204);

        // Verify it is gone
        given()
                .pathParam("id", 1)
                .when().get("/Asset/{id}")
                .then()
                .statusCode(404);
    }


    @Test
    void testGetByGridCellIds() {
        given()
                .queryParam("cellIds", "PORTO_NORTH")
                .queryParam("cellIds", "LISBON_SOUTH")
                .when().get("/Asset/active/by-grid-cell-ids")
                .then()
                .statusCode(200)
                .body("size()", is(3))
                .body("gridCellId", hasItems("PORTO_NORTH", "LISBON_SOUTH"));
    }

    @Test
    void testFindActiveBatteriesByProsumerId() {
        given()
                .pathParam("id", 1)
                .when().get("/Asset/active/by-prosumer/{id}")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].type", is("BATTERY"))
                .body("[1].type", is("BATTERY"));
    }
}
package com.arushi.practicevertx.resources;

import com.arushi.practicevertx.entity.Product;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.ArrayList;
import java.util.List;

public class ProductResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductResources.class);

    public Router getAPISubRouter(Vertx vertx){
        Router apiSubRouter = Router.router(vertx);

        // API routing
        apiSubRouter.route("/*").handler(this::defaultProcessorForAllAPI);
        apiSubRouter.route("/v1/products*").handler(BodyHandler.create());
        apiSubRouter.get("/v1/products").handler(this::getAllProducts);
        apiSubRouter.get("/v1/products/:id").handler(this::getProductById);
        apiSubRouter.post("/v1/products").handler(this::addProduct);
        apiSubRouter.put("/v1/products/:id").handler(this::updateProductById);
        apiSubRouter.delete("/v1/products/:id").handler(this::deleteProductById);

        return apiSubRouter;
    }

    // Called for all default API HTTP GET, POST, PUT and DELETE
    public void defaultProcessorForAllAPI(RoutingContext routingContext) {
        String authToken = routingContext.request().headers().get("AuthToken");

        if(authToken == null || !authToken.equals("123")) {
            LOGGER.info("Failed basic auth check");

            routingContext.response()
                    .setStatusCode(401)
                    .putHeader(HttpHeaders.CONTENT_TYPE,"application/json")
                    .end(Json.encodePrettily(new JsonObject().put("error", "Not Authorized to use these APIs")));

        } else {
            LOGGER.info("Passed basic auth check");

            // Allowing CORS - Cross Domain API calls
            routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE");

            // Call the next matching route
            routingContext.next();
        }
    }


    // Get All Products as array of products
    public void getAllProducts(RoutingContext routingContext) {
        JsonObject responseJson = new JsonObject();

        Product firstItem = new Product("2322333", "123", "Item 1");
        Product secondItem = new Product("34241123", "432", "Item 2");
        List<Product> products = new ArrayList<>();
        products.add(firstItem);
        products.add(secondItem);

        responseJson.put("products", products);

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type","application/json")
                .end(Json.encodePrettily(responseJson));
    }

    // Get one product that matches input id and return as a single Json object
    public void getProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");
        String number = "234";
        Product firstItem = new Product(productId, number, "Item " + number);

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type","application/json")
                .end(Json.encodePrettily(firstItem));
    }

    // Insert a product item passed in from the http post body
    // Return what was added with the unique id from the insert
    public void addProduct(RoutingContext routingContext) {
        JsonObject jsonBody = routingContext.body().asJsonObject();
        String number = jsonBody.getString("number");
        String description = jsonBody.getString("description");

        Product newItem = new Product("", number, description);

        // Add into database & get unique id
        newItem.setId("234978");

        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type","application/json")
                .end(Json.encodePrettily(newItem));
    }

    // Update the item based on the url product id
    // Return updated product info
    public void updateProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");

        JsonObject jsonBody = routingContext.body().asJsonObject();
        String number = jsonBody.getString("number");
        String description = jsonBody.getString("description");

        Product updatedItem = new Product(productId, number, description);

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type","application/json")
                .end(Json.encodePrettily(updatedItem));

    }

    // Delete item and return 200 on success or 400 on failure
    public void deleteProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type","application/json")
                .end();
    }

}

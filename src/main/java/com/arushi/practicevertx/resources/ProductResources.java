package com.arushi.practicevertx.resources;

import com.arushi.practicevertx.database.MongoManager;
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


public class ProductResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductResources.class);
    private Vertx vertx = null;
    private final String serviceName = MongoManager.serviceName();

    public Router getAPISubRouter(Vertx vertx){
        this.vertx = vertx;

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

        JsonObject cmdJson = new JsonObject();
        cmdJson.put("cmd","findAll");

        vertx.eventBus().request(serviceName,
                                        cmdJson.toString(),
                                        asyncResult -> {
                                            if(asyncResult.succeeded()) {
                                                JsonObject reply = new JsonObject(asyncResult.result().body().toString());
                                                System.out.println(serviceName + " :: Got reply --> " + reply.toString());

                                                int statusCode = (reply.containsKey("products")) ? 200 : 400;

                                                routingContext.response()
                                                        .setStatusCode(statusCode)
                                                        .putHeader("content-type","application/json")
                                                        .end(Json.encodePrettily(reply));

                                            }

                                        }
        );

    }

    // Get one product that matches input id and return as a single Json object
    public void getProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");

        JsonObject cmdJson = new JsonObject();
        cmdJson.put("cmd","findById");
        cmdJson.put("id",productId);

        vertx.eventBus().request(serviceName,
                                    cmdJson.toString(),
                                    messageAsyncResult -> {
                                        if(messageAsyncResult.succeeded()) {
                                            JsonObject reply = new JsonObject(messageAsyncResult.result().body().toString());
                                            System.out.println(serviceName + " :: Got reply --> " + reply.toString());

                                            int statusCode = (reply.containsKey("error")) ? 400 : 200;

                                            routingContext.response()
                                                    .setStatusCode(statusCode)
                                                    .putHeader("content-type","application/json")
                                                    .end(Json.encodePrettily(reply));

                                            }

                                        }
                );
    }

    // Insert a product item passed in from the http post body
    // Return what was added with the unique id from the insert
    public void addProduct(RoutingContext routingContext) {

        JsonObject cmdJson = new JsonObject();
        cmdJson.put("cmd","add");

        JsonObject jsonBody = routingContext.body().asJsonObject();
        String number = jsonBody.getString("number");
        String description = jsonBody.getString("description");

        cmdJson.put("product", jsonBody);

        Product newItem = new Product("", number, description);

        vertx.eventBus().request(serviceName,
                                    cmdJson.toString(),
                                    asyncResult -> {
                                        if(asyncResult.succeeded()) {
                                            JsonObject reply = new JsonObject(asyncResult.result().body().toString());
                                            System.out.println(serviceName + " :: Got reply --> " + reply.toString());

                                            int statusCode = 400;

                                            if(reply.containsKey("id")) {
                                                // Get unique id
                                                String newItemId = reply.getString("id");
                                                newItem.setId(newItemId);
                                                statusCode = (!newItemId.isEmpty()) ? 201 : 400;

                                                routingContext.response()
                                                        .setStatusCode(statusCode)
                                                        .putHeader("content-type","application/json")
                                                        .end(Json.encodePrettily(newItem));
                                            }

                                            // Any other error
                                            routingContext.response()
                                                    .setStatusCode(statusCode)
                                                    .putHeader("content-type","application/json")
                                                    .end(Json.encodePrettily(reply));


                                        }

                                    }
                );
    }

    // Update the item based on the url product id
    // Return updated product info
    public void updateProductById(RoutingContext routingContext) {

        final String productId = routingContext.request().getParam("id");

        JsonObject jsonBody = routingContext.body().asJsonObject();


        JsonObject cmdJson = new JsonObject();
        cmdJson.put("cmd","update");
        cmdJson.put("id", productId);
        cmdJson.put("product", jsonBody);


        vertx.eventBus().request(serviceName,
                                    cmdJson.toString(),
                                    asyncResult -> {
                                        if(asyncResult.succeeded()) {
                                            JsonObject reply = new JsonObject(asyncResult.result().body().toString());
                                            System.out.println(serviceName + " :: Got reply --> " + reply.toString());
                                            JsonObject apiResponse = new JsonObject();

                                            int statusCode = 400;

                                            if(reply.containsKey("updated") && reply.getBoolean("updated")) {
                                                statusCode = 200;
                                                apiResponse.put("updated", true);
                                            } else {
                                                apiResponse.put("updated", false);
                                            }

                                            routingContext.response()
                                                    .setStatusCode(statusCode)
                                                    .putHeader("content-type","application/json")
                                                    .end(Json.encodePrettily(apiResponse));

                                        }
                                    }
                );

    }

    // Delete item and return 200 on success or 400 on failure
    public void deleteProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");

        JsonObject cmdJson = new JsonObject();
        cmdJson.put("cmd","delete");
        cmdJson.put("id", productId);

        vertx.eventBus().request(serviceName,
                                    cmdJson.toString(),
                                    asyncResult -> {
                                        if(asyncResult.succeeded()) {
                                            routingContext.response()
                                                    .setStatusCode(200)
                                                    .putHeader("content-type","application/json")
                                                    .end();
                                        }
                                    }

                );
    }

}

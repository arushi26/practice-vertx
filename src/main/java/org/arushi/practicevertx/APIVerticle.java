package org.arushi.practicevertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.arushi.practicevertx.entity.Product;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class APIVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIVerticle.class);
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        // Use config/config.json from resources/classpath
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        configRetriever.getConfig( config -> {
            if(config.succeeded()) {
                JsonObject configJson = config.result();
//                System.out.println(configJson.encodePrettily());

                DeploymentOptions options = new DeploymentOptions().setConfig(configJson);
                vertx.deployVerticle(new APIVerticle(), options);
            }
                }

        );
        System.out.println("Config is read");
    }


    @Override
    public void start() throws Exception {
        LOGGER.info("APIVerticle started");

        Router router = Router.router(vertx);

        // API routing
        router.route("/api*").handler(this::defaultProcessorForAllAPI);
        router.route("/api/v1/products*").handler(BodyHandler.create());
        router.get("/api/v1/products").handler(this::getAllProducts);
        router.get("/api/v1/products/:id").handler(this::getProductById);
        router.post("/api/v1/products").handler(this::addProduct);
        router.put("/api/v1/products/:id").handler(this::updateProductById);
        router.delete("/api/v1/products/:id").handler(this::deleteProductById);


        // Default if no routes matched
        router.route()
                        .handler(routingContext -> {
                            ClassLoader classLoader = getClass().getClassLoader();
                            File file = new File(classLoader.getResource("webroot/home.html").getFile());

                            String mappedHtml = "";
                            try{
                                StringBuilder builder = new StringBuilder("");
                                Scanner scanner = new Scanner(file);

                                while(scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    builder.append(line).append("\n");
                                }

                                scanner.close();

                                mappedHtml = builder.toString();
                                mappedHtml = replaceAllTokens(mappedHtml, "{name}", "Arushi");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            routingContext.response().putHeader("content-type","text/html").end(mappedHtml);
                        });
                //.handler(StaticHandler.create().setCachingEnabled(false));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getInteger("http.port"),
                            httpServerAsyncResult -> {
                                    // Log whether server able to start at given port
                                    if(httpServerAsyncResult.succeeded()) {
                                        LOGGER.info("HTTP server running on port " + config().getInteger("http.port"));
                                    } else {
                                        LOGGER.error("Could not start an HTTP server ", httpServerAsyncResult.cause());
                                    }
                            });
    }

    // Called for all default API HTTP GET, POST, PUT and DELETE
    private void defaultProcessorForAllAPI(RoutingContext routingContext) {
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

    private String replaceAllTokens(String mappedHtml, String token, String replaceWith) {

        while(mappedHtml.contains(token)) {
            mappedHtml = mappedHtml.replace(token, replaceWith);
        }
        return  mappedHtml;
    }

    // Get All Products as array of products
    private void getAllProducts(RoutingContext routingContext) {
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
    private void getProductById(RoutingContext routingContext) {
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
    private void addProduct(RoutingContext routingContext) {
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
    private void updateProductById(RoutingContext routingContext) {
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
    private void deleteProductById(RoutingContext routingContext) {
        final String productId = routingContext.request().getParam("id");

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type","application/json")
                .end();
    }
    @Override
    public void stop() throws Exception {
        LOGGER.info("APIVerticle stopped");
    }
}
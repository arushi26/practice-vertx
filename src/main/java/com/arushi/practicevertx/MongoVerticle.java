package com.arushi.practicevertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class MongoVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoVerticle.class);
    public static MongoClient mongoClient = null;

    public static void main(String[] args) {

        /* Code to deploy cluster -->
        /* deployClusteredVerticle();
        */

        Vertx vertx = Vertx.vertx();

        // Use config/config.json from resources/classpath to get DB connection details
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);

        configRetriever.getConfig( config -> {
                    if(config.succeeded()) {
                        JsonObject configJson = config.result();
                        //                      System.out.println(configJson.encodePrettily());

                        DeploymentOptions options = new DeploymentOptions().setConfig(configJson);
                        vertx.deployVerticle(new MongoVerticle(), options);
                    }
                }

        );

    }

    private static void deployClusteredVerticle() {
        // Cluster
        VertxOptions vertxOptions = new VertxOptions();

        Vertx.clusteredVertx(vertxOptions, results -> {
            if(results.succeeded()) {

                Vertx vertx = results.result();

                // Use config/config.json from resources/classpath to get DB connection details
                ConfigRetriever configRetriever = ConfigRetriever.create(vertx);

                configRetriever.getConfig( config -> {
                            if(config.succeeded()) {
                                JsonObject configJson = config.result();
                                //                      System.out.println(configJson.encodePrettily());

                                DeploymentOptions options = new DeploymentOptions().setConfig(configJson);
                                vertx.deployVerticle(new MongoVerticle(), options);
                            }
                        }

                );

            }
        });

    }


    @Override
    public void start() throws Exception {
        LOGGER.info("MongoVerticle started");

        Router router = Router.router(vertx);

        router.get("/mongo").handler(this::getAllProducts);


        /* Mongo DB connection */
        JsonObject dbConfig = new JsonObject();
        dbConfig.put("connection_string",config().getValue("db.connection_string"));
//        dbConfig.put("username",);
//        dbConfig.put("password",);
//        dbConfig.put("authSource",);
        dbConfig.put("useObjectId",config().getValue("db.useObjectId"));

        // Create Mongo Client that shares a pool between different client instances
        mongoClient = MongoClient.createShared(vertx, dbConfig);


        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getInteger("db.http.port"));

        // To communicate via Event Bus
        // Consumer listens to messages sent across to specified service
        vertx.eventBus().consumer("com.arushi.myservice", message -> {
            System.out.println("Received message : " + message.body());

            // reply to message received
            // reply() for Vert.x 3.x releases
            // replyAndRequest() for Vert.x 4.x releases
            message.replyAndRequest(
                    new JsonObject()
                        .put("responseCode","OK")
                        .put("message","This is the response to your event")
            );
        });

        // Code to test sending message to service
        vertx.setTimer(5000, handler -> {
            sendTestEvent();
        });
    }

    private void sendTestEvent() {
        // sending message to consumers of specified service - com.arushi.myservice

        JsonObject testInfo = new JsonObject();
        testInfo.put("info", "Hi");
        System.out.println("Sending message : " + testInfo);

        // eventBus().send() for Vert.x 3.x releases
        // eventBus().request() for Vert.x 4.x releases
        vertx.eventBus().request("com.arushi.myservice",
                                    testInfo.toString(),
                                    asyncResult -> {
                                            if(asyncResult.succeeded()) {
                                                JsonObject reply = (JsonObject) asyncResult.result().body();
                                                System.out.println("Reply message received - " + reply.toString());
                                            }


                                    }

                 )

        ;
    }

    private void getAllProducts(RoutingContext routingContext) {
        FindOptions findOptions = new FindOptions();
//        findOptions.setLimit(1);
        findOptions.setSort(new JsonObject().put("number",-1)); // descending sort for number field

        mongoClient.findWithOptions("products",
                new JsonObject(),
                findOptions,
                results -> {
                    JsonObject jsonResponse = new JsonObject();

                    try{
                        List<JsonObject> products = results.result();

                        if(products!=null && !products.isEmpty()) {
                            System.out.println(products.size() + " products");
                            jsonResponse.put("products",products);

                            routingContext.response()
                                    .putHeader("content-type","application/json")
                                    .setStatusCode(200)
                                    .end(Json.encodePrettily(jsonResponse));
                        } else {
                            jsonResponse.put("error", "No items found");

                            routingContext.response()
                                    .putHeader("content-type","application/json; charset=utf-8")
                                    .setStatusCode(400)
                                    .end(Json.encodePrettily(jsonResponse));
                        }

                    } catch (Exception e) {
                        LOGGER.error("getAllProducts failed. Exception e = " , e);

                        jsonResponse.put("error", "Exception & No items found");

                        routingContext.response()
                                .putHeader("content-type","application/json; charset=utf-8")
                                .setStatusCode(400)
                                .end(Json.encodePrettily(jsonResponse));
                    }

                  });


    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("MongoVerticle stopped");
        mongoClient.close();
        mongoClient = null;
    }
}

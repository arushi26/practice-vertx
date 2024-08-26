package com.arushi.practicevertx;

import com.arushi.practicevertx.database.MongoManager;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.mongo.MongoClient;


public class MongoVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoVerticle.class);
    public static MongoClient mongoClient = null;

    private final String myServiceName = "com.arushi.myservice";
    public static void main(String[] args) {

        // Code to deploy cluster -->
        deployClusteredVerticle();

        // Non-clustered -->
        //deployNonClusteredVerticle();

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

    private static void deployNonClusteredVerticle() {
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


    @Override
    public void start() throws Exception {
        LOGGER.info("MongoVerticle started");


        /* Mongo DB connection */
        JsonObject dbConfig = new JsonObject();
        dbConfig.put("connection_string", "mongodb://" + config().getString("mongodb.host")
                + ":" + config().getInteger("mongodb.port") + "/" + config().getString("mongodb.databasename") );
//        dbConfig.put("username", config().getString("mongodb.username"));
//        dbConfig.put("password", config().getString("mongodb.password"));
//        dbConfig.put("authSource", config().getString("mongodb.authSource"));
        dbConfig.put("useObjectId",config().getBoolean("mongodb.useObjectId"));

        // Create Mongo Client that shares a pool between different client instances
        mongoClient = MongoClient.createShared(vertx, dbConfig);
        MongoManager mongoManager = new MongoManager(mongoClient);

        mongoManager.registerConsumer(vertx);
        registerMyServiceConsumer();

        // Code to test sending message to service
        vertx.setTimer(5000, handler -> {
            sendTestEvent();
        });
    }

    private void registerMyServiceConsumer() {
        // To communicate via Event Bus
        // Consumer listens to messages sent across to specified service
        vertx.eventBus().consumer(myServiceName, message -> {
            System.out.println( myServiceName + "  :: Received message : " + message.body());

            // reply to message received
            // reply() for Vert.x 3.x releases
            // replyAndRequest() for Vert.x 4.x releases
            message.replyAndRequest(
                    new JsonObject()
                            .put("responseCode","OK")
                            .put("message","This is the response to your event")
            );
        });

    }

    private void sendTestEvent() {
        // sending message to consumers of specified service - com.arushi.myservice

        JsonObject testInfo = new JsonObject();
        testInfo.put("info", "Hi");
        System.out.println( myServiceName + "  :: Sending message : " + testInfo);

        // eventBus().send() for Vert.x 3.x releases
        // eventBus().request() for Vert.x 4.x releases
        vertx.eventBus().request(myServiceName,
                                    testInfo.toString(),
                                    asyncResult -> {
                                            if(asyncResult.succeeded()) {
                                                JsonObject reply = (JsonObject) asyncResult.result().body();
                                                System.out.println( myServiceName + "  :: Reply message received - " + reply.toString());
                                            }


                                    }

                 )

        ;
    }



    @Override
    public void stop() throws Exception {
        LOGGER.info("MongoVerticle stopped");
        mongoClient.close();
        mongoClient = null;
    }
}

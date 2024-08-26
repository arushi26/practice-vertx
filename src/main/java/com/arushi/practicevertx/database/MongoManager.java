package com.arushi.practicevertx.database;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class MongoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoManager.class);
    private MongoClient mongoClient = null;
    private static final String mongoServiceName = "com.arushi.mongoservice";

   public MongoManager(MongoClient mongoClient) {
       this.mongoClient = mongoClient;
    }

    public static String serviceName() {
       return mongoServiceName;
    }

    public void registerConsumer(Vertx vertx) {

       vertx.eventBus().consumer(mongoServiceName, message -> {
            System.out.println(mongoServiceName+ " :: Received message : " + message.body());

            JsonObject inputJson = new JsonObject(message.body().toString());

            if (inputJson.getString("cmd").equals("findAll")) {
               getAllProducts(message);
            } else if (inputJson.getString("cmd").equals("findById")) {
                getProductById(message, inputJson.getString("id"));
            } else if (inputJson.getString("cmd").equals("add")) {
                addProduct(message, inputJson.getJsonObject("product"));
            } else if (inputJson.getString("cmd").equals("update")) {
                updateProduct(message, inputJson.getString("id"), inputJson.getJsonObject("product"));
            } else if (inputJson.getString("cmd").equals("delete")) {
                deleteProduct(message, inputJson.getString("id"));
            }

        });

    }

    private void getAllProducts(Message<Object> message) {
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

                        } else {
                            jsonResponse.put("error", "No items found");
                        }

                    } catch (Exception e) {
                        LOGGER.error("getAllProducts failed. Exception e = " , e);

                        jsonResponse.put("error", "Exception & No items found");

                    }

                    message.replyAndRequest(jsonResponse.toString());

                });
    }


    private void getProductById(Message<Object> message, String id) {
       mongoClient.find("products",
                            new JsonObject().put("_id",id),
                            asyncResult -> {
                                JsonObject jsonResponse = null;

                                try {
                                    List<JsonObject> resultList = asyncResult.result();

                                    if (resultList != null && !resultList.isEmpty()) {
                                        System.out.println("Product = " + resultList);

                                        jsonResponse = resultList.get(0);

                                    } else {
                                        jsonResponse = new JsonObject().put("error", "No items found");
                                    }

                                } catch (Exception e) {
                                    LOGGER.error("getAllProducts failed. Exception e = ", e);

                                    jsonResponse = new JsonObject().put("error", "Exception & No items found");

                                }

                                message.replyAndRequest(jsonResponse.toString());

                            }
       );

    }


   private void addProduct(Message<Object> message, JsonObject product) {
       System.out.println("Add Product to Mongo request : " + product.toString());
       mongoClient.insert("products",
                                product,
                                asyncResult -> {
                                    if(asyncResult.succeeded()) {
                                        JsonObject jsonResponse = null;

                                        try {
                                            String itemId = asyncResult.result();

                                            if (!itemId.isEmpty() && !itemId.isBlank()) {
                                                System.out.println("Product added with Id = " + itemId);

                                                jsonResponse = new JsonObject().put("id", itemId);

                                            }
                                        else {
                                                jsonResponse = new JsonObject().put("error", "Item could not be added");
                                            }

                                        } catch (Exception e) {
                                            LOGGER.error("addProduct failed. Exception e = ", e);

                                            jsonResponse = new JsonObject().put("error", "Exception & Item not added");

                                        }

                                        message.replyAndRequest(jsonResponse.toString());
                                    }
                                });
   }

    private void updateProduct(Message<Object> message, String productId, JsonObject updatedProduct) {
        System.out.println("Update Product request to MongoDB : " + updatedProduct.toString());

        JsonObject queryObject = new JsonObject().put("_id", productId);
        JsonObject updateRequest = new JsonObject().put("$set",updatedProduct);

        mongoClient.updateCollection("products",
                                        queryObject,
                                        updateRequest,
                                        asyncResult -> {

                                            if(asyncResult.succeeded()) {

                                                JsonObject jsonResponse = new JsonObject();
                                                jsonResponse.put("updated", true);

                                                message.replyAndRequest(jsonResponse.toString());

                                            } else {
                                                LOGGER.error("updateProduct failed for ID " + productId + " : " + asyncResult.cause().toString());
                                            }
                                        }
                );

    }

    private void deleteProduct(Message<Object> message, String id) {
        System.out.println("Delete Product request to MongoDB for Id " + id);

        mongoClient.findOneAndDelete("products",
                                        new JsonObject().put("_id",id),
                                        asyncResult -> {
                                            JsonObject response = new JsonObject();

                                            if(asyncResult.succeeded()) {
                                                response.put("deleted", true);
                                                message.replyAndRequest(response.toString());

                                            } else {
                                                LOGGER.error("deleteProduct failed for ID " + id + " : " + asyncResult.cause().toString());
                                            }

                                        }
                );

    }


}

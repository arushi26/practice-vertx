package com.arushi.practicevertx;

import com.arushi.practicevertx.resources.ProductResources;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class APIVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIVerticle.class);
    public static void main(String[] args) {

        deployClusteredVerticle();
    }


    private static void deployClusteredVerticle() {
        // Cluster
        VertxOptions vertxOptions = new VertxOptions();

        Vertx.clusteredVertx(vertxOptions, results -> {
            if(results.succeeded()) {

                Vertx vertx = results.result();

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
        });

    }

    @Override
    public void start() throws Exception {
        LOGGER.info("APIVerticle started");

        Router router = Router.router(vertx);

        // Create instance of ProductResources
        ProductResources productResources = new ProductResources();

        // Map subrouter for Products
        router.route("/api/*").subRouter(productResources.getAPISubRouter(vertx));

        // Default if no routes matched
        router.route()
                        .handler(routingContext -> {
                            ClassLoader classLoader = getClass().getClassLoader();
                            File file = new File(classLoader.getResource("webroot/home.html").getFile());

                            // Get "name" cookie
                            Cookie nameCookie =  routingContext.request().getCookie("name");
                            String name = "Unknown";
                            if(nameCookie!=null) {
                                // Get value from "name" cookie
                                name = nameCookie.getValue();
                            } else {
                                // Set value for "name" cookie
                                nameCookie = Cookie.cookie("name","Arushi");
                                nameCookie.setPath("/");
                                nameCookie.setMaxAge(60); // 1 minute in seconds
                                routingContext.response().addCookie(nameCookie);
                            }

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
                                mappedHtml = replaceAllTokens(mappedHtml, "{name}", name);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            routingContext.response().putHeader("content-type","text/html").end(mappedHtml);
                        });
                //.handler(StaticHandler.create().setCachingEnabled(false));

        vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
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


    private String replaceAllTokens(String mappedHtml, String token, String replaceWith) {

        while(mappedHtml.contains(token)) {
            mappedHtml = mappedHtml.replace(token, replaceWith);
        }
        return  mappedHtml;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("APIVerticle stopped");
    }
}
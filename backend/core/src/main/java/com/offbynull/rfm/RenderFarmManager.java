/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.rfm;

import com.offbynull.actors.ActorSystem;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Render farm manager.
 * @author Kasra Faghihi
 */
public final class RenderFarmManager implements Closeable {
    
    private static final Logger LOG = LoggerFactory.getLogger(RenderFarmManager.class);
    
    private final ActorSystem actorSystem;
    private final Server jettyServer;
    
    /**
     * Create a new {@link RenderFarmManager} instance.
     * @param listenAddr http server listen address
     * @return new render farm manager
     * @throws IllegalArgumentException if any argument is {@code null}
     * @throws IllegalStateException if instance couldn't start
     */
    public static RenderFarmManager create(InetSocketAddress listenAddr) {
        Validate.notNull(listenAddr);

        ActorSystem actorSystem = ActorSystem.defaultBuilder().build();
        actorSystem.getActorGateway().addActor(
                "echoer",
                new EchoActor(),
                new Object());
        
        HttpServlet messageServlet = actorSystem.getServletGateway().getMessageServlet();
        HttpServlet addressServlet = actorSystem.getServletGateway().getAddressServlet();
        Server jettyServer = null;
        try {
            QueuedThreadPool threadPool = new QueuedThreadPool(100, 10, 30000); // max:100threads, min:10threads, kill threads after 30s
            jettyServer = new Server(threadPool);
            
            ServerConnector connector = new ServerConnector(jettyServer);
            connector.setPort(listenAddr.getPort());
            connector.setHost(listenAddr.getHostString());
            jettyServer.setConnectors(new Connector[] {connector});
  

            // https://stackoverflow.com/a/28192729/1196226
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");

            ServletHolder messageServletHolder = new ServletHolder(messageServlet);
            context.addServlet(messageServletHolder, "/rfm/*");
            
            ServletHolder addressServletHolder = new ServletHolder(addressServlet);
            context.addServlet(addressServletHolder, "/address/*");

            FilterHolder cors = context.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
            cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
            cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
            cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] {context, new DefaultHandler() });


            jettyServer.setHandler(handlers);
            jettyServer.setStopAtShutdown(true);
            jettyServer.start();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
            throw new IllegalStateException(e);
        }
        
        return new RenderFarmManager(jettyServer, actorSystem);
    }
    
    private RenderFarmManager(Server jettyServer, ActorSystem actorSystem) {
        Validate.notNull(jettyServer);
        Validate.notNull(actorSystem);
        this.jettyServer = jettyServer;
        this.actorSystem = actorSystem;
    }

    @Override
    public void close() throws IOException {
        try {
            jettyServer.stop();
            jettyServer.join();
        } catch (Exception e) {
            LOG.error("Stopping Jetty failed", e);
        }

        try {
            actorSystem.close();
            actorSystem.join();
        } catch (RuntimeException | InterruptedException e) {
            LOG.error("Stopping ActorSystem failed", e);
        }
    }
}

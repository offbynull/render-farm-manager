/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
import javax.servlet.http.HttpServlet;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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
        
        HttpServlet servlet = actorSystem.getServletGateway().getServlet();
        Server jettyServer = null;
        try {
            QueuedThreadPool threadPool = new QueuedThreadPool(100, 10, 30000); // max:100threads, min:10threads, kill thread after 30s
            jettyServer = new Server(threadPool);
            
            ServerConnector connector = new ServerConnector(jettyServer);
            connector.setPort(listenAddr.getPort());
            connector.setHost(listenAddr.getHostString());
            jettyServer.setConnectors(new Connector[] {connector});
  
            
            ServletHandler servletHandler = new ServletHandler();
            
            ServletHolder servletHolder = new ServletHolder(servlet);
            servletHolder.setAsyncSupported(true);
            servletHandler.addServletWithMapping(servletHolder, "/rfm");
            
//            DoSFilter dosFilter = new DoSFilter();
//            FilterHolder filterHolder = new FilterHolder(dosFilter);
//            servletHandler.addFilterWithMapping(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] {servletHandler, new DefaultHandler()});
            
            
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

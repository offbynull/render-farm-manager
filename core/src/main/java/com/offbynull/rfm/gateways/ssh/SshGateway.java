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
package com.offbynull.rfm.gateways.ssh;

import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttles.simple.Bus;
import com.offbynull.actors.shuttles.simple.SimpleShuttle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * {@link Gateway} that runs commands on a SSH-enabled Linux host.
 * <p>
 * To run a command on a host, send a {@link SshRequestMessage} to this gateway. This gateway will eventually respond with a
 * {@link SshResponseMessage} on successful execution or a {@link SshErrorMessage} on failed execution.
 * @author Kasra Faghihi
 */
public final class SshGateway implements Gateway {

    private final ExecutorService workerPool;
    private final Bus bus;
    
    private final ConcurrentHashMap<String, Shuttle> outShuttles;    
    private final SimpleShuttle shuttle;

    /**
     * Create a {@link SshGateway} instance.
     * @param prefix address prefix for this gateway
     * @param max maximum number of SSH commands that can be run concurrently
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code max <= 0}
     * @return new SSH gateway
     */
    public static SshGateway create(String prefix, int max) {
        Validate.notNull(prefix);
        Validate.isTrue(max > 0);

        SshGateway gateway = new SshGateway(prefix, max);
        return gateway;
    }
    
    private SshGateway(String prefix, int workerCount) {
        Validate.notNull(prefix);
        Validate.isTrue(workerCount > 0);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        outShuttles = new ConcurrentHashMap<>();
        workerPool = Executors.newFixedThreadPool(
                workerCount,
                new BasicThreadFactory.Builder()
                        .namingPattern(prefix + " " + SshGateway.class.getSimpleName() + " worker %d")
                        .daemon(true)
                        .build()
        );
    }

    @Override
    public Shuttle getIncomingShuttle() {
        if (workerPool.isShutdown()) {
            throw new IllegalStateException();
        }

        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        if (workerPool.isShutdown()) {
            throw new IllegalStateException();
        }

        String shuttlePrefix = shuttle.getPrefix();
        outShuttles.put(shuttlePrefix, shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (workerPool.isShutdown()) {
            throw new IllegalStateException();
        }
        
        outShuttles.remove(shuttlePrefix);
    }

    @Override
    public void join() throws InterruptedException {
        workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @Override
    public void close() {
        workerPool.shutdownNow();
    }
}

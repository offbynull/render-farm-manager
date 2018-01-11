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
package com.offbynull.rfm.gateways.select;

import com.offbynull.rfm.gateways.ssh.*;
import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.gateways.threadpool.ThreadPoolGateway;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Map;
import com.offbynull.actors.gateways.threadpool.ThreadPoolProcessor;
import java.util.HashMap;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that selects/updates/deletes hosts and jobs.
 * <p>
 * FILL THIS IN {@code 
 * @author Kasra Faghihi
 */
public final class SelectGateway implements Gateway {
    private final ThreadPoolGateway backingGateway;

    public static ThreadPoolGateway create(String prefix, Map<Class<?>, ThreadPoolProcessor> payloadTypes, int threads) {
        return ThreadPoolGateway.create(prefix, payloadTypes, threads);
    }

    public static ThreadPoolGateway create(String prefix, Map<Class<?>, ThreadPoolProcessor> payloadTypes, int minThreads, int maxThreads) {
        return ThreadPoolGateway.create(prefix, payloadTypes, minThreads, maxThreads);
    }

    public SelectGateway(String prefix, int minThreads, int maxThreads) {
        Validate.notNull(prefix);
        Validate.isTrue(minThreads >= 0);
        Validate.isTrue(maxThreads >= 0);

        Map<Class<?>, ThreadPoolProcessor> payloadTypes = new HashMap<>();
        payloadTypes.put(SshRequestMessage.class, new SshProcessor());

        this.backingGateway = ThreadPoolGateway.create(prefix, payloadTypes, minThreads, maxThreads);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return backingGateway.getIncomingShuttle();
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        backingGateway.addOutgoingShuttle(shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        backingGateway.removeOutgoingShuttle(shuttlePrefix);
    }

    @Override
    public void join() throws InterruptedException {
        backingGateway.join();
    }

    @Override
    public void close() {
        backingGateway.close();
    }
    
}

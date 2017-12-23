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

import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.Validate;

final class SshShuttle implements Shuttle {
    private final String prefix;
    private final ExecutorService workerPool;
    private final ConcurrentHashMap<String, Shuttle> outShuttles;

    SshShuttle(String prefix, ConcurrentHashMap<String, Shuttle> outShuttles, ExecutorService workerPool) {
        Validate.notNull(prefix);
        Validate.notNull(outShuttles); // outShuttles is concurrent map -- entries added/removed on the fly, don't nullcheck keys or values
        Validate.notNull(workerPool);
        this.prefix = prefix;
        this.outShuttles = outShuttles;
        this.workerPool = workerPool;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        if (workerPool.isShutdown()) {
            return;
        }

        messages.stream()
                .filter(m -> m.getDestinationAddress().size() <= 2)
                .filter(m -> m.getDestinationAddress().getElement(0).equals(prefix))
                .filter(m -> m.getMessage() instanceof SshRequestMessage)
                .forEach(m -> workerPool.submit(new SshCallable(m, outShuttles)));
    }
    
}

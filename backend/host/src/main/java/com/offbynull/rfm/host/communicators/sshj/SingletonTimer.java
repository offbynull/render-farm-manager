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
package com.offbynull.rfm.host.communicators.sshj;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SingletonTimer {
    
    private static final Logger logger = LoggerFactory.getLogger(SingletonTimer.class);
    
    private final Object lock = new Object();

    private int counter;
    private ScheduledExecutorService value;
    
    public void addReference() {
        synchronized (lock) {
            logger.debug("Add reference");
            if (counter == 0) {
                logger.debug("Counter is 0 -- starting up thread");
                ThreadFactory threadFactory = new BasicThreadFactory.Builder()
                        .daemon(true)
                        .namingPattern("SSHJ Singleton Timeout Timer")
                        .build();
                value = Executors.newScheduledThreadPool(1, threadFactory);
            }
            counter++;
        }
    }
    
    public void removeReference() {
        synchronized (lock) {
            logger.debug("Remove reference");
            if (counter == 0) {
                throw new IllegalStateException();
            }

            counter--;
            if (counter == 0) {
                logger.debug("Counter reached 0 -- shutting down thread");
                value.shutdownNow();
                value = null;
            }
        }
    }

    public void schedule(Runnable runnable, long delay, TimeUnit unit) {
        Validate.notNull(runnable);
        Validate.notNull(unit);
        Validate.isTrue(delay >= 0L);
        synchronized (lock) {
            logger.debug("Schedule timeout after {} {}", delay, unit);
            if (counter == 0) {
                throw new IllegalStateException();
            }

            value.schedule(runnable, delay, unit);
        }
    }

    public int getReferenceCount() {
        synchronized (lock) {
            return counter;
        }
    }
}

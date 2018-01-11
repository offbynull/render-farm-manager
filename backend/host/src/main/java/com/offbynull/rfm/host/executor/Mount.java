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
package com.offbynull.rfm.host.executor;

import org.apache.commons.lang3.Validate;

/**
 * Mount information.
 * @author Kasra Faghihi
 */
public final class Mount {
    private final String target;
    private final long used;
    private final long available;

    /**
     * Constructs a {@link Mount} object.
     * @param target target directory
     * @param used bytes used
     * @param available bytes available
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code used >= 0L},
     * {@code available >= 0L}
     */
    public Mount(String target, long used, long available) {
        Validate.notNull(target);
        Validate.isTrue(used >= 0L);
        Validate.isTrue(available >= 0L);
        this.target = target;
        this.used = used;
        this.available = available;
    }

    /**
     * Get target directory.
     * @return target directory
     */
    public String getTarget() {
        return target;
    }

    /**
     * Get bytes used.
     * @return bytes used
     */
    public long getUsed() {
        return used;
    }

    /**
     * Get bytes available.
     * @return bytes available
     */
    public long getAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return "Mount{" + "target=" + target + ", used=" + used + ", available=" + available + '}';
    }
    
}

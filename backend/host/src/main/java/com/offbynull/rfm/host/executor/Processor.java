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

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

/**
 * Processor (CPU) information.
 * @author Kasra Faghihi
 */
public final class Processor {

    private final long physicalId;
    private final long coreId;
    private final long processor;
    private final String model;
    private final UnmodifiableSet<String> flags;
    private final double usage;

    /**
     * Construct a {@link Processor} instance.
     * @param physicalId physical/socket ID
     * @param coreId core ID
     * @param processorId processor ID
     * @param model model name
     * @param flags processor flags
     * @param usage processor usage (over 1 second window)
     * @throws NullPointerException if nay argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code physicalId >= 0L},
     * {@code coreId >= 0L},
     * {@code processor >= 0L},
     * {@code usage >= 0.0 && usage <= 1.0},
     * {@code !flags.contains(null)}
     */
    public Processor(long physicalId, long coreId, long processorId, String model, Set<String> flags, double usage) {
        Validate.notNull(model);
        Validate.notNull(flags);
        Validate.isTrue(physicalId >= 0L);
        Validate.isTrue(coreId >= 0L);
        Validate.isTrue(processorId >= 0L);
        Validate.isTrue(usage >= 0.0 && usage <= 1.0);
        Validate.noNullElements(flags);
        this.physicalId = physicalId;
        this.coreId = coreId;
        this.processor = processorId;
        this.model = model;
        this.flags = (UnmodifiableSet<String>) unmodifiableSet(new HashSet<>(flags));
        this.usage = usage;
    }

    /**
     * Get physical/socket ID.
     * @return physical/socket ID
     */
    public long getPhysicalId() {
        return physicalId;
    }

    /**
     * Get core ID.
     * @return core ID
     */
    public long getCoreId() {
        return coreId;
    }

    /**
     * Get processor ID.
     * @return processor ID
     */
    public long getProcessor() {
        return processor;
    }

    /**
     * Get model name.
     * @return model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Get processor flags.
     * @return processor flags
     */
    public UnmodifiableSet<String> getFlags() {
        return flags;
    }

    /**
     * Get processor usage.
     * @return processor usage (over 1 second window)
     */
    public double getUsage() {
        return usage;
    }

    @Override
    public String toString() {
        return "Processor{" + "physicalId=" + physicalId + ", coreId=" + coreId + ", processor=" + processor
                + ", model=" + model + ", flags=" + flags + ", usage=" + usage + '}';
    }
    
}

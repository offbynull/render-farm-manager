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
 * Task resource allocation.
 * @author Kasra Faghihi
 */
public final class TaskResourceAllocation {
    
    public static final long CFS_PERIOD = 100000L;
    
    private final UnmodifiableSet<Long> cpuAffinity;
    private final long schedulerSlice;
    private final long memoryLimit;
    private final long diskLimit;

    /**
     * Constructs a {@link ResourceAllocation} instance.
     * @param cpuAffinity CPUs to lock task to
     * @param schedulerSlice portion of processing time to devote from the CPUs listed in {@code cpuAffinity} ({@link #CFS_PERIOD} = 1 whole
     * CPU worth of time)
     * @param memoryLimit maximum memory usage allowed (RSS+cache)
     * @param diskLimit maximum disk usage allowed
     * @throws NullPointerException if any argument is {@code null}
     * @throws ArithmeticException if any of the following expressions cause an overflow:
     * {@code cpuAffinity.size() * CFS_PERIOD}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code cpuAffinity.stream().allMatch(x -> x >= 0)},
     * {@code !cpuAffinity.isEmpty()},
     * {@code !cpuAffinity.contains(null)},
     * {@code schedulerSlice >= 0L},
     * {@code schedulerSlice <= cpuAffinity.size() * CFS_PERIOD},
     * {@code memoryLimit >= 0L},
     * {@code diskLimit >= 0L}
     */
    public TaskResourceAllocation(Set<Long> cpuAffinity, long schedulerSlice, long memoryLimit, long diskLimit) {
        Validate.notNull(cpuAffinity);
        Validate.isTrue(cpuAffinity.stream().allMatch(x -> x >= 0));
        Validate.isTrue(!cpuAffinity.isEmpty());
        Validate.noNullElements(cpuAffinity);
        Validate.isTrue(schedulerSlice > 0L);
        long maxSchedulerSlice = Math.multiplyExact(cpuAffinity.size(), CFS_PERIOD); // arithmetic exception if overflow
        Validate.isTrue(schedulerSlice <= maxSchedulerSlice);
        Validate.isTrue(memoryLimit >= 0L);
        Validate.isTrue(diskLimit >= 0L);

        this.cpuAffinity = (UnmodifiableSet<Long>) unmodifiableSet(new HashSet<>(cpuAffinity));
        this.schedulerSlice = schedulerSlice;
        this.memoryLimit = memoryLimit;
        this.diskLimit = diskLimit;
    }

    /**
     * Get CPUs.
     * @return CPUs
     */
    public UnmodifiableSet<Long> getCpuAffinity() {
        return cpuAffinity;
    }

    /**
     * Get scheduler slice.
     * @return scheduler slice
     */
    public long getSchedulerSlice() {
        return schedulerSlice;
    }

    /**
     * Get memory limit.
     * @return memory limit
     */
    public long getMemoryLimit() {
        return memoryLimit;
    }

    /**
     * Get disk limit.
     * @return disk limit
     */
    public long getDiskLimit() {
        return diskLimit;
    }

    @Override
    public String toString() {
        return "TaskResourceAllocation{" + "cpuAffinity=" + cpuAffinity + ", schedulerSlice=" + schedulerSlice
                + ", memoryLimit=" + memoryLimit + ", diskLimit=" + diskLimit + '}';
    }
    
}

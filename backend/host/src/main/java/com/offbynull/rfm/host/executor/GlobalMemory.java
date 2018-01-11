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
 * Memory information.
 * @author Kasra Faghihi
 */
public final class GlobalMemory {
    // Is it worth tracking page cache? Possibly.
    //   cache = inmemory cache of file content  (page cache)
    //   buffer = inmemory cache of file metadata
    // Linux kernel 2.4+ combine the buffer into the page cache, so the cache entry will include both. The cache will automatically shrink
    // as your application uses more memory. All this means is that IO may be slower because the cache gets shrunk.
    //
    // See https://serverfault.com/a/903435/461008...
    // 2. Cgroup's memory.stat file gives you the page cache for the group. If the same file is opened by processes in different cgroups,
    //    the cgroup that touched the page first is the the cgroup that will have it's cache field incremented.
    // 3. Cgroup's memory.stat file won't give you a swap field unless the kernel was compiled with CONFIG_MEMCG_SWAP_ENABLED option. 

        private final long memTotal;
        private final long memFree;
        private final long swapTotal;
        private final long swapFree;

    /**
     * Constructs a {@link Memory} object.
     * @param memTotal 
     * @param memFree  
     * @param swapTotal  
     * @param swapFree 
     * @throws IllegalArgumentException if any of the following expressions are NOT met:
     * {@code memTotal >= 0L},
     * {@code memFree >= 0L},
     * {@code swapTotal >= 0L},
     * {@code swapFree >= 0L}
     */
    public GlobalMemory(long memTotal, long memFree, long swapTotal, long swapFree) {
        Validate.isTrue(memTotal >= 0L);
        Validate.isTrue(memFree >= 0L);
        Validate.isTrue(swapTotal >= 0L);
        Validate.isTrue(swapFree >= 0L);
        this.memTotal = memTotal;
        this.memFree = memFree;
        this.swapTotal = swapTotal;
        this.swapFree = swapFree;
    }

    /**
     * Get total memory.
     * @return total memory in bytes
     */
    public long getMemTotal() {
        return memTotal;
    }

    /**
     * Get free memory.
     * @return free memory in bytes
     */
    public long getMemFree() {
        return memFree;
    }

    /**
     * Get total swap.
     * @return total swap in bytes
     */
    public long getSwapTotal() {
        return swapTotal;
    }

    /**
     * Get free swap.
     * @return free swap in bytes
     */
    public long getSwapFree() {
        return swapFree;
    }

    @Override
    public String toString() {
        return "Memory{" + "memTotal=" + memTotal + ", memAvailable=" + memFree + ", swapTotal=" + swapTotal
                + ", swapFree=" + swapFree + '}';
    }


}

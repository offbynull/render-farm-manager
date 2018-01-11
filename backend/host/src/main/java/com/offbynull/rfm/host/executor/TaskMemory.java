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
public final class TaskMemory {
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

    private final long rss;
    private final long cache;
    private final long swap;

    /**
     * Constructs a {@link Memory} object.
     * @param rss resident-set size (RSS) in bytes
     * @param cache cache in bytes
     * @param swap swap in bytes
     */
    public TaskMemory(long rss, long cache, long swap) {
        Validate.isTrue(rss >= 0L);
        Validate.isTrue(cache >= 0L);
        Validate.isTrue(swap >= 0L);
        this.rss = rss;
        this.cache = cache;
        this.swap = swap;
    }

    /**
     * Get resident-set size (RSS).
     * @return resident-set size (RSS) in bytes
     */
    public long getRss() {
        return rss;
    }

    /**
     * Get cache.
     * @return cache in bytes
     */
    public long getCache() {
        return cache;
    }

    /**
     * Get swap.
     * @return swap in bytes
     */
    public long getSwap() {
        return swap;
    }

    @Override
    public String toString() {
        return "Memory{" + "rss=" + rss + ", cache=" + cache + ", swap=" + swap + '}';
    }

}

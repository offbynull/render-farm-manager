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
package com.offbynull.rfm.host.executors.communicator;

import static com.offbynull.rfm.host.executors.communicator.InternalUtils.toLong;
import org.apache.commons.lang3.Validate;

// cat /sys/fs/cgroup/memory/memory.stat
//   total_cache+total_rss+total_swap = cat /sys/fs/cgroup/memory/memory.usage_in_bytes
final class CGroupMemoryStatParser {
    
    private CGroupMemoryStatParser() {
        // do nothing
    }

    public static CGroupMemoryStat parse(String str) {
        Validate.notNull(str);
        
        Long totalCache = null;
        Long totalRss = null;
        Long totalSwap = null;
        
        String[] lines = str.split("\\n+");
        for (String line : lines) {
            String[] words = line.split("\\s+");
            if (words.length < 2) {
                continue;
            }

            switch (words[0]) {
                case "total_cache":
                    totalCache = toLong(words[1]);
                    break;
                case "total_rss":
                    totalRss = toLong(words[1]);
                    break;
                case "total_swap":
                    totalSwap = toLong(words[1]);
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        Validate.isTrue(totalRss != null);
        Validate.isTrue(totalCache != null);
        return new CGroupMemoryStat(totalRss, totalCache, totalSwap);
    }

    static final class CGroupMemoryStat {
        private final long rss;
        private final long cache;
        private final Long swap; // if system configured without CONFIG_MEMCG_SWAP_ENABLED, swap features of memory cgroup will be disabled

        public CGroupMemoryStat(long rss, long cache, Long swap) {
            this.rss = rss;
            this.cache = cache;
            this.swap = swap;
        }

        public long getRss() {
            return rss;
        }

        public long getCache() {
            return cache;
        }

        public Long getSwap() {
            return swap;
        }
    }
}

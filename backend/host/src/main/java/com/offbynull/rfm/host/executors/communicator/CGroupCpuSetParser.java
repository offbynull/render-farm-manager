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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.Validate;

// cat /sys/fs/cgroup/cpuset/cpuset.cpus
final class CGroupCpuSetParser {
    
    private CGroupCpuSetParser() {
        // do nothing
    }

    public static Set<Long> parse(String str) {
        Validate.notNull(str);
        
        Set<Long> ret = new HashSet<>();
        
        String[] ranges = str.trim().split(",");
        for (String range : ranges) {
            String[] rangeArr = range.split("-", 2);
            
            switch (rangeArr.length) {
                case 1:
                    long val = toLong(rangeArr[0]);
                    ret.add(val);
                    break;
                case 2:
                    long start = toLong(rangeArr[0]);
                    long end = toLong(rangeArr[1]);
                    Validate.isTrue(start >= 0L);
                    Validate.isTrue(end >= 0L);
                    Validate.isTrue(start <= end);
                    Validate.isTrue(end - start < 2048L); // sanity check -- more than 2048 processors in a machine? likely not.
                    for (long i = start; i <= end; i++) {
                        ret.add(i);
                    }
                    break;
                default:
                    throw new IllegalStateException(); // should never happen
            }
        }
        
        return ret;
    }
}

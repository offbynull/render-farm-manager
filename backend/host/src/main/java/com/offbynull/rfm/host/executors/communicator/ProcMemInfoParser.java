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

final class ProcMemInfoParser {
    
    private ProcMemInfoParser() {
        // do nothing
    }

    public static ProcMemInfo parse(String str) {
        Validate.notNull(str);
        
        Long memTotal = null;
        Long memAvailable = null;
        Long swapTotal = null;
        Long swapFree = null;
        
        String[] lines = str.split("\\n+");
        for (String line : lines) {
            String[] words = line.split("[:\\s]+");
            if (words.length < 2) {
                continue;
            }

            switch (words[0]) {
                case "MemTotal":
                    memTotal = toLong(words[1]);
                    break;
                case "MemAvailable":
                    memAvailable = toLong(words[1]);
                    break;
                case "SwapTotal":
                    swapTotal = toLong(words[1]);
                    break;
                case "SwapFree":
                    swapFree = toLong(words[1]);
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        Validate.isTrue(memTotal != null);
        Validate.isTrue(memAvailable != null);
        Validate.isTrue(swapTotal != null);
        Validate.isTrue(swapFree != null);
        return new ProcMemInfo(memTotal, memAvailable, swapTotal, swapFree);
    }

    static final class ProcMemInfo {
        private final long memTotal;
        private final long memAvailable;
        private final long swapTotal;
        private final long swapFree;

        public ProcMemInfo(long memTotal, long memAvailable, long swapTotal, long swapFree) {
            this.memTotal = memTotal;
            this.memAvailable = memAvailable;
            this.swapTotal = swapTotal;
            this.swapFree = swapFree;
        }

        public long getMemTotal() {
            return memTotal;
        }

        public long getMemAvailable() {
            return memAvailable;
        }

        public long getSwapTotal() {
            return swapTotal;
        }

        public long getSwapFree() {
            return swapFree;
        }

    }
}

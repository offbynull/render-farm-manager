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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class DfParser {
    private DfParser() {
        // do nothing
    }

    public static List<DfEntry> parse(String str) {
        Validate.notNull(str);
        
        List<DfEntry> ret = new ArrayList<>();
        
        String[] lines = str.split("\\n+");
        
        // start at 1, 0 is the header
        for (int i = 1; i < lines.length; i++) {
            String[] fields = lines[i].trim().split("\\s+", 4);
            
            Long used = toLong(fields[0]);
            Long available = toLong(fields[1]);
            String type = fields[2];
            String target = fields[3];
            
            Validate.isTrue(target != null);
            DfEntry dfEntry = new DfEntry(used, available, type, target);

            ret.add(dfEntry);
        }
        
        return ret;
    }

    static final class DfEntry {
        private final Long used;
        private final Long available;
        private final String type;
        private final String target;

        public DfEntry(Long used, Long available, String type, String target) {
            Validate.notNull(target);
            this.used = used;
            this.available = available;
            this.type = type;
            this.target = target;
        }

        public Long getUsed() {
            return used;
        }

        public Long getAvailable() {
            return available;
        }

        public String getType() {
            return type;
        }

        public String getTarget() {
            return target;
        }

    }
}

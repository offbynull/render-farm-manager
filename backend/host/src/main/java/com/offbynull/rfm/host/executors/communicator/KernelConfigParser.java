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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class KernelConfigParser {
    private KernelConfigParser() {
        // do nothing
    }

    public static List<KernelConfigEntry> parse(String str) {
        Validate.notNull(str);
        
        List<KernelConfigEntry> ret = new ArrayList<>();
        
        String[] lines = str.split("\\n+");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            int commentIdx = line.indexOf('#');
            if (commentIdx != -1) {
                line = line.substring(0, commentIdx);
            }
            
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split("=", 2);
            if (fields.length < 2) {
                continue;
            }

            KernelConfigEntry kcEntry = new KernelConfigEntry(fields[0], fields[1]);
            ret.add(kcEntry);
        }
        
        return ret;
    }

    static final class KernelConfigEntry {
        private final String key;
        private final String value;

        public KernelConfigEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

    }
}

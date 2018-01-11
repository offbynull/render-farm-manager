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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

final class ProcCpuInfoParser {

    private ProcCpuInfoParser() {
        // do nothing
    }

    public static List<ProcCpuInfoEntry> parse(String str) {
        Validate.notNull(str);
        
        List<ProcCpuInfoEntry> ret = new ArrayList<>();
        
        String[] cpuInfoBlocks = str.split("\\n{2,}");
        for (String cpuInfoBlock : cpuInfoBlocks) {
            Long physicalId = null;
            Long coreId = null;
            Long processor = null;
            String model = null;
            Set<String> flags = null;

            try (BufferedReader br = new BufferedReader(new StringReader(cpuInfoBlock))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] words = line.split(":", 2);
                    if (words.length < 2) {
                        continue;
                    }

                    String key = words[0].trim();
                    String val = words[1].trim();
                    
                    switch (key) {
                        case "physical id":
                            physicalId = toLong(val);
                            break;
                        case "core id":
                            coreId = toLong(val);
                            break;
                        case "processor":
                            processor = toLong(val);
                            break;
                        case "model name":
                            model = val;
                            break;
                        case "flags":
                            flags = new HashSet<>(Arrays.asList(val.split("\\s+")));
                            break;
                        default:
                            // do nothing
                            break;
                    }
                }
            } catch (IOException ioe) {
                // should never happen
                throw new IllegalStateException(ioe);
            }
            
            Validate.isTrue(physicalId != null);
            Validate.isTrue(coreId != null);
            Validate.isTrue(processor != null);
            ProcCpuInfoEntry procCpuInfo = new ProcCpuInfoEntry(physicalId, coreId, processor, model, flags);
            ret.add(procCpuInfo);
        }
        
        return ret;
    }

    static final class ProcCpuInfoEntry {
        private final long physicalId;
        private final long coreId;
        private final long processor;
        private final String model;
        private final UnmodifiableSet<String> flags;

        public ProcCpuInfoEntry(long physicalId, long coreId, long processor, String model, Set<String> flags) {
            Validate.isTrue(physicalId >= 0);
            Validate.isTrue(coreId >= 0);
            Validate.isTrue(processor >= 0);
            Validate.notNull(model);
            Validate.notNull(flags);
            Validate.noNullElements(flags);
            this.physicalId = physicalId;
            this.coreId = coreId;
            this.processor = processor;
            this.model = model;
            this.flags = (UnmodifiableSet<String>) unmodifiableSet(new HashSet<>(flags));
        }

        public long getPhysicalId() {
            return physicalId;
        }

        public long getCoreId() {
            return coreId;
        }

        public long getProcessor() {
            return processor;
        }

        public String getModel() {
            return model;
        }

        public UnmodifiableSet<String> getFlags() {
            return flags;
        }
    }
}

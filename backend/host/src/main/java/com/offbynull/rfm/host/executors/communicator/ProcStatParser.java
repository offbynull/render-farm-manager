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
import static com.offbynull.rfm.host.executors.communicator.InternalUtils.toLong;

final class ProcStatParser {

    private ProcStatParser() {
        // do nothing
    }

    public static List<ProcStatCpuEntry> parse(String str) {
        Validate.notNull(str);
        
        List<ProcStatCpuEntry> ret = new ArrayList<>();
        
        String[] procStatLines = str.split("\\n+");
        for (String line : procStatLines) {
            if (!line.matches("^cpu\\d+\\s+.*")) {
                continue;
            }

            String[] fields = line.split("\\s+");
            Validate.isTrue(fields.length >= 5);
            Long processor = toLong(fields[0].substring("cpu".length()));
            Long userTime = toLong(fields[1]);
            Long niceTime = toLong(fields[2]);
            Long systemTime = toLong(fields[3]);
            Long idleTime = toLong(fields[4]);

            Validate.isTrue(processor != null);
            Validate.isTrue(userTime != null);
            Validate.isTrue(niceTime != null);
            Validate.isTrue(systemTime != null);
            Validate.isTrue(idleTime != null);
            ProcStatCpuEntry procStatCpu = new ProcStatCpuEntry(processor, userTime, niceTime, systemTime, idleTime);
            ret.add(procStatCpu);
        }
        
        return ret;
    }

    // http://man7.org/linux/man-pages/man5/proc.5.html (search for /proc/stat section)
    // https://stackoverflow.com/a/9027251
    // https://unix.stackexchange.com/questions/361245/what-does-an-idle-cpu-process-do
    static final class ProcStatCpuEntry {
        private final long processor;
        private final long userTime;
        private final long niceTime;
        private final long systemTime;
        private final long idleTime;

        public ProcStatCpuEntry(long processor, long userTime, long niceTime, long systemTime, long idleTime) {
            Validate.isTrue(processor >= 0L);
            Validate.isTrue(userTime >= 0L);
            Validate.isTrue(niceTime >= 0L);
            Validate.isTrue(systemTime >= 0L);
            Validate.isTrue(idleTime >= 0L);
            this.processor = processor;
            this.userTime = userTime;
            this.niceTime = niceTime;
            this.systemTime = systemTime;
            this.idleTime = idleTime;
        }

        public long getProcessor() {
            return processor;
        }

        public long getUserTime() {
            return userTime;
        }

        public long getNiceTime() {
            return niceTime;
        }

        public long getSystemTime() {
            return systemTime;
        }

        public long getIdleTime() {
            return idleTime;
        }

    }
}

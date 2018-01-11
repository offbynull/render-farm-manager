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

final class TrackParser {
    
    private TrackParser() {
        // do nothing
    }

    public static List<TrackEntry> parse(String str) {
        List<TrackEntry> jobs = new ArrayList<>();
        
        if (str.isEmpty()) {
            return jobs;
        }
        
        String[] splitStr = str.split("\n+");
        for (String line : splitStr) {
            String[] splitLine = line.split("\\s", 3);

            String id = splitLine[0];
            long bootTime = Long.parseLong(splitLine[1]);
            String directory = splitLine[2];

            jobs.add(new TrackEntry(id, bootTime, directory));
        }

        return jobs;
    }

    static final class TrackEntry {
        private final String id;
        private final long bootTime;
        private final String directory;

        public TrackEntry(String id, long bootTime, String directory) {
            Validate.notNull(id);
            Validate.notNull(directory);
            Validate.notBlank(id);
            Validate.isTrue(bootTime >= 0L);
            Validate.notBlank(directory);

            this.id = id;
            this.bootTime = bootTime;
            this.directory = directory;
        }

        public String getId() {
            return id;
        }

        public long getBootTime() {
            return bootTime;
        }

        public String getDirectory() {
            return directory;
        }
    }
}

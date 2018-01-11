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
package com.offbynull.rfm.host.service;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Work to worker binding.
 * @author Kasra Faghihi
 */
public final class Bind {
    private final String workId;
    private final String workerHost;
    private final int workerPort;

    public Bind(String workId, String workerHost, int workerPort) {
        Validate.notNull(workId);
        Validate.notNull(workerHost);
        Validate.notEmpty(workId);
        Validate.notEmpty(workerHost);
        Validate.isTrue(workerPort >= 1 && workerPort <= 65535);
        this.workId = workId;
        this.workerHost = workerHost;
        this.workerPort = workerPort;
    }

    public String getWorkId() {
        return workId;
    }

    public String getWorkerHost() {
        return workerHost;
    }

    public int getWorkerPort() {
        return workerPort;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.workId);
        hash = 41 * hash + Objects.hashCode(this.workerHost);
        hash = 41 * hash + this.workerPort;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Bind other = (Bind) obj;
        if (this.workerPort != other.workerPort) {
            return false;
        }
        if (!Objects.equals(this.workId, other.workId)) {
            return false;
        }
        if (!Objects.equals(this.workerHost, other.workerHost)) {
            return false;
        }
        return true;
    }
    
}

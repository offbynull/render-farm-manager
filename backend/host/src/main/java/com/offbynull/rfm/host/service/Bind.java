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

import com.offbynull.rfm.host.model.partition.Partition;
import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;

/**
 * Work to worker binding.
 * @author Kasra Faghihi
 */
public final class Bind {
    private final String workId;
    private final String workerHost;
    private final int workerPort;
    private final Partition partition;

    public Bind(String workId, Partition partition) {
        Validate.notNull(workId);
        Validate.notNull(partition);
        Validate.notEmpty(workId);
        
        String host = (String) partition.getSpecificationId().get("s_host");
        BigDecimal portBd = (BigDecimal) partition.getSpecificationId().get("n_port");
        Validate.validState(host != null); // should never trigger
        Validate.validState(portBd != null); // should never trigger
        
        this.workId = workId;
        this.workerHost = host;
        Validate.validState(!workerHost.isEmpty()); // should never trigger
        try {
            this.workerPort = portBd.intValueExact();
        } catch (ArithmeticException ae) {
            throw new IllegalStateException(ae); // should never happen
        }
        Validate.validState(workerPort >= 1 && workerPort <= 65535); // should never trigger
        this.partition = partition;
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

    public Partition getPartition() {
        return partition;
    }
}

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
package com.offbynull.rfm.host.model.worker;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class Worker {
    private final HostSpecification hostSpecification;

    public Worker(HostSpecification hostSpecification) {
        Validate.notNull(hostSpecification);
        this.hostSpecification = hostSpecification;
    }

    public HostSpecification getHostSpecification() {
        return hostSpecification;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.hostSpecification);
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
        final Worker other = (Worker) obj;
        if (!Objects.equals(this.hostSpecification, other.hostSpecification)) {
            return false;
        }
        return true;
    }

}

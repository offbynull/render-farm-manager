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

import com.offbynull.rfm.host.model.work.Work;
import org.apache.commons.lang3.Validate;

/**
 * {@link Work} along with its associated key in the backend datastore.
 * @author Kasra Faghihi
 */
public class StoredWork {
    private final String key;
    private final Work work;
    private final WorkState state;

    public StoredWork(String key, Work work, WorkState state) {
        Validate.notNull(key);
        Validate.notNull(work);
        Validate.notNull(state);
        this.key = key;
        this.work = work;
        this.state = state;
    }

    public String getKey() {
        return key;
    }

    public Work getWork() {
        return work;
    }

    public WorkState getState() {
        return state;
    }
    
}

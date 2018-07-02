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

import org.apache.commons.lang3.Validate;

/**
 * {@link Worker} along with its associated key in the backend datastore.
 * @author Kasra Faghihi
 */
public class StoredWorker {
    private final String key;
    private final Worker worker;

    public StoredWorker(String key, Worker worker) {
        Validate.notNull(key);
        Validate.notNull(worker);
        this.key = key;
        this.worker = worker;
    }

    public String getKey() {
        return key;
    }

    public Worker getWorker() {
        return worker;
    }
    
}

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
package com.offbynull.rfm.gateways.select;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class UpdateHostRequestMessage {
    private final String host;
    private final UnmodifiableMap<String, String> updatedProperties;
    private final UnmodifiableMap<String, String> removedProperties;

    public UpdateHostRequestMessage(String host, Map<String, String> updatedProperties, Map<String, String> removedProperties) {
        Validate.notNull(host);
        Validate.notNull(updatedProperties);
        Validate.notNull(removedProperties);
        Validate.noNullElements(updatedProperties.keySet());
        Validate.noNullElements(updatedProperties.values());
        Validate.noNullElements(removedProperties.keySet());
        Validate.noNullElements(removedProperties.values());
        this.host = host;
        this.updatedProperties = (UnmodifiableMap<String, String>) UnmodifiableMap.unmodifiableMap(new HashMap<>(updatedProperties));
        this.removedProperties = (UnmodifiableMap<String, String>) UnmodifiableMap.unmodifiableMap(new HashMap<>(removedProperties));
    }

    public String getHost() {
        return host;
    }

    public UnmodifiableMap<String, String> getUpdatedProperties() {
        return updatedProperties;
    }

    public UnmodifiableMap<String, String> getRemovedProperties() {
        return removedProperties;
    }
}

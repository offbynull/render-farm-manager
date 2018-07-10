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
package com.offbynull.rfm.host.model.specification;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectVarId;
import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public abstract class Specification {
    private final UnmodifiableMap<String, Object> properties;

    Specification(Map<String, Object> properties, Set<String> keys) {
        Validate.notNull(properties);
        Validate.noNullElements(properties.keySet());
        properties.entrySet().forEach(e -> {
            String k = e.getKey();
            Object v = e.getValue();
            
            Validate.isTrue(k != null && v != null);
            isCorrectVarId(k, v.getClass());
        });
        
        // Normalize BigDecimal -- needed because BigDecimal.equals()/BigDecimal.hashCode() takes scale into account
        Map<String, Object> normalizedProperties = new HashMap<>();
        for (Entry<String, Object> prop : properties.entrySet()) {
            String name = prop.getKey();
            Object value = prop.getValue();
            
            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).stripTrailingZeros();
            }
            
            normalizedProperties.put(name, value);
        }
        
        Map<String, Object> keyProps = new HashMap<>(normalizedProperties);
        keyProps.keySet().retainAll(keys);
        Validate.isTrue(keyProps.keySet().equals(keys), "Key properties are missing: %s vs %s", keys, keyProps.keySet());
        
        this.properties = (UnmodifiableMap<String, Object>) unmodifiableMap(normalizedProperties);
    }
    
    /**
     * Get properties.
     * @return properties
     */
    public UnmodifiableMap<String, Object> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.properties);
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
        final Specification other = (Specification) obj;
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        return true;
    }


}

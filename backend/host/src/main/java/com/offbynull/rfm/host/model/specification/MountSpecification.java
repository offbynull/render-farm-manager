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

import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast0;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

/**
 * Mount specification.
 * @author Kasra Faghihi
 */
public final class MountSpecification extends Specification implements CapacityEnabledSpecification {
    private static final UnmodifiableSet<String> KEY_NAMES;

    static {
        LinkedHashSet<String> keyNames = new LinkedHashSet<>();
        keyNames.add("s_target");
        KEY_NAMES = (UnmodifiableSet<String>) unmodifiableSet(keyNames);
    }

    private final BigDecimal capacity;

    /**
     * Constructs a {@link MountSpecification} object.
     * <p>
     * Required key properties...
     * <ul>
     * <li>s_target -- mount target</li>
     * </ul>
     * @param capacity current capacity
     * @param properties disk properties
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code properties.keySet().contains("s_target")},
     * {@code !properties.keySet().contains(null)},
     * {@code !properties.values().contains(null)},
     * {@code properties.entrySet().forEach(e -> IdCheckUtils.isCorrectVarId(k, v.getClass()))},
     * {@code !properties.get("s_target").isEmpty()},
     * {@code NumberCheckUtils.isAtLeast0(capacity)}
     */
    public MountSpecification(BigDecimal capacity, Map<String, Object> properties) {
        super(properties, KEY_NAMES);
        Validate.notNull(capacity);
        
        String target = (String) properties.get("s_target");
        
        Validate.notEmpty(target);
        isAtLeast0(capacity);
        this.capacity = capacity.stripTrailingZeros();
    }
    
    @Override
    public BigDecimal getCapacity() {
        return capacity;
    }

    /**
     * Get key property names.
     * @return property names that uniquely identify this specification -- elements in returned set are order deterministically
     */
    public static UnmodifiableSet<String> getKeyPropertyNames() {
        return KEY_NAMES;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() ^ Objects.hash(
                this.capacity
        );
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
        final MountSpecification other = (MountSpecification) obj;
        if (!Objects.equals(this.capacity, other.capacity)) {
            return false;
        }
        return super.equals(obj);
    }
}

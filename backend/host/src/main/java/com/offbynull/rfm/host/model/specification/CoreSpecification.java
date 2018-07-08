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
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import java.math.BigDecimal;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

/**
 * CPU core specification.
 * @author Kasra Faghihi
 */
public final class CoreSpecification extends Specification {
    private static final UnmodifiableSet<String> KEY_NAMES;

    static {
        LinkedHashSet<String> keyNames = new LinkedHashSet<>();
        keyNames.add("n_core_id");
        KEY_NAMES = (UnmodifiableSet<String>) unmodifiableSet(keyNames);
    }

    private final UnmodifiableList<CpuSpecification> cpuSpecifications;
    
    /**
     * Constructs a {@link CoreSpecification} object.
     * <p>
     * Required key properties...
     * <ul>
     * <li>n_core_id -- id</li>
     * </ul>
     * @param cpuSpecifications CPU specification
     * @param properties disk properties
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code properties.keySet().contains("n_core_id")},
     * {@code !properties.keySet().contains(null)},
     * {@code !properties.values().contains(null)},
     * {@code properties.entrySet().forEach(e -> IdCheckUtils.isCorrectVarId(k, v.getClass()))},
     * {@code stream(cpuSpecifications).noneMatch(r -> r == null)},
     * {@code NumberCheckUtils.isNonFractional(properties.get("n_core_id"))},
     * {@code NumberCheckUtils.isAtLeast0(properties.get("n_core_id"))},
     * {@code cpuSpecifications.stream().map(x -> x.getKeyProperties()).distinct().count() == cpuSpecifications.length}
     */
    public CoreSpecification(CpuSpecification[] cpuSpecifications, Map<String, Object> properties) {
        super(properties, KEY_NAMES);
        
        Validate.notNull(cpuSpecifications);
        Validate.noNullElements(cpuSpecifications);

        BigDecimal coreId = (BigDecimal) properties.get("n_core_id");

        isNonFractional(coreId);
        isAtLeast0(coreId);
        
        Validate.isTrue(stream(cpuSpecifications).map(x -> x.getProperties()).distinct().count() == cpuSpecifications.length);

        this.cpuSpecifications = (UnmodifiableList<CpuSpecification>) unmodifiableList(new ArrayList<>(asList(cpuSpecifications)));        
    }
    
    /**
     * Get key property names.
     * @return property names that uniquely identify this specification -- elements in returned set are order deterministically
     */
    public static UnmodifiableSet<String> getKeyPropertyNames() {
        return KEY_NAMES;
    }
    
    /**
     * Get CPU specification.
     * @return CPU specification
     */
    public UnmodifiableList<CpuSpecification> getCpuSpecifications() {
        return cpuSpecifications;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ Objects.hash(
                this.cpuSpecifications
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
        if (!super.equals(obj)) {
            return false;
        }
        final CoreSpecification other = (CoreSpecification) obj;
        if (!Objects.equals(this.cpuSpecifications, other.cpuSpecifications)) {
            return false;
        }
        return true;
    }
    
}

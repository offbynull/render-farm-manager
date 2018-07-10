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
 * CPU socket specification.
 * @author Kasra Faghihi
 */
public final class SocketSpecification extends Specification {
    private static final UnmodifiableSet<String> KEY_NAMES;

    static {
        LinkedHashSet<String> keyNames = new LinkedHashSet<>();
        keyNames.add("n_socket_id");
        KEY_NAMES = (UnmodifiableSet<String>) unmodifiableSet(keyNames);
    }

    private final UnmodifiableList<CoreSpecification> coreSpecifications;
    
    /**
     * Constructs a {@link SocketSpecification} object.
     * <p>
     * Required key properties...
     * <ul>
     * <li>n_socket_id -- id</li>
     * </ul>
     * @param coreSpecifications CPU core specification
     * @param properties CPU core properties
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code properties.keySet().contains("n_socket_id")},
     * {@code !properties.keySet().contains(null)},
     * {@code !properties.values().contains(null)},
     * {@code properties.entrySet().forEach(e -> IdCheckUtils.isCorrectVarId(k, v.getClass()))},
     * {@code stream(coreSpecifications).noneMatch(r -> r == null)},
     * {@code NumberCheckUtils.isNonFractional(properties.get("n_socket_id"))},
     * {@code NumberCheckUtils.isAtLeast0(properties.get("n_socket_id"))},
     * {@code stream(coreSpecifications).map(x -> x.getKeyProperties()).distinct().count() == coreSpecifications.length}
     */
    public SocketSpecification(CoreSpecification[] coreSpecifications, Map<String, Object> properties) {
        super(properties, KEY_NAMES);
        
        Validate.notNull(coreSpecifications);
        Validate.noNullElements(coreSpecifications);

        BigDecimal socketId = (BigDecimal) properties.get("n_socket_id");

        isNonFractional(socketId);
        isAtLeast0(socketId);
        
        Validate.isTrue(stream(coreSpecifications).map(x -> x.getProperties()).distinct().count() == coreSpecifications.length);
        
        this.coreSpecifications = (UnmodifiableList<CoreSpecification>) unmodifiableList(new ArrayList<>(asList(coreSpecifications)));
    }
    
    /**
     * Get key property names.
     * @return property names that uniquely identify this specification -- elements in returned set are order deterministically
     */
    public static UnmodifiableSet<String> getKeyPropertyNames() {
        return KEY_NAMES;
    }
    
    /**
     * Get CPU core specification.
     * @return CPU core specification
     */
    public UnmodifiableList<CoreSpecification> getCoreSpecifications() {
        return coreSpecifications;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ Objects.hash(
                this.coreSpecifications
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
        final SocketSpecification other = (SocketSpecification) obj;
        if (!Objects.equals(this.coreSpecifications, other.coreSpecifications)) {
            return false;
        }
        return super.equals(obj);
    }
    
}

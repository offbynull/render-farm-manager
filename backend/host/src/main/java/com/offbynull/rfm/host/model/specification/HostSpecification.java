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
 * Host specifications.
 * @author Kasra Faghihi
 */
public final class HostSpecification extends Specification {
    private static final UnmodifiableSet<String> KEY_NAMES;
    
    static {
        LinkedHashSet<String> keyNames = new LinkedHashSet<>();
        keyNames.add("s_host");
        keyNames.add("n_port");
        KEY_NAMES = (UnmodifiableSet<String>) unmodifiableSet(keyNames);
    }
    
    private final UnmodifiableList<SocketSpecification> socketSpecifications;
    private final UnmodifiableList<GpuSpecification> gpuSpecifications;
    private final UnmodifiableList<MountSpecification> mountSpecifications;
    private final UnmodifiableList<RamSpecification> ramSpecifications;

    /**
     * Construct a {@link HostSpecification} object.
     * <p>
     * Required key properties...
     * <ul>
     * <li>s_host -- host name</li>
     * <li>n_port -- host port</li>
     * </ul>
     * @param socketSpecifications CPU socket specifications
     * @param gpuSpecifications GPU specifications
     * @param mountSpecifications disk specifications
     * @param ramSpecifications RAM specifications
     * @param properties host properties
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code properties.keySet().contains("s_host")},
     * {@code properties.keySet().contains("n_port")},
     * {@code !properties.keySet().contains(null)},
     * {@code !properties.values().contains(null)},
     * {@code properties.entrySet().forEach(e -> IdCheckUtils.isCorrectVarId(k, v.getClass()))},
     * {@code stream(socketSpecifications).noneMatch(r -> r == null)},
     * {@code stream(gpuSpecifications).noneMatch(r -> r == null)},
     * {@code stream(mountSpecifications).noneMatch(r -> r == null)},
     * {@code stream(ramSpecifications).noneMatch(r -> r == null)},
     * {@code !socketSpecifications.contains(null)},
     * {@code !gpuSpecifications.contains(null)},
     * {@code !mountSpecifications.contains(null)},
     * {@code !ramSpecifications.contains(null)},
     * {@code properties.get("s_host") instanceof String},
     * {@code properties.get("n_port") instanceof BigDecimal},
     * {@code !((String) properties.get("s_host")).isEmpty()},
     * {@code NumberCheckUtils.isNonFractional((BigDecimal) properties.get("n_port"))},
     * {@code ((BigDecimal) properties.get("n_port")).intValueExact() >= 1},
     * {@code ((BigDecimal) properties.get("n_port")).intValueExact() <= 65535},
     * {@code stream(socketSpecifications).map(x -> x.getKeyProperties()).distinct().count() == socketSpecifications.length},
     * {@code stream(gpuSpecifications).map(x -> x.getKeyProperties()).distinct().count() == gpuSpecifications.length},
     * {@code stream(mountSpecifications).map(x -> x.getKeyProperties()).distinct().count() == mountSpecifications.length},
     * {@code stream(ramSpecifications).map(x -> x.getKeyProperties()).distinct().count() == ramSpecifications.length},
     * {@code ramSpecifications.size() == 1},
     */
    public HostSpecification(
            SocketSpecification[] socketSpecifications,
            GpuSpecification[] gpuSpecifications,
            MountSpecification[] mountSpecifications,
            RamSpecification[] ramSpecifications,
            Map<String, Object> properties) {
        super(properties, KEY_NAMES);
        
        Validate.notNull(socketSpecifications);
        Validate.notNull(gpuSpecifications);
        Validate.notNull(mountSpecifications);
        Validate.notNull(ramSpecifications);
        Validate.noNullElements(socketSpecifications);
        Validate.noNullElements(gpuSpecifications);
        Validate.noNullElements(mountSpecifications);
        Validate.noNullElements(ramSpecifications);
        
        String host = (String) properties.get("s_host");
        BigDecimal port = (BigDecimal) properties.get("n_port");
        
        Validate.notEmpty(host);
        isNonFractional(port);
        Validate.isTrue(port.intValueExact() >= 1);
        Validate.isTrue(port.intValueExact() <= 65535);
        
        Validate.isTrue(stream(socketSpecifications).map(x -> x.getProperties()).distinct().count() == socketSpecifications.length);
        Validate.isTrue(stream(gpuSpecifications).map(x -> x.getProperties()).distinct().count() == gpuSpecifications.length);
        Validate.isTrue(stream(mountSpecifications).map(x -> x.getProperties()).distinct().count() == mountSpecifications.length);
        Validate.isTrue(stream(ramSpecifications).map(x -> x.getProperties()).distinct().count() == ramSpecifications.length);
        Validate.isTrue(ramSpecifications.length == 1);
        
        this.socketSpecifications = (UnmodifiableList<SocketSpecification>) unmodifiableList(new ArrayList<>(asList(socketSpecifications)));
        this.gpuSpecifications = (UnmodifiableList<GpuSpecification>) unmodifiableList(new ArrayList<>(asList(gpuSpecifications)));
        this.mountSpecifications = (UnmodifiableList<MountSpecification>) unmodifiableList(new ArrayList<>(asList(mountSpecifications)));
        this.ramSpecifications = (UnmodifiableList<RamSpecification>) unmodifiableList(new ArrayList<>(asList(ramSpecifications)));
    }
    
    /**
     * Get key property names.
     * @return property names that uniquely identify this specification -- elements in returned set are order deterministically
     */
    public static UnmodifiableSet<String> getKeyPropertyNames() {
        return KEY_NAMES;
    }

    /**
     * Get CPU socket specifications.
     * @return CPU socket specifications
     */
    public UnmodifiableList<SocketSpecification> getSocketSpecifications() {
        return socketSpecifications;
    }

    /**
     * Get GPU specifications
     * @return GPU specifications
     */
    public UnmodifiableList<GpuSpecification> getGpuSpecifications() {
        return gpuSpecifications;
    }

    /**
     * Get mount specifications
     * @return mount specifications
     */
    public UnmodifiableList<MountSpecification> getMountSpecifications() {
        return mountSpecifications;
    }

    /**
     * Get RAM specifications.
     * @return RAM specifications
     */
    public UnmodifiableList<RamSpecification> getRamSpecifications() {
        return ramSpecifications;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ Objects.hash(
                this.socketSpecifications,
                this.gpuSpecifications,
                this.mountSpecifications,
                this.ramSpecifications
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
        final HostSpecification other = (HostSpecification) obj;
        if (!Objects.equals(this.socketSpecifications, other.socketSpecifications)) {
            return false;
        }
        if (!Objects.equals(this.gpuSpecifications, other.gpuSpecifications)) {
            return false;
        }
        if (!Objects.equals(this.mountSpecifications, other.mountSpecifications)) {
            return false;
        }
        if (!Objects.equals(this.ramSpecifications, other.ramSpecifications)) {
            return false;
        }
        return super.equals(obj);
    }

    
}

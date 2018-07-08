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
package com.offbynull.rfm.host.model.requirement;

import com.offbynull.rfm.host.model.expression.Expression;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast1;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * Host requirement.
 * @author Kasra Faghihi
 */
public final class HostRequirement extends Requirement {
    private final UnmodifiableList<SocketRequirement> socketRequirements;
    private final UnmodifiableList<GpuRequirement> gpuRequirements;
    private final UnmodifiableList<RamRequirement> ramRequirements;
    private final UnmodifiableList<MountRequirement> mountRequirements;


    /**
     * Construct a {@link HostRequirement} object.
     * @param count count
     * @param whereCondition where condition
     * @param socketRequirements CPU socket requirements
     * @param gpuRequirements GPU requirements
     * @param ramRequirements RAM requirements
     * @param mountRequirements mount requirements
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code stream(socketRequirements).noneMatch(r -> r == null)},
     * {@code stream(gpuRequirements).noneMatch(r -> r == null)},
     * {@code stream(ramRequirements).noneMatch(r -> r == null)},
     * {@code stream(mountRequirements).noneMatch(r -> r == null)},
     * {@code NumberCheckUtils.isAtLeast1(count.getStart())},
     * {@code NumberCheckUtils.isNonFractional(count.getStart())},
     * {@code NumberCheckUtils.isNonFractional(count.getEnd())}
     */
    public HostRequirement(NumberRange count, Expression whereCondition,
            SocketRequirement[] socketRequirements,
            GpuRequirement[] gpuRequirements,
            RamRequirement[] ramRequirements,
            MountRequirement[] mountRequirements) {
        super(count, whereCondition);
        
        Validate.notNull(count, "Host requirement must have a count");
        
        Validate.notNull(socketRequirements);
        Validate.notNull(gpuRequirements);
        Validate.notNull(ramRequirements);
        Validate.notNull(mountRequirements);
        Validate.noNullElements(socketRequirements);
        Validate.noNullElements(gpuRequirements);
        Validate.noNullElements(ramRequirements);
        Validate.noNullElements(mountRequirements);

        isAtLeast1(count.getStart());
        isNonFractional(count.getStart());
        isNonFractional(count.getEnd());

        // Don't bother checking stuff like the commented block below. These requirements are intended to be generic host 
        // requirements. They're not nessecarily intended selecting hosts to do work on. For example, if I'm a PST and I want to find all
        // hosts with a certain amount of RAM available, I shouldn't have to specify cpus/mounts/etc...
        //
        // When selecting specifically for work, we can do checks to ensure that it does have a cpu/mount/etc...
//        Validate.isTrue(!socketRequirements.isEmpty() || !coreSelections.isEmpty() || !cpuSelections.isEmpty(),
//                "Atleast 1 of [socket,core,cpu] selection required");
//
//        Validate.isTrue(!mountRequirements.isEmpty(),
//                "Atleast 1 of [mount] selection required");
//
//        Validate.isTrue(ramRequirements.size() == 1
//                && ramRequirements.get(0).getNumberRange().getStart().compareTo(ONE) == 0
//                && ramRequirements.get(0).getNumberRange().getEnd().compareTo(ONE) == 0,
//                "Exactly 1 of [ram] selection required with range of [1,1]");
        
        this.socketRequirements = (UnmodifiableList<SocketRequirement>) unmodifiableList(new ArrayList<>(asList(socketRequirements)));
        this.gpuRequirements = (UnmodifiableList<GpuRequirement>) unmodifiableList(new ArrayList<>(asList(gpuRequirements)));
        this.ramRequirements = (UnmodifiableList<RamRequirement>) unmodifiableList(new ArrayList<>(asList(ramRequirements)));
        this.mountRequirements = (UnmodifiableList<MountRequirement>) unmodifiableList(new ArrayList<>(asList(mountRequirements)));
    }

    /**
     * Get CPU socket requirements.
     * @return CPU socket requirements
     */
    public UnmodifiableList<SocketRequirement> getSocketRequirements() {
        return socketRequirements;
    }

    /**
     * Get GPU requirements.
     * @return GPU requirements
     */
    public UnmodifiableList<GpuRequirement> getGpuRequirements() {
        return gpuRequirements;
    }

    /**
     * Get RAM requirements.
     * @return RAM requirements
     */
    public UnmodifiableList<RamRequirement> getRamRequirements() {
        return ramRequirements;
    }

    /**
     * Get mount requirements.
     * @return mount requirements
     */
    public UnmodifiableList<MountRequirement> getMountRequirements() {
        return mountRequirements;
    }
}

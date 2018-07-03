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
package com.offbynull.rfm.host.parser;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.CapacityRequirement;
import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import static java.util.Arrays.asList;
import org.apache.commons.lang3.Validate;

final class InternalRequirementFactory {

    private InternalRequirementFactory() {
        // do nothing
    }

    public static HostRequirement host(
            NumberRange numberRange,
            Expression whereCondition,
            SocketRequirement[] socketRequirements,
            GpuRequirement[] gpuRequirements,
            RamRequirement[] ramRequirements,
            MountRequirement[] mountRequirements) {
        return new HostRequirement(numberRange, whereCondition,
                asList(socketRequirements),
                asList(gpuRequirements),
                asList(ramRequirements),
                asList(mountRequirements));
    }

    public static SocketRequirement socket(
            NumberRange numberRange,
            Expression whereCondition,
            CoreRequirement[] coreRequirements) {
        return new SocketRequirement(numberRange, whereCondition,
                asList(coreRequirements));
    }

    public static CoreRequirement core(
            NumberRange numberRange,
            Expression whereCondition,
            CpuRequirement[] cpuRequirements) {
        return new CoreRequirement(numberRange, whereCondition,
                asList(cpuRequirements));
    }

    public static CpuRequirement cpu(
            NumberRange numberRange,
            Expression whereCondition,
            CapacityRequirement[] capacityRequirements) {
        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new CpuRequirement(numberRange, whereCondition,
                capacityRequirements[0]);
    }

    public static GpuRequirement gpu(
            NumberRange numberRange,
            Expression whereCondition,
            CapacityRequirement[] capacityRequirements) {
        
        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new GpuRequirement(numberRange, whereCondition,
                capacityRequirements[0]);
    }

    public static RamRequirement ram(
            NumberRange numberRange,
            Expression whereCondition,
            CapacityRequirement[] capacityRequirements) {

        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");

        return new RamRequirement(numberRange, whereCondition,
                capacityRequirements[0]);
    }

    public static MountRequirement mount(
            NumberRange numberRange,
            Expression whereCondition,
            CapacityRequirement[] capacityRequirements) {

        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new MountRequirement(numberRange, whereCondition,
                capacityRequirements[0]);
    }
    
    public static CapacityRequirement capacity(
            NumberRange numberRange,
            Expression whereCondition) {
        return new CapacityRequirement(numberRange, whereCondition);
    }
    
    public static CapacityRequirement available(
            NumberRange numberRange,
            Expression whereCondition) {
        return new CapacityRequirement(numberRange, whereCondition);
    }
}

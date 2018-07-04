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
            NumberRange capacityRange,
            Expression whereCondition,
            SocketRequirement[] socketRequirements,
            GpuRequirement[] gpuRequirements,
            RamRequirement[] ramRequirements,
            MountRequirement[] mountRequirements) {
        Validate.isTrue(capacityRange == null, "No capacity required");
        
        return new HostRequirement(numberRange, whereCondition,
                asList(socketRequirements),
                asList(gpuRequirements),
                asList(ramRequirements),
                asList(mountRequirements));
    }

    public static SocketRequirement socket(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition,
            CoreRequirement[] coreRequirements) {
        Validate.isTrue(capacityRange == null, "No capacity required");
        
        return new SocketRequirement(numberRange, whereCondition,
                asList(coreRequirements));
    }

    public static CoreRequirement core(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition,
            CpuRequirement[] cpuRequirements) {
        Validate.isTrue(capacityRange == null, "No capacity required");
        
        return new CoreRequirement(numberRange, whereCondition,
                asList(cpuRequirements));
    }

    public static CpuRequirement cpu(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition) {
        Validate.isTrue(capacityRange != null, "Capacity required");
        
        return new CpuRequirement(numberRange, whereCondition, capacityRange);
    }

    public static GpuRequirement gpu(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition) {
        
        Validate.isTrue(capacityRange != null, "Capacity required");
        
        return new GpuRequirement(numberRange, whereCondition, capacityRange);
    }

    public static RamRequirement ram(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition) {

        Validate.isTrue(capacityRange != null, "Capacity required");

        return new RamRequirement(numberRange, whereCondition, capacityRange);
    }

    public static MountRequirement mount(
            NumberRange numberRange,
            NumberRange capacityRange,
            Expression whereCondition) {

        Validate.isTrue(capacityRange != null, "Capacity required");
        
        return new MountRequirement(numberRange, whereCondition, capacityRange);
    }
}

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
package com.offbynull.rfm.host.model.parser;

import com.offbynull.rfm.host.model.selection.Expression;
import com.offbynull.rfm.host.model.selection.NumberRange;
import com.offbynull.rfm.host.model.selection.CapacitySelection;
import com.offbynull.rfm.host.model.selection.CoreSelection;
import com.offbynull.rfm.host.model.selection.CpuSelection;
import com.offbynull.rfm.host.model.selection.SocketSelection;
import com.offbynull.rfm.host.model.selection.MountSelection;
import com.offbynull.rfm.host.model.selection.GpuSelection;
import com.offbynull.rfm.host.model.selection.HostSelection;
import com.offbynull.rfm.host.model.selection.RamSelection;
import com.offbynull.rfm.host.model.selection.SelectionType;
import static java.util.Arrays.asList;
import org.apache.commons.lang3.Validate;

final class InternalRequirementFactory {

    private InternalRequirementFactory() {
        // do nothing
    }

    public static HostSelection host(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            SocketSelection[] socketRequirements,
            GpuSelection[] gpuRequirements,
            RamSelection[] ramRequirements,
            MountSelection[] mountRequirements) {
        return new HostSelection(numberRange, selectionType, whereCondition,
                asList(socketRequirements),
                asList(gpuRequirements),
                asList(ramRequirements),
                asList(mountRequirements));
    }
    
    public static HostSelection hosts(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            SocketSelection[] socketRequirements,
            GpuSelection[] gpuRequirements,
            RamSelection[] ramRequirements,
            MountSelection[] mountRequirements) {
        return host(numberRange, selectionType, whereCondition, socketRequirements, gpuRequirements, ramRequirements, mountRequirements);
    }

    public static SocketSelection socket(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CoreSelection[] coreRequirements) {
        return new SocketSelection(numberRange, selectionType, whereCondition,
                asList(coreRequirements));
    }

    public static SocketSelection sockets(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CoreSelection[] coreRequirements) {
        return socket(numberRange, selectionType, whereCondition, coreRequirements);
    }

    public static CoreSelection core(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CpuSelection[] cpuRequirements) {
        return new CoreSelection(numberRange, selectionType, whereCondition,
                asList(cpuRequirements));
    }

    public static CoreSelection cores(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CpuSelection[] cpuRequirements) {
        return core(numberRange, selectionType, whereCondition, cpuRequirements);
    }

    public static CpuSelection cpu(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {
        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new CpuSelection(numberRange, selectionType, whereCondition,
                capacityRequirements[0]);
    }

    public static CpuSelection cpus(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {
        return cpu(numberRange, selectionType, whereCondition, capacityRequirements);
    }

    public static GpuSelection gpu(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {
        
        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new GpuSelection(numberRange, selectionType, whereCondition,
                capacityRequirements[0]);
    }

    public static GpuSelection gpus(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {
        return gpu(numberRange, selectionType, whereCondition, capacityRequirements);
    }

    public static RamSelection ram(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {

        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");

        return new RamSelection(numberRange, selectionType, whereCondition,
                capacityRequirements[0]);
    }

    public static MountSelection mount(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {

        Validate.isTrue(capacityRequirements.length == 1, "Exactly 1 capacity requirement required");
        
        return new MountSelection(numberRange, selectionType, whereCondition,
                capacityRequirements[0]);
    }

    public static MountSelection mounts(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition,
            CapacitySelection[] capacityRequirements) {
        return mount(numberRange, selectionType, whereCondition, capacityRequirements);
    }
    
    public static CapacitySelection capacity(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition) {
        return new CapacitySelection(numberRange, selectionType, whereCondition);
    }
    
    public static CapacitySelection available(
            NumberRange numberRange,
            SelectionType selectionType,
            Expression whereCondition) {
        return new CapacitySelection(numberRange, selectionType, whereCondition);
    }
}

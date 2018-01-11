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
package com.offbynull.rfm.host.model.selection;

import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast1;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * Host selection.
 * @author Kasra Faghihi
 */
public final class HostSelection extends Selection {
    private final UnmodifiableList<SocketSelection> socketSelections;
    private final UnmodifiableList<GpuSelection> gpuSelections;
    private final UnmodifiableList<RamSelection> ramSelections;
    private final UnmodifiableList<MountSelection> mountSelections;


    /**
     * Construct a {@link HostSelection} object.
     * @param numberRange number range
     * @param selectionType selection type
     * @param whereCondition where condition
     * @param socketSelections CPU socket selections
     * @param gpuSelections GPU selections
     * @param ramSelections RAM selections
     * @param mountSelections mount selections
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code !socketSelections.contains(null)},
     * {@code !coreSelections.contains(null)},
     * {@code !cpuSelections.contains(null)},
     * {@code !gpuSelections.contains(null)},
     * {@code !ramSelections.contains(null)},
     * {@code !mountSelections.contains(null)},
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public HostSelection(NumberRange numberRange, SelectionType selectionType, Expression whereCondition,
            List<SocketSelection> socketSelections,
            List<GpuSelection> gpuSelections,
            List<RamSelection> ramSelections,
            List<MountSelection> mountSelections) {
        super(numberRange, selectionType, whereCondition);
        
        Validate.notNull(socketSelections);
        Validate.notNull(gpuSelections);
        Validate.notNull(ramSelections);
        Validate.notNull(mountSelections);
        Validate.noNullElements(socketSelections);
        Validate.noNullElements(gpuSelections);
        Validate.noNullElements(ramSelections);
        Validate.noNullElements(mountSelections);

        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());

        // Don't bother checking stuff like the commented block below. These selections are intended to be generic host selection
        // requirements. They're not nessecarily intended selecting hosts to do work on. For example, if I'm a PST and I want to find all
        // hosts with a certain amount of RAM available, I shouldn't have to specify cpus/mounts/etc...
        //
        // When selecting specifically for work, we can do checks to ensure that it does have a cpu/mount/etc...
//        Validate.isTrue(!socketSelections.isEmpty() || !coreSelections.isEmpty() || !cpuSelections.isEmpty(),
//                "Atleast 1 of [socket,core,cpu] selection required");
//
//        Validate.isTrue(!mountSelections.isEmpty(),
//                "Atleast 1 of [mount] selection required");
//
//        Validate.isTrue(ramSelections.size() == 1
//                && ramSelections.get(0).getNumberRange().getStart().compareTo(ONE) == 0
//                && ramSelections.get(0).getNumberRange().getEnd().compareTo(ONE) == 0,
//                "Exactly 1 of [ram] selection required with range of [1,1]");
        
        this.socketSelections = (UnmodifiableList<SocketSelection>) unmodifiableList(new ArrayList<>(socketSelections));
        this.gpuSelections = (UnmodifiableList<GpuSelection>) unmodifiableList(new ArrayList<>(gpuSelections));
        this.ramSelections = (UnmodifiableList<RamSelection>) unmodifiableList(new ArrayList<>(ramSelections));
        this.mountSelections = (UnmodifiableList<MountSelection>) unmodifiableList(new ArrayList<>(mountSelections));
    }

    /**
     * Get CPU socket selections.
     * @return CPU socket selections
     */
    public UnmodifiableList<SocketSelection> getSocketSelections() {
        return socketSelections;
    }

    /**
     * Get GPU selections.
     * @return GPU selections
     */
    public UnmodifiableList<GpuSelection> getGpuSelections() {
        return gpuSelections;
    }

    /**
     * Get RAM selections.
     * @return RAM selections
     */
    public UnmodifiableList<RamSelection> getRamSelections() {
        return ramSelections;
    }

    /**
     * Get mount selections.
     * @return mount selections
     */
    public UnmodifiableList<MountSelection> getMountSelections() {
        return mountSelections;
    }
}

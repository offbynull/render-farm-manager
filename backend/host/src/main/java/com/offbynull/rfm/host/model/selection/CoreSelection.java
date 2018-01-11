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
 * CPU core selection.
 * @author Kasra Faghihi
 */
public final class CoreSelection extends Selection {

    private final UnmodifiableList<CpuSelection> cpuSelections;
    
    /**
     * Construct a {@link CoreSelection} object.
     * @param numberRange number range
     * @param selectionType selection type
     * @param whereCondition where condition
     * @param cpuSelections CPU selections
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code !cpuSelections.contains(null)},
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public CoreSelection(NumberRange numberRange, SelectionType selectionType, Expression whereCondition,
            List<CpuSelection> cpuSelections) {
        super(numberRange, selectionType, whereCondition);
        
        Validate.notNull(cpuSelections);
        Validate.noNullElements(cpuSelections);

        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        this.cpuSelections = (UnmodifiableList<CpuSelection>) unmodifiableList(new ArrayList<>(cpuSelections));
    }

    /**
     * Get CPU selections
     * @return CPU selections
     */
    public UnmodifiableList<CpuSelection> getCpuSelections() {
        return cpuSelections;
    }
    
}

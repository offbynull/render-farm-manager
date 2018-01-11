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
import org.apache.commons.lang3.Validate;

/**
 * CPU selection. Capacity is measured in CFS slices.
 * @author Kasra Faghihi
 */
public final class CpuSelection extends Selection implements CapacityEnabledSelection {

    private final CapacitySelection capacitySelection;
    
    /**
     * Construct a {@link CpuSelection} object.
     * @param numberRange number range
     * @param selectionType selection type
     * @param whereCondition where condition
     * @param capacitySelection capacity selection (in CFS slices)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code NumberCheckUtils.isAtLeast0(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public CpuSelection(NumberRange numberRange, SelectionType selectionType, Expression whereCondition,
            CapacitySelection capacitySelection) {
        super(numberRange, selectionType, whereCondition);

        Validate.notNull(capacitySelection);
        
        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        this.capacitySelection = capacitySelection;
    }

    @Override
    public CapacitySelection getCapacitySelection() {
        return capacitySelection;
    }
}

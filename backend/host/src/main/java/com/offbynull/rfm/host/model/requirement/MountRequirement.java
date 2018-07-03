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
import org.apache.commons.lang3.Validate;

/**
 * Mount requirement. Capacity is measured in bytes.
 * @author Kasra Faghihi
 */
public final class MountRequirement extends Requirement implements CapacityEnabledRequirement {

    private final CapacityRequirement capacityRequirement;
    
    /**
     * Construct a {@link MountRequirement} object.
     * @param numberRange number range
     * @param whereCondition where condition
     * @param capacityRequirement capacity requirement (in bytes)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public MountRequirement(NumberRange numberRange, Expression whereCondition,
            CapacityRequirement capacityRequirement) {
        super(numberRange, whereCondition);

        Validate.notNull(capacityRequirement);
        
        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        this.capacityRequirement = capacityRequirement;
    }

    /**
     * Get capacity requirement
     * @return capacity requirement
     */
    @Override
    public CapacityRequirement getCapacityRequirement() {
        return capacityRequirement;
    }
}

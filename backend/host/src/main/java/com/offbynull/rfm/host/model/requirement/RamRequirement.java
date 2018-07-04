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
 * RAM requirement. Capacity is measured in bytes.
 * @author Kasra Faghihi
 */
public final class RamRequirement extends Requirement implements CapacityEnabledRequirement {

    private final NumberRange capacityRange;
    
    /**
     * Construct a {@link RamRequirement} object.
     * @param numberRange number range
     * @param whereCondition where condition
     * @param capacityRange ram range (in bytes)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public RamRequirement(NumberRange numberRange, Expression whereCondition,
            NumberRange capacityRange) {
        super(numberRange, whereCondition);

        Validate.notNull(capacityRange);
        
        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        this.capacityRange = capacityRange;
    }

    @Override
    public NumberRange getCapacityRange() {
        return capacityRange;
    }
}

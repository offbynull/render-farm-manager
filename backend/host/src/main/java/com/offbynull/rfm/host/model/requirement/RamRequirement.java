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
     * @param count count
     * @param whereCondition where condition
     * @param capacityRange ram range (in bytes)
     * @throws NullPointerException if any argument other than {@code count} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code count != null && NumberCheckUtils.isAtLeast1(count.getStart())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getStart())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getEnd())}
     */
    public RamRequirement(NumberRange count, Expression whereCondition,
            NumberRange capacityRange) {
        super(count, whereCondition);

        Validate.notNull(capacityRange);
        
        if (count != null) {
            isAtLeast1(count.getStart());
            isNonFractional(count.getStart());
            isNonFractional(count.getEnd());
        }
        
        this.capacityRange = capacityRange;
    }

    @Override
    public NumberRange getCapacityRange() {
        return capacityRange;
    }
}

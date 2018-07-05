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
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast0;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtMost1;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import org.apache.commons.lang3.Validate;

/**
 * GPU requirement.
 * @author Kasra Faghihi
 */
public final class GpuRequirement extends Requirement implements CapacityEnabledRequirement {
    
    private final NumberRange capacityRange; // doesn't make sense being anything other than 0 or 1
    
    /**
     * Construct a {@link GpuRequirement} object.
     * @param count count
     * @param whereCondition where condition
     * @param capacityRange available range
     * @throws NullPointerException if any argument other than {@code count} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code count != null && NumberCheckUtils.isAtLeast0(count.getStart())},
     * {@code count != null && NumberCheckUtils.isAtMost1(count.getEnd())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getStart())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getEnd())}
     */
    public GpuRequirement(NumberRange count, Expression whereCondition,
            NumberRange capacityRange) {
        super(count, whereCondition);
        
        Validate.notNull(capacityRange);
        
        if (count != null) {
            isAtLeast0(count.getStart());
            isAtMost1(count.getEnd());
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
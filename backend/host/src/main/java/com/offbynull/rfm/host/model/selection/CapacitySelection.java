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
 * Generic capacity requirement. Used to represent bytes, speed, etc..
 * @author Kasra Faghihi
 */
public final class CapacitySelection extends Selection {

    /**
     * Construct a {@link CapacitySelection} object.
     * @param numberRange number range
     * @param selectionType selection type
     * @param whereCondition where condition
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())},
     * {@code whereCondition instanceof BooleanLiteralExpression},
     * {@code ((BooleanLiteralExpression) whereCondition).getValue() == Boolean.TRUE}
     */
    public CapacitySelection(NumberRange numberRange, SelectionType selectionType, Expression whereCondition) {
        super(numberRange, selectionType, whereCondition);

        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        // Where condition must always be set to true (defaults to true if it's nonexistant in the parser)
        Validate.isTrue(whereCondition instanceof BooleanLiteralExpression);
        Validate.isTrue(((BooleanLiteralExpression) whereCondition).getValue() == Boolean.TRUE);
    }
    
}

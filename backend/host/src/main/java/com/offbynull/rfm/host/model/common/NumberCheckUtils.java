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
package com.offbynull.rfm.host.model.common;

import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;

public final class NumberCheckUtils {
    private NumberCheckUtils() {
        // do nothing
    }
    
    
    
    public static void isAtLeast0(BigDecimal value) {
        Validate.notNull(value);
        Validate.isTrue(value.compareTo(BigDecimal.ZERO) >= 0, "Value must >= 0");
    }

    public static void isAtLeast1(BigDecimal value) {
        Validate.notNull(value);
        Validate.isTrue(value.compareTo(BigDecimal.ONE) >= 0, "Value must >= 1");
    }

    public static void isAtMost1(BigDecimal value) {
        Validate.notNull(value);
        Validate.isTrue(value.compareTo(BigDecimal.ONE) <= 0, "Value must <= 1");
    }

    public static void isAtMost(BigDecimal value, long limit) {
        Validate.notNull(value);
        Validate.isTrue(value.compareTo(BigDecimal.valueOf(limit)) <= 0, "Value must <= %d", limit);
    }
    
    public static void isNonFractional(BigDecimal value) {
        Validate.notNull(value);
        try {
            value.toBigIntegerExact();
        } catch (ArithmeticException ae) {
            throw new IllegalArgumentException("Fractional value not allowed");
        }
    }

}

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

import java.math.BigDecimal;
import static java.math.BigDecimal.ZERO;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class NumberRange {
    /**
     * A number range of [0,0].
     */
    public static final NumberRange NONE = new NumberRange(ZERO, ZERO);
    
    private final BigDecimal start;
    private final BigDecimal end;

    public NumberRange(BigDecimal start, BigDecimal end) {
        Validate.notNull(start);
        Validate.notNull(end);
        Validate.isTrue(end.compareTo(start) >= 0);

        this.start = start;
        this.end = end;
    }

    public BigDecimal getStart() {
        return start;
    }

    public BigDecimal getEnd() {
        return end;
    }

    public int compareStart(long i) {
        return start.compareTo(BigDecimal.valueOf(i));
    }

    public int compareEnd(long i) {
        return end.compareTo(BigDecimal.valueOf(i));
    }

    public boolean isInRange(long i) {
        return isInRange(BigDecimal.valueOf(i));
    }
    
    public boolean isInRange(BigDecimal bd) {
        Validate.notNull(bd);
        return bd.compareTo(start) >= 0 && bd.compareTo(end) <= 0;
    }
    
    public static NumberRange of(long value) {
        return new NumberRange(BigDecimal.valueOf(value), BigDecimal.valueOf(value));
    }
    
    public static NumberRange of(long start, long end) {
        return new NumberRange(BigDecimal.valueOf(start), BigDecimal.valueOf(end));
    }
    
    public static NumberRange combineNumberRanges(NumberRange one, NumberRange two) {
        Validate.notNull(one);
        Validate.notNull(two);
        
        return combineNumberRanges(List.of(one, two));
    }

    public static NumberRange combineNumberRanges(List<NumberRange> numberRanges) {
        Validate.notNull(numberRanges);
        Validate.noNullElements(numberRanges);
        
        BigDecimal start = ZERO;
        BigDecimal end = ZERO;
        
        for (NumberRange numberRange : numberRanges) {
            start = start.add(numberRange.getStart());
            end = end.add(numberRange.getEnd());
        }
        
        return new NumberRange(start, end);
    }
}

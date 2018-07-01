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

import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import org.apache.commons.lang3.Validate;

public final class NumberRange {
    /**
     * A number range of [1,1].
     */
    public static final NumberRange SINGLE = new NumberRange(ONE, ONE);
    
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
}

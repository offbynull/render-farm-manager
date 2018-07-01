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
package com.offbynull.rfm.host.model.expression;

import static com.offbynull.rfm.host.model.expression.DataType.NUMBER;
import java.math.BigDecimal;

/**
 * Number literal expression node.
 * @author Kasra Faghihi
 */
public final class NumberLiteralExpression extends LiteralExpression<BigDecimal> {

    /**
     * Create a {@link NumberLiteralExpression} object.
     * @param value value
     * @throws NullPointerException if any argument is {@code null}
     */
    public NumberLiteralExpression(BigDecimal value) {
        super(NUMBER, value);
    }
}

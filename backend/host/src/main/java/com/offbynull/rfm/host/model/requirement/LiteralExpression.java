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

import org.apache.commons.lang3.Validate;

/**
 * Literal expression node.
 * @param <T> type
 * @author Kasra Faghihi
 */
public abstract class LiteralExpression<T> extends Expression {
    
    private final T value;
    
    LiteralExpression(DataType type, T value) {
        super(type);
        
        Validate.notNull(type);
        Validate.notNull(value);
        
        this.value = value;
    }

    /**
     * Get value.
     * @return value
     */
    public T getValue() {
        return value;
    }

}

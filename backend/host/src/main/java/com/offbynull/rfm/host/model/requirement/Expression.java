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
 * Abstract expression node.
 * @author Kasra Faghihi
 */
public abstract class Expression {
    private final DataType type;
    
    /**
     * Create a {@link Expression} object.
     * @param type type
     * @throws NullPointerException if any argument is {@code null}
     */
    public Expression(DataType type) {
        Validate.notNull(type);
        this.type = type;
    }

    /**
     * Get type the evaluation of this expression will result to.
     * @return resulting type
     */
    public DataType getType() {
        return type;
    }
    
}

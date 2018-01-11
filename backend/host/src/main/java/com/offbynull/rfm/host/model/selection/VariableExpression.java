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

import static com.offbynull.rfm.host.model.selection.DataType.BOOLEAN;
import static com.offbynull.rfm.host.model.selection.DataType.NUMBER;
import static com.offbynull.rfm.host.model.selection.DataType.STRING;
import org.apache.commons.lang3.Validate;

/**
 * Variable expression node.
 * @author Kasra Faghihi
 */
public final class VariableExpression extends Expression {
    private final String scope;
    private final String name;

    /**
     * Create a {@link VariableExpression} object.
     * @param scope scope
     * @param name name
     * @throws NullPointerException if any argument is {@code null}
     */
    public VariableExpression(String scope, String name) {
        super(prefixToType(name));

        Validate.notNull(scope);
        Validate.notNull(name);

        this.scope = scope;
        this.name = name;
    }

    /**
     * Get scope.
     * @return scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Get name.
     * @return name
     */
    public String getName() {
        return name;
    }
    
    private static DataType prefixToType(String input) {
        switch (input.substring(0, 2)) {
            case "b_":
                return BOOLEAN;
            case "n_":
                return NUMBER;
            case "s_":
                return STRING;
            default:
                throw new IllegalArgumentException(); // should never happen
        }
    }
}

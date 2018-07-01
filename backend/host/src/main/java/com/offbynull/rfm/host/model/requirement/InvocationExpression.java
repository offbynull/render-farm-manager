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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

// REMEMBER tag functions are evaluated at runtime to literals/constants -- they'll never be represented as invocation nodes. Invocation
// nodes are explicitly for requirements because they get invoked when trying to find a hosts instead of when script is built.

/**
 * Function invocation expression node.
 * @author Kasra Faghihi
 */
public final class InvocationExpression extends Expression {
    private final RequirementFunction function;
    private final UnmodifiableList<Expression> arguments;

    /**
     * Create a {@link InvocationExpression} object.
     * @param function function
     * @param arguments function arguments
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the follows conditions are NOT met:
     * {@code !arguments.contains(null) }
     */
    public InvocationExpression(RequirementFunction function, Expression... arguments) {
        this(function, Arrays.asList(arguments));
    }

    /**
     * Create a {@link InvocationExpression} object.
     * @param function function
     * @param arguments function arguments
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the follows conditions are NOT met:
     * {@code !arguments.contains(null) }
     */
    public InvocationExpression(RequirementFunction function, List<Expression> arguments) {
        super(function.getReturnType());
        
        Validate.notNull(function);
        Validate.notNull(arguments);
        Validate.noNullElements(arguments);

        this.function = function;
        this.arguments = (UnmodifiableList<Expression>) unmodifiableList(new ArrayList<>(arguments));
    }

    /**
     * Get function name and signature.
     * @return function
     */
    public RequirementFunction getFunction() {
        return function;
    }

    /**
     * Get expression nodes for the function arguments.
     * @return arguments
     */
    public UnmodifiableList<Expression> getArguments() {
        return arguments;
    }
    
}

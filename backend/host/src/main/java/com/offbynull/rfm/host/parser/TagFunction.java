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
package com.offbynull.rfm.host.parser;

import com.offbynull.rfm.host.model.common.IdCheckUtils;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.Validate;

/**
 * Tag function name and invocation point.
 * @author Kasra Faghihi
 */
public final class TagFunction {
    private final String name;
    private final Function<List<Object>, Object> invocationPoint;

    /**
     * Constructs a {@link TagFunction} object.
     * @param name function name
     * @param invocationPoint function invocation point
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if name is empty or an invalid identifier
     */
    public TagFunction(String name, Function<List<Object>, Object> invocationPoint) {
        Validate.notNull(name);
        Validate.notNull(invocationPoint);
        IdCheckUtils.isCorrectId(name);
        this.name = name;
        this.invocationPoint = invocationPoint;
    }

    /**
     * Get name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get invocation point.
     * @return invocation point
     */
    public Function<List<Object>, Object> getInvocationPoint() {
        return invocationPoint;
    }
}

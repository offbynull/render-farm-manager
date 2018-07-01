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

import com.offbynull.rfm.host.model.common.IdCheckUtils;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * Function name and signature.
 * @author Kasra Faghihi
 */
public final class RequirementFunction {
    private final DataType returnType;
    private final String name;
    private final UnmodifiableList<DataType> parameters;

    /**
     * Constructs a {@link Function} object.
     * @param returnType function return type
     * @param name function name
     * @param parameterTypes function parameter types
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if name is empty or an invalid identifier
     */
    public RequirementFunction(DataType returnType, String name, DataType... parameterTypes) {
        this(returnType, name, asList(parameterTypes));
    }

    /**
     * Constructs a {@link Function} object.
     * @param returnType function return type
     * @param name function name
     * @param parameterTypes function parameter types
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if name is empty or an invalid identifier
     */
    public RequirementFunction(DataType returnType, String name, List<DataType> parameterTypes) {
        Validate.notNull(returnType);
        Validate.notNull(name);
        Validate.notNull(parameterTypes);
        
        IdCheckUtils.isCorrectId(name);
        
        this.returnType = returnType;
        this.name = name;
        this.parameters = (UnmodifiableList<DataType>) unmodifiableList(new ArrayList<>(parameterTypes));
    }

    /**
     * Get return type.
     * @return return type
     */
    public DataType getReturnType() {
        return returnType;
    }

    /**
     * Get name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get parameters.
     * @return parameters
     */
    public UnmodifiableList<DataType> getParameterTypes() {
        return parameters;
    }
    
}

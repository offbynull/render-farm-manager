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

import static com.offbynull.rfm.host.model.requirement.DataType.BOOLEAN;
import org.apache.commons.lang3.Validate;

/**
 * Abstract requirement.
 * @author Kasra Faghihi
 */
public abstract class Requirement {
    private final NumberRange numberRange;
    private final RequirementType requirementType;
    private final Expression whereCondition;

    Requirement(NumberRange numberRange, RequirementType requirementType, Expression whereCondition) {
        Validate.notNull(numberRange);
        Validate.notNull(requirementType);
        Validate.notNull(whereCondition);

        Validate.isTrue(BOOLEAN == whereCondition.getType());

        this.numberRange = numberRange;
        this.requirementType = requirementType;
        this.whereCondition = whereCondition;
    }

    /**
     * Get number range.
     * @return number range
     */
    public NumberRange getNumberRange() {
        return numberRange;
    }

    /**
     * Get requirement type.
     * @return requirement type
     */
    public RequirementType getRequirementType() {
        return requirementType;
    }
    
    /**
     * Where condition.
     * @return where condition
     */
    public Expression getWhereCondition() {
        return whereCondition;
    }
    
}
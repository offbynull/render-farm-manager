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

import com.offbynull.rfm.host.model.expression.Expression;
import static com.offbynull.rfm.host.model.expression.DataType.BOOLEAN;
import org.apache.commons.lang3.Validate;

/**
 * Abstract requirement.
 * @author Kasra Faghihi
 */
public abstract class Requirement {
    private final NumberRange numberRange;
    private final Expression whereCondition;

    Requirement(NumberRange numberRange, Expression whereCondition) {
        Validate.notNull(numberRange);
        Validate.notNull(whereCondition);

        Validate.isTrue(BOOLEAN == whereCondition.getType());

        this.numberRange = numberRange;
        this.whereCondition = whereCondition;
    }

    /**
     * Get number range.
     * <p>
     * If this is NOT {@code null}, it means the children of this requirement are replicated for each parent requirement found. For example,
     * imagine the following requirement hierarchy...
     * <pre>
     * [1,5] parent {
     *   [3,6] child
     * }
     * </pre>
     * 1 to 5 parents will be returned. For each parent found, that parent will have anywhere from 3 to 6 children that are guaranteed to be
     * owned by that parent.
     * <p>
     * If this is {@code null}, it means the children of this requirement are spread across one or more parents. For example, image the
     * following requirement hiearachy...
     * <pre>
     * ? parent {
     *   [3,6] child
     * }
     * </pre>
     * 3 to 6 children will be found, but each what parent they're bound to is unknown.
     * @return number range
     */
    public NumberRange getNumberRange() {
        return numberRange;
    }
    
    /**
     * Where condition.
     * @return where condition
     */
    public Expression getWhereCondition() {
        return whereCondition;
    }
    
}

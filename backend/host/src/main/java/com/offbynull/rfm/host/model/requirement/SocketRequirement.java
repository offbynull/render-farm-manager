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
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast1;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * CPU socket requirement.
 * @author Kasra Faghihi
 */
public final class SocketRequirement extends Requirement {

    private final UnmodifiableList<CoreRequirement> coreRequirements;
    
    /**
     * Construct a {@link SocketRequirement} object.
     * @param count count
     * @param whereCondition where condition
     * @param coreRequirements CPU core requirements
     * @throws NullPointerException if any argument other than {@code count} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code stream(coreRequirements).noneMatch(r -> r == null)},
     * {@code count != null && NumberCheckUtils.isAtLeast1(count.getStart())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getStart())},
     * {@code count != null && NumberCheckUtils.isNonFractional(count.getEnd())}
     */
    public SocketRequirement(NumberRange count, Expression whereCondition,
            CoreRequirement[] coreRequirements) {
        super(count, whereCondition);
        
        Validate.notNull(coreRequirements);
        Validate.noNullElements(coreRequirements);

        if (count != null) {
            isAtLeast1(count.getStart());
            isNonFractional(count.getStart());
            isNonFractional(count.getEnd());
        }
        
        this.coreRequirements = (UnmodifiableList<CoreRequirement>) unmodifiableList(new ArrayList<>(asList(coreRequirements)));
    }

    /**
     * Get CPU core requirements
     * @return CPU core requirements
     */
    public UnmodifiableList<CoreRequirement> getCoreRequirements() {
        return coreRequirements;
    }
    
}

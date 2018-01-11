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

import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast1;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isNonFractional;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * CPU socket selection.
 * @author Kasra Faghihi
 */
public final class SocketSelection extends Selection {

    private final UnmodifiableList<CoreSelection> coreSelections;
    
    /**
     * Construct a {@link SocketSelection} object.
     * @param numberRange number range
     * @param selectionType selection type
     * @param whereCondition where condition
     * @param coreSelections CPU core selections
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions do NOT evaluate to true:
     * {@code !coreSelections.contains(null)},
     * {@code NumberCheckUtils.isAtLeast1(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getStart())},
     * {@code NumberCheckUtils.isNonFractional(numberRange.getEnd())}
     */
    public SocketSelection(NumberRange numberRange, SelectionType selectionType, Expression whereCondition,
            List<CoreSelection> coreSelections) {
        super(numberRange, selectionType, whereCondition);
        
        Validate.notNull(coreSelections);
        Validate.noNullElements(coreSelections);

        isAtLeast1(numberRange.getStart());
        isNonFractional(numberRange.getStart());
        isNonFractional(numberRange.getEnd());
        
        this.coreSelections = (UnmodifiableList<CoreSelection>) unmodifiableList(new ArrayList<>(coreSelections));
    }

    /**
     * Get CPU core selections
     * @return CPU core selections
     */
    public UnmodifiableList<CoreSelection> getCoreSelections() {
        return coreSelections;
    }
    
}

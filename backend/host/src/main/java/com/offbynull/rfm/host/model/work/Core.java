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
package com.offbynull.rfm.host.model.work;

import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectId;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast0;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtMost1;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Core {
    private final String id;
    private final BigDecimal priority;
    private final UnmodifiableSet<String> parents;

    public Core(String id, BigDecimal priority, Collection<String> parents) {
        Validate.notNull(id);
        Validate.notNull(priority);
        Validate.notNull(parents);
        Validate.noNullElements(parents);
        
        isCorrectId(id);
        isAtLeast0(priority);
        isAtMost1(priority);
        parents.forEach(depId -> isCorrectId(depId));
        Validate.isTrue(!parents.contains(id), "ID cannot be in dependencies: %s", id);

        this.id = id;
        this.priority = priority;
        this.parents = (UnmodifiableSet<String>) unmodifiableSet(new HashSet<>(parents));
    }

    public String getId() {
        return id;
    }

    public BigDecimal getPriority() {
        return priority;
    }

    public UnmodifiableSet<String> getParents() {
        return parents;
    }

}

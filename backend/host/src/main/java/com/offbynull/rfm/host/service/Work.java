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
package com.offbynull.rfm.host.service;

import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectId;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectVarId;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast0;
import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtMost1;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import static java.util.stream.Collectors.toMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;

public final class Work {
    private final String id;
    private final BigDecimal priority;
    private final UnmodifiableSet<String> parents;
    private final UnmodifiableMap<String, Object> tags;
    private final String requirementsScript;

    public Work(String id, BigDecimal priority, Collection<String> parents, Map<String, Object> tags, String requirementsScript) {
        Validate.notNull(id);
        Validate.notNull(priority);
        Validate.notNull(parents);
        Validate.notNull(tags);
        Validate.notNull(requirementsScript);
        Validate.noNullElements(parents);
        
        isCorrectId(id);
        isAtLeast0(priority);
        isAtMost1(priority);
        parents.forEach(depId -> isCorrectId(depId));
        Validate.isTrue(!parents.contains(id), "ID cannot be in dependencies: %s", id);
        
        this.id = id;
        this.priority = priority.stripTrailingZeros();
        this.parents = (UnmodifiableSet<String>) unmodifiableSet(new HashSet<>(parents));
        
        tags.entrySet().forEach(e -> {
            String name = e.getKey();
            Object value = e.getValue();

            Validate.notNull(name);
            Validate.notNull(value);

            isCorrectVarId(name, value.getClass());
        });
        
        Map<String, Object> tagsWithNormalizedNumbers = tags.entrySet().stream()
                .map(e -> {
                    if (e.getKey().startsWith("n_")) {
                        return Map.entry(e.getKey(), ((BigDecimal) e.getValue()).stripTrailingZeros());
                    }
                    return e;
                })
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));
        
        this.tags = (UnmodifiableMap<String, Object>) unmodifiableMap(tagsWithNormalizedNumbers);
        this.requirementsScript = requirementsScript;
    }

    /**
     * Get ID.
     * @return ID 
     */
    public String getId() {
        return id;
    }

    /**
     * Get priority.
     * @return priority
     */
    public BigDecimal getPriority() {
        return priority;
    }

    /**
     * Get parent IDs.
     * @return parent IDs
     */
    public UnmodifiableSet<String> getParents() {
        return parents;
    }

    /**
     * Get tags.
     * @return tags
     */
    public UnmodifiableMap<String, Object> getTags() {
        return tags;
    }

    /**
     * Get requirements script.
     * @return requirements script
     */
    public String getRequirementsScript() {
        return requirementsScript;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.priority);
        hash = 67 * hash + Objects.hashCode(this.parents);
        hash = 67 * hash + Objects.hashCode(this.tags);
        hash = 67 * hash + Objects.hashCode(this.requirementsScript);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Work other = (Work) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.requirementsScript, other.requirementsScript)) {
            return false;
        }
        if (!Objects.equals(this.priority, other.priority)) {
            return false;
        }
        if (!Objects.equals(this.parents, other.parents)) {
            return false;
        }
        if (!Objects.equals(this.tags, other.tags)) {
            return false;
        }
        return true;
    }
    
}

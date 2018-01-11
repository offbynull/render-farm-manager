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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.model.common.IdCheckUtils.isCorrectVarId;

public final class Work {
    private final Core core;
    private final UnmodifiableMap<String, Object> tags;
    private final String requirementsScript;

    public Work(Core core, Map<String, Object> tags, String requirementsScript) {
        Validate.notNull(core);
        Validate.notNull(tags);
        Validate.notNull(requirementsScript);
        this.core = core;
        tags.entrySet().forEach(e -> {
            String name = e.getKey();
            Object value = e.getValue();

            Validate.notNull(name);
            Validate.notNull(value);

            isCorrectVarId(name, value.getClass());
        });
        this.tags = (UnmodifiableMap<String, Object>) unmodifiableMap(new HashMap<>(tags));
        this.requirementsScript = requirementsScript;
    }

    /**
     * Get core.
     * @return core
     */
    public Core getCore() {
        return core;
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
}

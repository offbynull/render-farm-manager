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
package com.offbynull.rfm.host.executor;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * Host check result.
 * @author Kasra Faghihi
 */
public final class HostCheckResult {
    private final String system;
    private final boolean swapEnabled;
    private final UnmodifiableList<Mount> mounts;
    private final UnmodifiableList<Processor> processors;
    private final GlobalMemory memory;
    private final UnmodifiableList<String> tasks;

    /**
     * Constructs a {@link HostCheckResult} object.
     * @param system kernel/system information
     * @param swapEnabled if kernel's memory cgroup feature was configured/compiled with swap functionality
     * @param mounts mount information
     * @param processors processor information
     * @param memory memory information
     * @param tasks task IDs
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !mounts.contains(null)},
     * {@code !processors.contains(null)}
     * {@code !tasks.contains(null)}
     * {@code !tasks.contains("")}
     */
    public HostCheckResult(String system, boolean swapEnabled, List<Mount> mounts, List<Processor> processors, GlobalMemory memory,
            List<String> tasks) {
        Validate.notNull(system);
        Validate.notNull(mounts);
        Validate.notNull(processors);
        Validate.notNull(memory);
        Validate.notNull(tasks);
        Validate.noNullElements(mounts);
        Validate.noNullElements(processors);
        Validate.noNullElements(tasks);
        Validate.isTrue(!tasks.contains(""));
        this.system = system;
        this.swapEnabled = swapEnabled;
        this.mounts = (UnmodifiableList<Mount>) unmodifiableList(new ArrayList<>(mounts));
        this.processors = (UnmodifiableList<Processor>) unmodifiableList(new ArrayList<>(processors));
        this.memory = memory;
        this.tasks = (UnmodifiableList<String>) unmodifiableList(new ArrayList<>(tasks));
    }

    /**
     * Get system information.
     * @return kernel/system information
     */
    public String getSystem() {
        return system;
    }

    /**
     * Get if kernel's memory cgroup feature was compiled/configured with swap functionality. The configuration option for this is
     * {@code CONFIG_MEMCG_SWAP_ENABLED}.
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public boolean isSwapEnabled() {
        return swapEnabled;
    }

    /**
     * Get mounts.
     * @return mount information
     */
    public UnmodifiableList<Mount> getMounts() {
        return mounts;
    }

    /**
     * Get processors.
     * @return processors information
     */
    public UnmodifiableList<Processor> getProcessors() {
        return processors;
    }

    /**
     * Get memory information.
     * @return memory information
     */
    public GlobalMemory getMemory() {
        return memory;
    }

    /**
     * Get task IDs.
     * @return task IDs
     */
    public UnmodifiableList<String> getTasks() {
        return tasks;
    }

    @Override
    public String toString() {
        return "HostCheckResult{" + "system=" + system + ", swapEnabled=" + swapEnabled + ", mounts=" + mounts
                + ", processors=" + processors + ", memory=" + memory + ", tasks=" + tasks + '}';
    }
    
}

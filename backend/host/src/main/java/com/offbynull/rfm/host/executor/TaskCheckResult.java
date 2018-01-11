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

import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

/**
 * Work check result.
 * @author Kasra Faghihi
 */
public final class TaskCheckResult {
    private final TaskState state;
    private final TaskConfiguration configuration;
    private final TaskResourceAllocation resourceAllocation;
    private final Integer sid;
    private final UnmodifiableSet<Integer> pids;
    private final Integer exitCode;
    private final Mount disk;
    private final TaskMemory memory;
    

    /**
     * Constructs a {@link TaskCheckResult} object.
     * @param state task state
     * @param configuration task configuration
     * @param resourceAllocation task resource allocation ({@code null} if no resources allocated yet)
     * @param sid session ID ({@code null} if session no longer running)
     * @param pids process IDs under session ID ({@code null} if session no longer running)
     * @param exitCode exit code ({@code null} if still running or didn't exit normally)
     * @param mount mount information ({@code null} if not mounted)
     * @param memory memory information ({@code null} if memory restriction not set up)
     * @throws NullPointerException if {@code state} or {@code config} is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code !pids.contains(null)},
     * {@code state == CREATED ? resourceAllocation == null : resourceAllocation != null},
     */
    public TaskCheckResult(TaskState state, TaskConfiguration configuration, TaskResourceAllocation resourceAllocation, Integer sid,
            Collection<Integer> pids, Integer exitCode, Mount mount, TaskMemory memory) {
        Validate.notNull(state);
        Validate.notNull(configuration);

        if (pids != null) {
            Validate.noNullElements(pids);
        }
        
        Validate.isTrue(state == TaskState.CREATED ? resourceAllocation == null : resourceAllocation != null);

        this.state = state;
        this.configuration = configuration;
        this.resourceAllocation = resourceAllocation;
        this.sid = sid;
        this.pids = pids == null ? null : (UnmodifiableSet<Integer>) unmodifiableSet(new HashSet<>(pids));
        this.exitCode = exitCode;
        this.disk = mount;
        this.memory = memory;
    }

    /**
     * Get task state.
     * @return task state
     */
    public TaskState getState() {
        return state;
    }

    /**
     * Get task configuration.
     * @return task configuration
     */
    public TaskConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get task resource allocation.
     * @return task resource allocation, or {@code null} if no resources allocated
     */
    public TaskResourceAllocation getResourceAllocation() {
        return resourceAllocation;
    }

    /**
     * Get session ID.
     * @return SID, or {@code null} if session no longer running
     */
    public Integer getSid() {
        return sid;
    }

    /**
     * Get process IDs.
     * @return PIDs, or {@code null} if session no longer running
     */
    public UnmodifiableSet<Integer> getPids() {
        return pids;
    }

    /**
     * Get exit code.
     * @return exit code, or {@code null} if process hasn't exited or didn't exit normally
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Get mount information.
     * @return mount information, or {@code null} if not mounted
     */
    public Mount getDisk() {
        return disk;
    }

    /**
     * Get memory information.
     * @return memory information, or {@code null} if memory restriction/monitoring not set up
     */
    public TaskMemory getMemory() {
        return memory;
    }

    @Override
    public String toString() {
        return "TaskCheckResult{" + "configuration=" + configuration + ", resourceAllocation=" + resourceAllocation + ", sid=" + sid
                + ", pids=" + pids + ", exitCode=" + exitCode + ", disk=" + disk + ", memory=" + memory + '}';
    }

}

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
package com.offbynull.rfm.host.communicator;

/**
 * Linux command result.
 * @author Kasra Faghihi
 */
public class ExecuteResult {

    private final int exitCode;
    private final long bootTime;

    /**
     * Constructs a {@link ExecuteResult} object.
     * @param exitCode command exit code
     * @param bootTime time host was booted (seconds since Unix epoch)
     */
    public ExecuteResult(int exitCode, long bootTime) {
        this.bootTime = bootTime;
        this.exitCode = exitCode;
    }

    /**
     * Get exit code.
     * @return exit code
     */
    public final int getExitCode() {
        return exitCode;
    }

    /**
     * Get boot time.
     * @return boot time (seconds since Unix epoch)
     */
    public final long getBootTime() {
        return bootTime;
    }

    @Override
    public String toString() {
        return "ExecuteResult{" + "exitCode=" + exitCode + ", bootTime=" + bootTime + '}';
    }
}

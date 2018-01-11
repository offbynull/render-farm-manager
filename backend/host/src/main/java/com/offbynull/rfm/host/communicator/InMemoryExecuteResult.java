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

import org.apache.commons.lang3.Validate;

/**
 * Linux command result where stdout and stderr are buffered in memory.
 * @author Kasra Faghihi
 */
public final class InMemoryExecuteResult extends ExecuteResult {

    private final String stdout;
    private final String stderr;

    /**
     * Constructs a {@link InMemoryExecuteResult} object.
     * @param stdout command stdout
     * @param stderr command stderr
     * @param exitCode command exit code
     * @param bootTime time host was booted (seconds since Unix epoch)
     * @throws NullPointerException if any argument is {@code null}
     */
    public InMemoryExecuteResult(String stdout, String stderr, int exitCode, long bootTime) {
        super(exitCode, bootTime);

        Validate.notNull(stdout);
        Validate.notNull(stderr);

        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Get stdout.
     * @return stdout
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Get stderr.
     * @return stderr
     */
    public String getStderr() {
        return stderr;
    }

    @Override
    public String toString() {
        return "InMemoryExecuteResult{" + "super=" + super.toString() + "stdout=" + stdout + ", stderr=" + stderr + '}';
    }
}

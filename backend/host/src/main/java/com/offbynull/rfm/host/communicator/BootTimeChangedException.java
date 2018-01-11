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

import java.io.IOException;

/**
 * Exception that indicates the boot time is different than the one the operation expected.
 * @author Kasra Faghihi
 */
public final class BootTimeChangedException extends IOException {

    private final long bootTime;

    /**
     * Constructs a {@link BootTimeChangedException} instance.
     * @param bootTime boot time of the host (seconds since unix epoch)
     */
    public BootTimeChangedException(long bootTime) {
        super("Boot time changed: " + bootTime);
        this.bootTime = bootTime;
    }

    /**
     * Get boot time.
     * @return boot time (seconds since unix epoch)
     */
    public long getBootTime() {
        return bootTime;
    }

}

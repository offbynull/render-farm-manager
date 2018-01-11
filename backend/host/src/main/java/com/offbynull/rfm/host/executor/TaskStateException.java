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

import java.io.IOException;

/**
 * Exception that indicates the task state was different from that which was expected.
 * @author Kasra Faghihi
 */
public final class TaskStateException extends IOException {

    /**
     * Constructs a {@link TaskStateException} instance.
     */
    public TaskStateException() {
    }

    /**
     * Constructs a {@link TaskStateException} instance.
     * @param cause cause
     */
    public TaskStateException(Throwable cause) {
        super(cause);
    }

}

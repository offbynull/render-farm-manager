/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.rfm.gateways.ssh;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * Request a command to run on a SSH-enabled Linux host.
 * @author Kasra Faghihi
 */
public final class SshRequestMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String username;
    private final String password;
    private final String address;
    private final int port;
    private final String script;
    private final long timeout;

    /**
     * Constructs a {@link SshRequestMessage} object.
     * @param username username for host
     * @param password password for host
     * @param address address of host (domain or IP)
     * @param port port of host
     * @param script bash script to run on host
     * @param timeout maximum duration for script to finish running (in milliseconds)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port} isn't a valid port or {@code timeout} is negative
     */
    public SshRequestMessage(String username, String password, String address, int port, String script, long timeout) {
        Validate.notNull(username);
        Validate.notNull(password);
        Validate.notNull(address);
        Validate.notNull(script);
        Validate.isTrue(port > 0 && port <= 65535);
        Validate.isTrue(timeout >= 0L);

        this.username = username;
        this.password = password;
        this.address = address;
        this.port = port;
        this.script = script;
        this.timeout = timeout;
    }

    /**
     * Get username for host.
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get password for host.
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get address for host.
     * @return address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get port for host.
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get bash script to run on host.
     * @return host
     */
    public String getScript() {
        return script;
    }

    /**
     * Get maximum duration for script to finish running (in milliseconds).
     * @return timeout
     */
    public long getTimeout() {
        return timeout;
    }
    
}

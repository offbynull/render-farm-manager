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
package com.offbynull.rfm.host.communicators.sshj;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * SSH host.
 * @author Kasra Faghihi
 */
public final class SshHost {
    private final String host;
    private final int port;
    private final UnmodifiableList<String> fingerprints;

    /**
     * Constructs a {@link SshHost} object.
     * @param host SSH host
     * @param port SSH port
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code host.isEmpty()} or if {@code 1 > port > 65535}
     */
    public SshHost(String host, int port) {
        this(host, port, null);
    }

    /**
     * Constructs a {@link SshHost} object.
     * @param host SSH host
     * @param port SSH port
     * @param fingerprints SSH host fingerprints (ssh-keygen format e.g. {@code SHA256:ssyMcxeRiaGaPQ6DjGDck+gJw0C2Mo2Pt3FtIltdgM8})
     * @throws NullPointerException if any argument other than {@code fingerprints} is {@code null}
     * @throws IllegalArgumentException if {@code host.isEmpty()}, or if {@code 1 > port > 65535}, or if {@code fingerprints} contains any
     * {@code null} elements
     */
    public SshHost(String host, int port, Collection<String> fingerprints) {
        Validate.notNull(host);
        Validate.notBlank(host);
        Validate.isTrue(port >= 1 && port <= 65535);

        this.host = host;
        this.port = port;
        if (fingerprints != null) {
            Validate.noNullElements(fingerprints);
            this.fingerprints = (UnmodifiableList<String>) unmodifiableList(new ArrayList<>(fingerprints));
        } else {
            this.fingerprints = null;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public UnmodifiableList<String> getFingerprints() {
        return fingerprints;
    }

    @Override
    public String toString() {
        return "SshHost{" + "host=" + host + ", port=" + port + ", fingerprints=" + fingerprints + '}';
    }
    
}

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

import org.apache.commons.lang3.Validate;

/**
 * SSH credentials.
 * @author Kasra Faghihi
 */
public final class SshCredential {
    private final String user;
    private final String password;
    private final SshPublicPrivateKey key;

    /**
     * Construct a {@link SshCredential} object.
     * @param user user
     * @param password password
     * @throws NullPointerException if any argument is {@code null}
     */
    public SshCredential(String user, String password) {
        this(user, password, null);
    }

    /**
     * Construct a {@link SshCredential} object.
     * @param user user
     * @param key public/private key
     * @throws NullPointerException if any argument is {@code null}
     */
    public SshCredential(String user, SshPublicPrivateKey key) {
        this(user, null, key);
    }

    /**
     * Construct a {@link SshCredential} object.
     * @param user user
     * @param password password
     * @param key public-private key
     * @throws NullPointerException if either {@code user} is {@code null}, or if both {@code password} and {@code key} are null}
     */
    public SshCredential(String user, String password, SshPublicPrivateKey key) {
        Validate.notNull(user);
        Validate.isTrue(password != null || key != null, "Require either password or key");

        this.user = user;
        this.password = password;
        this.key = key;
    }

    /**
     * Get user.
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * Get password
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get key.
     * @return public-private key
     */
    public SshPublicPrivateKey getKey() {
        return key;
    }   

    @Override
    public String toString() {
        return "SshCredential{" + "user=" + user + ", password=REDACTED, key=REDACTED}";
    }
}

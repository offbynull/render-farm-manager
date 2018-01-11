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
 * SSH public-private key.
 * @author Kasra Faghihi
 */
public final class SshPublicPrivateKey {
    private final String privateKey;
    private final String publicKey;
    private final String password; // private key password

    /**
     * Construct a {@link SshPublicPrivateKey} object.
     * @param privateKey private key (ssh-keygen private key file format
     * e.g. {@code ssh-rsa AAAAB3N...TQ==})
     * @param publicKey public key (ssh-keygen public key file format
     * e.g. {@code -----BEGIN RSA PRIVATE KEY-----\nYmJoA...Vm35qwFp\n-----END RSA PRIVATE KEY-----})
     * @param password password for private key
     * @throws NullPointerException if any argument is {@code null}
     */
    public SshPublicPrivateKey(String privateKey, String publicKey, String password) {
        Validate.notNull(privateKey);
        Validate.notNull(publicKey);
        Validate.notNull(password);

        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.password = password;
    }

    /**
     * Get private key.
     * @return private key (ssh-keygen private key file format)
     */
    public String getPrivateKey() {
        return privateKey;
    }

    /**
     * Get public key.
     * @return public key (ssh-keygen public key file format)
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Get password for private key.
     * @return password for private key
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "SshPublicPrivateKey{privateKey=REDACTED, publicKey=REDACTED, password=REDACTED}";
    }
    
}

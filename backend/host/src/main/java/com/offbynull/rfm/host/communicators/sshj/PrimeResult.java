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

import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class PrimeResult {
    private final UnmodifiableSet<String> fingerprints;
    private final String privateKey;
    private final String publicKey;
    private final String keyPassword;

    public PrimeResult(Collection<String> fingerprints, String privateKey, String publicKey, String keyPassword) {
        Validate.notNull(fingerprints);
        Validate.notNull(privateKey);
        Validate.notNull(publicKey);
        Validate.notNull(keyPassword);
        Validate.noNullElements(fingerprints);
        this.fingerprints = (UnmodifiableSet<String>) unmodifiableSet(new HashSet<>(fingerprints));
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.keyPassword = keyPassword;
    }

    public UnmodifiableSet<String> getFingerprints() {
        return fingerprints;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    @Override
    public String toString() {
        return "PrimeResult{" + "fingerprints=" + fingerprints + ", privateKey=" + privateKey + ", publicKey=" + publicKey
                + ", keyPassword=" + keyPassword + '}';
    }
    
}

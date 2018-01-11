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

import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.apache.commons.lang3.Validate;

final class CustomPasswordFinder implements PasswordFinder {

    private final String password;

    public CustomPasswordFinder(String password) {
        Validate.notNull(password);
        this.password = password;
    }

    @Override
    public char[] reqPassword(Resource<?> resource) {
        return password.toCharArray();
    }

    @Override
    public boolean shouldRetry(Resource<?> resource) {
        return false;
    }
}

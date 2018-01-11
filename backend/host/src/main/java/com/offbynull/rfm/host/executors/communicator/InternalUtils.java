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
package com.offbynull.rfm.host.executors.communicator;

import org.apache.commons.lang3.Validate;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }

    static Long toLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
    
    static void validatePath(String path) {
        Validate.notNull(path);
        Validate.notBlank(path);
        Validate.isTrue(path.startsWith("/"), "Work path must be absolute");
        Validate.isTrue(!path.endsWith("/"), "Work path must not end with /");
        Validate.isTrue(!path.contains("/../"), "Work path must not traverse up");
        Validate.isTrue(!path.contains("/./"), "Work path must not self-reference");
        Validate.isTrue(!path.contains("\u0000"), "Work path must not contain NUL"); //https://serverfault.com/a/150744
    }
}

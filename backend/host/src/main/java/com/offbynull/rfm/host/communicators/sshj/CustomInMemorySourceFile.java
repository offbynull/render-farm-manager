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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.apache.commons.lang3.Validate;

final class CustomInMemorySourceFile extends InMemorySourceFile {
    private final String name;
    private final byte[] data;

    public CustomInMemorySourceFile(String name, byte[] data) {
        Validate.notNull(name);
        Validate.notNull(data);
        Validate.isTrue(!name.isEmpty());
        this.name = name;
        this.data = Arrays.copyOf(data, data.length);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getLength() {
        return data.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }
}

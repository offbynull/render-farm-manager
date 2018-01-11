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

import java.io.IOException;
import java.io.OutputStream;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.LocalDestFile;
import org.apache.commons.io.output.ByteArrayOutputStream;

final class CustomInMemoryDestFile extends InMemoryDestFile {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return baos;
    }

    public byte[] toByteArray() {
        return baos.toByteArray();
    }

    @Override
    public LocalDestFile getTargetDirectory(String dirname) throws IOException {
        throw new IOException("Attempting to download directory?");
    }

    @Override
    public LocalDestFile getChild(String name) {
        throw new IllegalStateException("Attempting to download directory?"); // shouldn't happen?
    }
    
}

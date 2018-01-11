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
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.lang3.Validate;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }

    static String readLine(Reader reader) throws IOException {
        Validate.notNull(reader);

        StringBuilder builder = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n') {
                break;
            }
            builder.append((char) ch);
        }
        return builder.toString();
    }

    static void writeLine(Writer writer, String line) throws IOException {
        Validate.notNull(writer);
        Validate.notNull(line);
        
        writer.write(line);
        writer.write('\n');
        writer.flush();
    }
}

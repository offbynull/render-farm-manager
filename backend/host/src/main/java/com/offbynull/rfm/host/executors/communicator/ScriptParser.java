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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class ScriptParser {
    private ScriptParser() {
        // do nothing
    }

    public static Map<String, String> parse(String str) {
        Validate.notNull(str);

        Map<String, String> ret = new HashMap<>();
        
        try (StringReader sr = new StringReader(str);
                BufferedReader br = new BufferedReader(sr)) {
            String markerStr;
            while (true) {
                markerStr = br.readLine();
                if (markerStr == null) {
                    break;
                }

                if (!markerStr.startsWith("!")) {
                    throw new IllegalArgumentException();
                }

                String lengthStr = br.readLine();

                long length = Long.parseLong(lengthStr);
                StringBuilder outputStr = new StringBuilder();
                for (long i = 0; i < length; i++) {
                    String line = br.readLine();

                    Validate.isTrue(line != null);

                    outputStr.append(line);
                    if (i < length - 1) {
                        outputStr.append('\n');
                    }
                }

                Validate.isTrue(!ret.containsKey(markerStr), "Duplicate entry found for %s", markerStr);
                ret.put(markerStr, outputStr.toString());
            }
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException ioe) { // should never happen
            throw new IllegalStateException(ioe);
        }
        
        return ret;
    }
}

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
package com.offbynull.rfm.host.model.common;

import static com.offbynull.rfm.host.model.expression.DataType.BOOLEAN;
import static com.offbynull.rfm.host.model.expression.DataType.NUMBER;
import static com.offbynull.rfm.host.model.expression.DataType.STRING;
import static java.lang.String.format;
import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;

/**
 * Utility class for checking IDs.
 * @author Kasra Faghihi
 */
public final class IdCheckUtils {
    private IdCheckUtils() {
        // do nothing
    }
    
    
    
    
    /**
     * Validate that an identifier is correct.
     * @param id ID
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code id.length() > 0},
     * {@code id.chars().allMatch(ch -> (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_')},
     */
    public static void isCorrectId(String id) {
        Validate.notNull(id);
        Validate.isTrue(id.length() > 0, "Empty name not allowed");
        Validate.isTrue(id.chars().allMatch(ch -> (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_'),
                "ID must can only contain the character ranges a-z, 0-9, and _");
    }

    /**
     * Validate that a variable identifier is correct and matches the given type.
     * @param id variable identifier
     * @param type variable type
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code id.length() > 2},
     * {@code id.chars().allMatch(ch -> (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_')},
     * {@code id.charAt(0) == 'b' || id.charAt(0) == 'n' || id.charAt(0) == 's'},
     * {@code id.charAt(1) == '_'}
     * {@code if (type == Boolean.class) id.charAt(0) == 'b' }
     * {@code if (type == BigDecimal.class) id.charAt(0) == 'n' }
     * {@code if (type == String.class) id.charAt(0) == 's' }
     */
    public static void isCorrectVarId(String id, Class<?> type) {
        Validate.notNull(id);
        Validate.notNull(type);
        isCorrectId(id);
        Validate.isTrue(id.length() > 2,
                "Variable ID %s not allowed -- must start with [bns] followed by an underscore", id);
        switch (id.charAt(0)) {
            case 'b':
                Validate.isTrue(type == Boolean.class,
                        "Variable ID %s not allowed -- expected type %s but evaluates to %s", id, type, BOOLEAN);
                break;
            case 'n':
                Validate.isTrue(type == BigDecimal.class,
                        "%Variable ID s not allowed -- expected type %s but evaluates to %s", id, type, NUMBER);
                break;
            case 's':
                Validate.isTrue(type == String.class,
                        "%Variable ID s not allowed -- expected type %s but evaluates to %s", id, type, STRING);
                break;
            default:
                throw new IllegalArgumentException(
                        format("Variable ID %s not allowed -- must start with [bns] followed by an underscore", id));
        }
        Validate.isTrue(id.charAt(1) == '_', "Variable ID %s not allowed -- expected _ at second character position", id);
    }

    /**
     * Validate that a variable identifier is correct.
     * @param id variable identifier
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any of the following conditions are NOT met:
     * {@code id.length() > 2},
     * {@code id.chars().allMatch(ch -> (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_')},
     * {@code id.charAt(0) == 'b' || id.charAt(0) == 'n' || id.charAt(0) == 's'},
     * {@code id.charAt(1) == '_'}
     */
    public static void isCorrectVarId(String id) {
        Validate.notNull(id);
        isCorrectId(id);
        Validate.isTrue(id.length() > 2,
                "Variable ID %s not allowed -- must start with [bns] followed by an underscore", id);        
        switch (id.charAt(0)) {
            case 'b':
            case 'n':
            case 's':
                break;
            default:
                throw new IllegalArgumentException(
                        format("Variable ID %s not allowed -- must start with [bns] followed by an underscore", id));
        }
        Validate.isTrue(id.charAt(1) == '_', "Variable ID %s not allowed -- expected _ at second character position", id);
    }
}

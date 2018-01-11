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
package com.offbynull.rfm.host.model.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }
    
    public static String getParserRuleText(ParserRuleContext rc) {
                Token startToken = rc.getStart();
                Token stopToken = rc.getStop();

                int startCharIdx = startToken.getStartIndex();
                int stopCharIdx = stopToken.getStopIndex();

                Interval charInterval = Interval.of(startCharIdx, stopCharIdx);
                CharStream charStream = startToken.getInputStream();
                
                return charStream.getText(charInterval);
    }

}

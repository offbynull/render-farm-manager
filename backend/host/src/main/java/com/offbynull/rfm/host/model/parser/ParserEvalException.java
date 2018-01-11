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

import static com.offbynull.rfm.host.model.parser.InternalUtils.getParserRuleText;
import static java.lang.String.format;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;

public final class ParserEvalException extends IllegalArgumentException {
    ParserEvalException(ParserRuleContext ctx, String formatMsg, Object... values) {
        super(format("line %d char %d\n%s\n%s",
                ctx.start.getLine(),
                ctx.start.getCharPositionInLine(),
                format(formatMsg, values),
                getParserRuleText(ctx)));
    }
    
    ParserEvalException(ParserRuleContext ctx, Exception backingException, String formatMsg, Object... values) {
        super(format("line %d char %d\n%s\n%s",
                ctx.start.getLine(),
                ctx.start.getCharPositionInLine(),
                format(formatMsg, values),
                getParserRuleText(ctx)), backingException);
    }
    
    ParserEvalException(int lineNum, int charPosInLine, String msg, RecognitionException re) {
//        "line " + line + ":" + charPositionInLine + " " + msg
        super(format("line %d char %d\n%s\n%s",
                lineNum,
                charPosInLine,
                msg,
                re.getCtx()
        ), re);
    }
}

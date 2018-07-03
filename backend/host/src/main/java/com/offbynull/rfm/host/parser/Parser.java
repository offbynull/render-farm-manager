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
package com.offbynull.rfm.host.parser;

import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.service.Work;
import com.offbynull.rfm.host.parser.antlr.EvalLexer;
import com.offbynull.rfm.host.parser.antlr.EvalParser;
import static com.offbynull.rfm.host.parser.antlr.EvalParser.VOCABULARY;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import static java.lang.String.format;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class Parser {
    
    private final UnmodifiableList<RequirementFunction> requirementFunctions;
    private final UnmodifiableList<TagFunction> tagFunctions;

    /**
     * Constructs a {@link Parser} object.
     * @param requirementFunctions function signatures allowed in requirement expressions
     * @param tagFunctions function function invocation points allowed in tag expressions
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public Parser(Collection<RequirementFunction> requirementFunctions, Collection<TagFunction> tagFunctions) {
        Validate.notNull(requirementFunctions);
        Validate.notNull(tagFunctions);
        Validate.noNullElements(requirementFunctions);
        Validate.noNullElements(tagFunctions);

        List<RequirementFunction> allFunctions = new ArrayList<>(requirementFunctions);
        
        FieldUtils.getAllFieldsList(RequirementFunctionBuiltIns.class).stream()
                .filter(f -> (f.getModifiers() & Modifier.STATIC) == Modifier.STATIC)
                .filter(f -> f.isAccessible())
                .filter(f -> f.getDeclaringClass() == RequirementFunctionBuiltIns.class)
                .filter(f -> f.getType() == RequirementFunction.class)
                .forEach(f -> {
                    try {
                        RequirementFunction builtin = (RequirementFunction) FieldUtils.readStaticField(f);
                        allFunctions.add(builtin);
                    } catch (IllegalAccessException iae) {
                        throw new IllegalStateException(iae); // should never happen
                    }
                });
        
        this.requirementFunctions = (UnmodifiableList<RequirementFunction>) unmodifiableList(new ArrayList<>(requirementFunctions));
        this.tagFunctions = (UnmodifiableList<TagFunction>) unmodifiableList(new ArrayList<>(tagFunctions));
        
        Set<String> reqFunctionLookup = new HashSet<>();
        requirementFunctions.forEach(f -> {
            String name = f.getName();
            Validate.isTrue(reqFunctionLookup.add(name), "Duplicate requirement function definition: %s", name);
        });
        
        Set<String> tagFunctionLookup = new HashSet<>();
        tagFunctions.forEach(f -> {
            String name = f.getName();
            Validate.isTrue(tagFunctionLookup.add(name), "Duplicate tag function definition: %s", f.getName());
        });
    }

    /**
     * Parse a list of requirements as an AST. This validates the entire script (including requirements), but only returns the parsed core
     * and tag portions. To parse the requirements, pass the requirements portion into
     * {@link #parseScriptReqs(java.util.Map, java.lang.String) }.
     * @param input requirements to parser
     * @return ASTs generated for {@code input}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code input} isn't valid (cannot be parsed)
     */
    public Work parseScript(String input) {
        Validate.notNull(input);

        EvalParser parser = initParser(input);
        ParserEvalVisitor visitor = new ParserEvalVisitor(requirementFunctions, tagFunctions);
        
        try {
            return (Work) visitor.visit(parser.main());
        } catch (ParseCancellationException pce) { // throws by BailErrorStrategy
            RecognitionException re = (RecognitionException) pce.getCause();
            throw new IllegalArgumentException(
                    format("Parser syntax error: token [%s] but expected one of [%s] (line %d pos %d)",
                            re.getOffendingToken() == null ? "" : VOCABULARY.getDisplayName(re.getOffendingToken().getType()),
                            re.getExpectedTokens().toList().stream().map(i -> VOCABULARY.getDisplayName(i)).collect(joining(",")),
                            re.getOffendingToken().getLine(),
                            re.getOffendingToken().getCharPositionInLine()),
                    re);
        } catch (ParserEvalException e) {
            throw new IllegalArgumentException("Parser semantic error: " + e.toString(), e);
        }
    }

    public HostRequirement parseScriptReqs(Map<String, Object> tags, String input) {
        Validate.notNull(tags); // contents of tags are verified in visitor.populateTagCache(), so don't worry about it here
        Validate.notNull(input);

        EvalParser parser = initParser(input);
        ParserEvalVisitor visitor = new ParserEvalVisitor(requirementFunctions, tagFunctions);
        
        try {
            visitor.populateTagCache(tags);
            return (HostRequirement) visitor.visit(parser.reqEntry());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private EvalParser initParser(String input) {
        input = input.trim();

        CharStream charStream = CharStreams.fromString(input);
        
        EvalLexer lexer = new EvalLexer(charStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EvalParser parser = new EvalParser(tokenStream);
        
        parser.setErrorHandler(new BailErrorStrategy());
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol,
                    int line,
                    int charPositionInLine,
                    String msg,
                    RecognitionException e) {
                throw new ParserEvalException(line, charPositionInLine, msg, e);
            }
        });
        
        return parser;
    }
}

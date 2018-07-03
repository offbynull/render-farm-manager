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
import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.DataType;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.model.expression.BooleanLiteralExpression;
import com.offbynull.rfm.host.model.expression.NumberLiteralExpression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.StringLiteralExpression;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.common.IdCheckUtils;
import static com.offbynull.rfm.host.parser.InternalUtils.getParserRuleText;
import static com.offbynull.rfm.host.model.expression.DataType.BOOLEAN;
import static com.offbynull.rfm.host.model.expression.DataType.NUMBER;
import static com.offbynull.rfm.host.model.expression.DataType.STRING;
import com.offbynull.rfm.host.parser.antlr.EvalBaseVisitor;
import com.offbynull.rfm.host.parser.antlr.EvalParser;
import com.offbynull.rfm.host.parser.antlr.EvalParser.NumberRangeContext;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.service.Work;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringEscapeUtils;
import static java.math.BigDecimal.ZERO;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.ImmutablePair;

final class ParserEvalVisitor extends EvalBaseVisitor<Object> {

    private static final int MAX_PRECISION = 38;
    private static final int MAX_SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.DOWN;
    
    private final UnmodifiableMap<String, RequirementFunction> deferredFunctions;
    private final UnmodifiableMap<String, TagFunction> immediateFunctions;
    
    private ParseState evalState;
    private final LinkedList<String> reqScope;
    private final Map<String, Object> tagCache;

    ParserEvalVisitor(Collection<RequirementFunction> deferredFunctions, Collection<TagFunction> immediateFunctions) {
        Validate.notNull(deferredFunctions);
        Validate.notNull(immediateFunctions);
        Validate.noNullElements(deferredFunctions);
        Validate.noNullElements(immediateFunctions);
        
        Map<String, RequirementFunction> reqFunctionLookup = new HashMap<>();
        deferredFunctions.forEach(f -> {
            RequirementFunction existing = reqFunctionLookup.put(f.getName(), f);
            Validate.isTrue(existing == null, "Duplicate requirement function definition: %s", f.getName());
        });
        
        Map<String, TagFunction> tagFunctionLookup = new HashMap<>();
        immediateFunctions.forEach(f -> {
            TagFunction existing = tagFunctionLookup.put(f.getName(), f);
            Validate.isTrue(existing == null, "Duplicate tag function definition: %s", f.getName());
        });

        this.deferredFunctions = (UnmodifiableMap<String, RequirementFunction>) unmodifiableMap(reqFunctionLookup);
        this.immediateFunctions = (UnmodifiableMap<String, TagFunction>) unmodifiableMap(tagFunctionLookup);
        this.reqScope = new LinkedList<>();
        this.tagCache = new HashMap<>();
    }

    public void populateTagCache(Map<String, Object> tags) {
        Validate.notNull(tags);
        Validate.noNullElements(tags.keySet());
        tags.entrySet().forEach(e -> IdCheckUtils.isCorrectVarId(e.getKey(), e.getValue().getClass()));

        tagCache.clear();
        tagCache.putAll(tags);
    }
    
    @Override
    public Object visitMain(EvalParser.MainContext ctx) {
        reqScope.clear();
        tagCache.clear();

        // core
        String id = ctx.core().ID(0).getText();
        isCorrectId(ctx, id);
        
        BigDecimal priority = convertNumberToBigDecimal(ctx.core().NUMBER());
        parseIsTrue(ctx.core(), priority.compareTo(ZERO) >= 0 && priority.compareTo(ONE) <= 0, "Priority must be between 0 and 1");

        Set<String> parents = ctx.core().ID().stream()
                .skip(1L) // first one is the ID (so skip it), rest are dependencies
                .map(d -> {
                    String depId = d.getText();
                    
                    isCorrectId(ctx, depId);
                    parseIsTrue(ctx, !depId.equals(id), "ID cannot be in dependencies: %s", id);
                    return depId;
                })
                .collect(toSet());
        
        // tags
        for (EvalParser.TagEntryContext tagCtx : ctx.tagEntry()) {
            ImmutablePair<String, Object> val = (ImmutablePair<String, Object>) visit(tagCtx);
            tagCache.put(val.getKey(), val.getValue());
        }

        // req
        Requirement req = (Requirement) visit(ctx.reqEntry()); // output not used, but this does do validation
        String reqText = getParserRuleText(ctx.reqEntry());
        parseIsTrue(ctx.reqEntry(), req instanceof HostRequirement, "Top-level requirement must be host");
        
        return new Work(id, priority, parents, new HashMap<>(tagCache), reqText);
    }
    
    @Override
    public Object visitTagEntry(EvalParser.TagEntryContext ctx) {
        evalState = ParseState.TAGS;
        
        
        String name = ctx.ID().getText();
        DataType varType = prefixToType(name);

        Object value = visit(ctx.expr());

        DataType valType = classToType(value.getClass());
        parseIsTrue(ctx, varType == valType, "%s not allowed -- expected type %s but evaluates to %s", name, varType, valType);
        
        if (valType == NUMBER) {
            BigDecimal truncatedBd = ((BigDecimal) value).setScale(MAX_SCALE, ROUNDING_MODE);
            parseIsTrue(ctx, truncatedBd.precision() <= MAX_PRECISION, "Tag %s evaluated to a number too high: %s", name, value);
            value = truncatedBd;
        }

        return ImmutablePair.of(name, value);
    }

    @Override
    public Object visitReqEntry(EvalParser.ReqEntryContext ctx) {
        evalState = ParseState.REQUIREMENTS;
        
        
        // Push req name
        String name = ctx.ID().getText();
        reqScope.push(name);
        
        
        
        // Get number range
        NumberRangeContext numberRangeCtx = ctx.numberRange();
        NumberRange numRange;
        if (numberRangeCtx != null) {
            numRange = (NumberRange) visit(ctx.numberRange());
        } else {
            numRange = new NumberRange(ONE, ONE);
        }
        
        
        
        // Evaluate where clause
        Expression expr;
        if (ctx.whereClause() != null) {
            expr = (Expression) visit(ctx.whereClause().expr());
            parseIsTrue(ctx, expr.getType() == BOOLEAN, "Where condition must evaluate to %s, but was %s", BOOLEAN, expr.getType());
        } else {
            expr = new BooleanLiteralExpression(true);
        }
        
        
        
        // Get factory method for requirement
        Method method = stream(InternalRequirementFactory.class.getMethods())
                .filter(m -> m.getName().equals(name))                                  // match name
                .filter(m -> Requirement.class.isAssignableFrom(m.getReturnType()))     // match ret type
                .filter(m -> m.getParameterCount() >= 2)                                // must have >= 2 params
                .filter(m -> m.getParameterTypes()[0] == NumberRange.class)             // param[0] must be numberrange
                .filter(m -> m.getParameterTypes()[1] == Expression.class)              // param[1] must be expression
                .filter(m -> (m.getModifiers() & Modifier.STATIC) != 0)                 // must be static
                .findAny().orElseThrow(() -> new ParserEvalException(ctx, "Unrecognized requirement type: %s", name));
        
        
        
        // Calculate sub-requirements
        Map<Class<?>, List<Requirement>> subReqs = ctx.reqEntry().stream()
                .map(x -> (Requirement) visit(x))
                .collect(groupingBy(x -> x.getClass()));
        
        
        // Invoke factory method
        Class<?>[] paramTypes = method.getParameterTypes();
        List<Object> invokeArgs = new ArrayList<>(paramTypes.length);
        
        LinkedList<Class<?>> expectedReqTypes = new LinkedList<>(asList(paramTypes));
        expectedReqTypes.removeFirst(); // remove numberrange param (not a requirement)
        expectedReqTypes.removeFirst(); // remove expression param (not a requirement)
        
        invokeArgs.add(numRange);      // place in number range
        invokeArgs.add(expr);          // place in where expression
        expectedReqTypes.stream()
                .map(c -> c.getComponentType()) // req args for factory method are always arrays, so get the component type of that array
                .forEachOrdered(c -> {
                    List<Requirement> reqArg = subReqs.getOrDefault(c, Collections.EMPTY_LIST);             // get (or none if non-existant)
                    Object reqArgAsArr = reqArg.stream().toArray(l -> (Object[]) Array.newInstance(c, l));  // to array
                    invokeArgs.add(reqArgAsArr);                                                            // append to invokeArgs
                });

        method.setAccessible(true);
        Requirement ret;
        try {
            ret = (Requirement) method.invoke(null, invokeArgs.toArray());
        } catch (InvocationTargetException ite) {
            throw new ParserEvalException(ctx, ite, "Bad requirement: %s", ite.getCause().getMessage());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException(e); // should never happen
        }
        
        
        
        // Pop req name
        reqScope.pop();
        
        
        
        // Return requirement object
        return ret;
    }

    @Override
    public Object visitWhereClause(EvalParser.WhereClauseContext ctx) {
        Expression ret = (Expression) visit(ctx.expr());
        parseIsTrue(ctx, BOOLEAN == ret.getType(), "Where %s expected type %s but evaluates to %s", ctx.getText(), BOOLEAN, ret.getType());
        return ret;
    }

    @Override
    public Object visitExactRange(EvalParser.ExactRangeContext ctx) {
        BigDecimal value = convertNumberToBigDecimal(ctx.NUMBER());
        
        return new NumberRange(value, value);
    }

    @Override
    public Object visitBetweenRange(EvalParser.BetweenRangeContext ctx) {
        BigDecimal start = convertNumberToBigDecimal(ctx.NUMBER(0));
        BigDecimal end = convertNumberToBigDecimal(ctx.NUMBER(1));
        
        parseIsTrue(ctx, end.compareTo(start) >= 0, "Range start cannot be less than end");
        
        return new NumberRange(start, end);
    }


    @Override
    public Object visitExprBracket(EvalParser.ExprBracketContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitExprNot(EvalParser.ExprNotContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprNot(ctx);
            case REQUIREMENTS:
                return visitReqExprNot(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprNot(EvalParser.ExprNotContext ctx) {
        Object input = (Object) visit(ctx.expr());
        parseIsTrue(ctx, input instanceof Boolean, "Negate operator cannot operate on (%s)", input.getClass());

        return !((Boolean) input);
    }

    private Object visitReqExprNot(EvalParser.ExprNotContext ctx) {
        Expression expr = (Expression) visit(ctx.expr());
        parseIsTrue(ctx, expr.getType() == BOOLEAN, "Negate operator cannot operate on (%s)", expr.getType());

        return new InvocationExpression(RequirementFunctionBuiltIns.NOT_B_B_FUNCTION, expr);
    }

    
    @Override
    public Object visitExprDivMul(EvalParser.ExprDivMulContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprDivMul(ctx);
            case REQUIREMENTS:
                return visitReqExprDivMul(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprDivMul(EvalParser.ExprDivMulContext ctx) {
            Object left = visit(ctx.expr(0));
            Object right = visit(ctx.expr(1));

            parseIsTrue(ctx, left instanceof BigDecimal && right instanceof BigDecimal,
                    "Multiply/divide operator cannot operate on (%s,%s)", left.getClass(), right.getClass());

            switch (ctx.op.getType()) {
                case EvalParser.DIVIDE:
                    return ((BigDecimal) left).divide((BigDecimal) right, MAX_SCALE, ROUNDING_MODE);
                case EvalParser.MULTIPLY:
                    return ((BigDecimal) left).multiply((BigDecimal) right);
                default:
                    throw new IllegalStateException();
            }
    }

    private Object visitReqExprDivMul(EvalParser.ExprDivMulContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx, left.getType() == NUMBER && right.getType() == NUMBER,
                "Multiply/divide operator cannot operate on (%s,%s)", left.getType(), right.getType());

        switch (ctx.op.getType()) {
            case EvalParser.DIVIDE:
                return new InvocationExpression(RequirementFunctionBuiltIns.DIVIDE_N_NN_FUNCTION, left, right);
            case EvalParser.MULTIPLY:
                return new InvocationExpression(RequirementFunctionBuiltIns.MULTIPLY_N_NN_FUNCTION, left, right);
            default:
                throw new IllegalStateException();
        }
    }
    
    
    @Override
    public Object visitExprAddSub(EvalParser.ExprAddSubContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprAddSub(ctx);
            case REQUIREMENTS:
                return visitReqExprAddSub(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprAddSub(EvalParser.ExprAddSubContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));

        parseIsTrue(ctx, left instanceof BigDecimal && right instanceof BigDecimal,
                "Add/subtract operator cannot operate on (%s,%s)", left.getClass(), right.getClass());

        switch (ctx.op.getType()) {
            case EvalParser.ADDITION:
                return ((BigDecimal) left).add((BigDecimal) right);
            case EvalParser.SUBTRACT:
                return ((BigDecimal) left).subtract((BigDecimal) right);
            default:
                throw new IllegalStateException();
        }
    }

    private Object visitReqExprAddSub(EvalParser.ExprAddSubContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx,
                left.getType() == NUMBER && right.getType() == NUMBER,
                "Add/subtract operator cannot operate on (%s,%s)", left.getType(), right.getType());

        switch (ctx.op.getType()) {
            case EvalParser.ADDITION:
                return new InvocationExpression(RequirementFunctionBuiltIns.ADD_N_NN_FUNCTION, left, right);
            case EvalParser.SUBTRACT:
                return new InvocationExpression(RequirementFunctionBuiltIns.SUB_N_NN_FUNCTION, left, right);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }


    @Override
    public Object visitExprRelational(EvalParser.ExprRelationalContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprRelational(ctx);
            case REQUIREMENTS:
                return visitReqExprRelational(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprRelational(EvalParser.ExprRelationalContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));

        parseIsTrue(ctx,
                left instanceof BigDecimal && right instanceof BigDecimal,
                "Relational operators cannot operate on (%s,%s)", left.getClass(), right.getClass());

        switch (ctx.op.getType()) {
            case EvalParser.GREATER_THAN:
                return ((BigDecimal) left).compareTo((BigDecimal) right) > 0;
            case EvalParser.LESS_THAN:
                return ((BigDecimal) left).compareTo((BigDecimal) right) < 0;
            case EvalParser.GREATER_THAN_OR_EQUALS: {
                return ((BigDecimal) left).compareTo((BigDecimal) right) >= 0;
            }
            case EvalParser.LESS_THAN_OR_EQUALS: {
                return ((BigDecimal) left).compareTo((BigDecimal) right) <= 0;
            }
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitReqExprRelational(EvalParser.ExprRelationalContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx,
                left.getType() == NUMBER && right.getType() == NUMBER,
                "Relational operators cannot operate on (%s,%s)", left.getType(), right.getType());

        switch (ctx.op.getType()) {
            case EvalParser.GREATER_THAN:
                return new InvocationExpression(RequirementFunctionBuiltIns.GREATER_THAN_B_NN_FUNCTION, left, right);
            case EvalParser.LESS_THAN:
                return new InvocationExpression(RequirementFunctionBuiltIns.LESS_THAN_B_NN_FUNCTION, left, right);
            case EvalParser.GREATER_THAN_OR_EQUALS: {
                InvocationExpression gtInvoke = new InvocationExpression(RequirementFunctionBuiltIns.GREATER_THAN_B_NN_FUNCTION, left, right);
                InvocationExpression eqInvoke = new InvocationExpression(RequirementFunctionBuiltIns.EQUAL_B_NN_FUNCTION, left, right);
                return new InvocationExpression(RequirementFunctionBuiltIns.OR_B_BB_FUNCTION, gtInvoke, eqInvoke);
            }
            case EvalParser.LESS_THAN_OR_EQUALS: {
                InvocationExpression ltInvoke = new InvocationExpression(RequirementFunctionBuiltIns.LESS_THAN_B_NN_FUNCTION, left, right);
                InvocationExpression eqInvoke = new InvocationExpression(RequirementFunctionBuiltIns.EQUAL_B_NN_FUNCTION, left, right);
                return new InvocationExpression(RequirementFunctionBuiltIns.OR_B_BB_FUNCTION, ltInvoke, eqInvoke);
            }
            default:
                throw new IllegalStateException(); // should never happen
        }
    }


    @Override
    public Object visitExprEquality(EvalParser.ExprEqualityContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprEquality(ctx);
            case REQUIREMENTS:
                return visitReqExprEquality(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprEquality(EvalParser.ExprEqualityContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));

        parseIsTrue(ctx,
                left.getClass() == right.getClass(),
                "Equality operators cannot operate on (%s,%s)", left.getClass(), right.getClass());

        switch (ctx.op.getType()) {
            case EvalParser.EQUALS:
                if (left instanceof BigDecimal) {
                    return ((BigDecimal) left).compareTo((BigDecimal) right) == 0;
                } else {
                    return left.equals(right);
                }
            case EvalParser.NOT_EQUALS: {
                if (left instanceof BigDecimal) {
                    return ((BigDecimal) left).compareTo((BigDecimal) right) != 0;
                } else {
                    return !left.equals(right);
                }
            }
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitReqExprEquality(EvalParser.ExprEqualityContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx,
                left.getType() == right.getType(),
                "Equality operators cannot operate on (%s,%s)", left.getType(), right.getType());

        RequirementFunction eqFunc = null;
        switch (left.getType()) {
            case BOOLEAN:
                eqFunc = RequirementFunctionBuiltIns.EQUAL_B_BB_FUNCTION;
                break;
            case NUMBER:
                eqFunc = RequirementFunctionBuiltIns.EQUAL_B_NN_FUNCTION;
                break;
            case STRING:
                eqFunc = RequirementFunctionBuiltIns.EQUAL_B_SS_FUNCTION;
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }
        switch (ctx.op.getType()) {
            case EvalParser.EQUALS:
                return new InvocationExpression(eqFunc, left, right);
            case EvalParser.NOT_EQUALS: {
                InvocationExpression eqInvoke = new InvocationExpression(eqFunc, left, right);
                return new InvocationExpression(RequirementFunctionBuiltIns.NOT_B_B_FUNCTION, eqInvoke);
            }
            default:
                throw new IllegalStateException(); // should never happen
        }
    }


    @Override
    public Object visitExprAnd(EvalParser.ExprAndContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprAnd(ctx);
            case REQUIREMENTS:
                return visitReqExprAnd(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprAnd(EvalParser.ExprAndContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));

        parseIsTrue(ctx,
                left instanceof Boolean && right instanceof Boolean,
                "And operator cannot operate on (%s,%s)", left.getClass(), right.getClass());

        return ((Boolean) left) && ((Boolean) right);
    }

    private Object visitReqExprAnd(EvalParser.ExprAndContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx, 
                left.getType() == BOOLEAN && right.getType() == BOOLEAN,
                "And operator cannot operate on (%s,%s)", left.getType(), right.getType());

        return new InvocationExpression(RequirementFunctionBuiltIns.AND_B_BB_FUNCTION, left, right);
    }


    @Override
    public Object visitExprOr(EvalParser.ExprOrContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprOr(ctx);
            case REQUIREMENTS:
                return visitReqExprOr(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprOr(EvalParser.ExprOrContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));

        parseIsTrue(ctx,
                left instanceof Boolean && right instanceof Boolean,
                "Or operator cannot operate on (%s,%s)", left.getClass(), right.getClass());

        return ((Boolean) left) || ((Boolean) right);
    }

    private Object visitReqExprOr(EvalParser.ExprOrContext ctx) {
        Expression left = (Expression) visit(ctx.expr(0));
        Expression right = (Expression) visit(ctx.expr(1));

        parseIsTrue(ctx,
                left.getType() == BOOLEAN && right.getType() == BOOLEAN,
                "Or operator cannot operate on (%s,%s)", left.getType(), right.getType());

        return new InvocationExpression(RequirementFunctionBuiltIns.OR_B_BB_FUNCTION, left, right);
    }


    @Override
    public Object visitExprBoolean(EvalParser.ExprBooleanContext ctx) {
        String valueStr = ctx.BOOLEAN().getText();
        boolean value;
        switch (valueStr) {
            case "true":
                value = true;
                break;
            case "false":
                value = false;
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }

        switch (evalState) {
            case TAGS:
                return value;
            case REQUIREMENTS:
                return new BooleanLiteralExpression(value);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }


    @Override
    public Object visitExprNumber(EvalParser.ExprNumberContext ctx) {
        BigDecimal value = convertNumberToBigDecimal(ctx.NUMBER());

        switch (evalState) {
            case TAGS:
                return value;
            case REQUIREMENTS:
                return new NumberLiteralExpression(value);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }


    @Override
    public Object visitExprString(EvalParser.ExprStringContext ctx) {
        String value = ctx.STRING().getText();
        value = StringUtils.substring(value, 1, -1); // remove quotes
        value = StringEscapeUtils.unescapeJava(value); // unescape

        switch (evalState) {
            case TAGS:
                return value;
            case REQUIREMENTS:
                return new StringLiteralExpression(value);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    @Override
    public Object visitExprFunction(EvalParser.ExprFunctionContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprFunction(ctx);
            case REQUIREMENTS:
                return visitReqExprFunction(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprFunction(EvalParser.ExprFunctionContext ctx) {
        String name = ctx.ID().getText();
        List<Object> args = ctx.expr().stream()
                .map(exprCtx -> visit(exprCtx))
                .collect(toList());

        TagFunction func = immediateFunctions.get(name);
        parseIsTrue(ctx, func != null, "Unrecognized function %s", name);

        return func.getInvocationPoint().apply(args);
    }

    private Object visitReqExprFunction(EvalParser.ExprFunctionContext ctx) {
        String name = ctx.ID().getText();
        List<Expression> args = ctx.expr().stream()
                .map(exprCtx -> (Expression) visit(exprCtx))
                .collect(toList());

        RequirementFunction func = deferredFunctions.get(name);
        parseIsTrue(ctx, func != null, "Unrecognized function %s", name);

        List<DataType> actualArgTypes = args.stream().map(e -> e.getType()).collect(toList());
        List<DataType> expectedArgTypes = func.getParameterTypes();
        parseIsTrue(ctx, expectedArgTypes.equals(actualArgTypes), "Bad arguments for %s (%s vs %s)",
                name, expectedArgTypes, actualArgTypes);

        return new InvocationExpression(func, args);
    }


    @Override
    public Object visitExprVariable(EvalParser.ExprVariableContext ctx) {
        switch (evalState) {
            case TAGS:
                return visitTagExprVariable(ctx);
            case REQUIREMENTS:
                return visitReqExprVariable(ctx);
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    private Object visitTagExprVariable(EvalParser.ExprVariableContext ctx) {
        parseIsTrue(ctx, ctx.ID().size() == 1, "Variables referenced in tags cannot be scoped -- they can only reference other tags");
        String name = ctx.ID(0).getText();
        Object val = tagCache.get(name);
        
        parseIsTrue(ctx, val != null, "Variables referenced in tags must reference other tags -- no previously defined tag found");
        
        return val;
    }
    
    private Object visitReqExprVariable(EvalParser.ExprVariableContext ctx) {
        List<String> varChain = ctx.ID().stream()
                .map(x -> x.getText())
                .collect(toList());
        
        String varScope = varChain.get(0);

        String varName = varChain.get(1);
        isCorrectVarName(ctx, varName);
        
        if (varScope == null) { // no scope? it's a tag -- grab the tag value and return it as a literal
            Object val = tagCache.get(varName);
            parseIsTrue(ctx, val != null, "Variables referenced in tags must reference other tags -- no previously defined tag found");

            if (val instanceof Boolean) {
                return new BooleanLiteralExpression((Boolean) val);
            } else if (val instanceof Number) {
                return new NumberLiteralExpression((BigDecimal) val);
            } else if (val instanceof String) {
                return new StringLiteralExpression((String) val);
            }

            throw new IllegalStateException(); // should never happen?
        } else { // yes scope? it's referencing the req or one of its parents -- return it as a variable expr
            parseIsTrue(ctx, reqScope.contains(varScope), "Scope not available: %s vs %s", varScope, reqScope.toString());

            return new VariableExpression(varScope, varName); 
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    private BigDecimal convertNumberToBigDecimal(TerminalNode node) {
        String value = node.getText();
        
        if (value.charAt(value.length() - 1) == 'b') {
            value = value.substring(0, value.length() - 1);
        }

        BigDecimal multiplier = BigDecimal.ONE;
        switch (value.charAt(value.length() - 1)) {
            case 'k':
                multiplier = new BigDecimal(1024L);
                value = value.substring(0, value.length() - 1);
                break;
            case 'm':
                multiplier = new BigDecimal(1024L).pow(2);
                value = value.substring(0, value.length() - 1);
                break;
            case 'g':
                multiplier = new BigDecimal(1024L).pow(3);
                value = value.substring(0, value.length() - 1);
                break;
            case 't':
                multiplier = new BigDecimal(1024L).pow(4);
                value = value.substring(0, value.length() - 1);
                break;
            case 'p':
                multiplier = new BigDecimal(1024L).pow(5);
                value = value.substring(0, value.length() - 1);
                break;
            case 'e':
                multiplier = new BigDecimal(1024L).pow(6);
                value = value.substring(0, value.length() - 1);
                break;
            default:
                // do nothing -- at this point we can assume this is a number due to the lexer rule for NUMBER
                break;
        }
        
        return new BigDecimal(value).multiply(multiplier);
    }
    
    
    
    
    
    
    
    
    
    private static DataType prefixToType(String input) {
        switch (input.charAt(0)) {
            case 'b':
                return BOOLEAN;
            case 'n':
                return NUMBER;
            case 's':
                return STRING;
            default:
                throw new IllegalArgumentException(); // should never happen
        }
    }
    
    private static DataType classToType(Class<?> input) {
        if (input == Boolean.class) {
            return BOOLEAN;
        } else if (input == BigDecimal.class) {
            return NUMBER;
        } else if (input == String.class) {    
            return STRING;
        }
        throw new IllegalArgumentException(); // should never happen
    }
    
    
    
    
    
    private static void parseIsTrue(ParserRuleContext ctx, boolean check, String formatMsg, Object... values) {
        Validate.notNull(ctx);
        Validate.notNull(formatMsg);

        if (!check) {
            throw new ParserEvalException(ctx, formatMsg, values);
        }
    }
    
    private static void isCorrectId(ParserRuleContext ctx, String name) {
        Validate.notNull(ctx);
        Validate.notNull(name);

        Validate.validState(name.length() > 0); // should never happen
        try {
            IdCheckUtils.isCorrectId(name);
        } catch (IllegalArgumentException iae) {
            throw new ParserEvalException(ctx, iae.getMessage());
        }
    }

    private static void isCorrectVarName(ParserRuleContext ctx, String varName) {
        Validate.notNull(varName);
        isCorrectId(ctx, varName);

        parseIsTrue(ctx, varName.length() > 2, "Variable names must be 2 or more characters");
        
        switch (varName.charAt(0)) {
            case 'b':
            case 'n':
            case 's':
                break;
            default:
                throw new ParserEvalException(ctx, "Variable names must start with the character b, n, or s");
        }
        
        parseIsTrue(ctx, varName.charAt(1) == '_', "Variable names must have a 2nd character of _");
    }

    
    
    private static enum ParseState {
        TAGS,
        REQUIREMENTS
    }
}

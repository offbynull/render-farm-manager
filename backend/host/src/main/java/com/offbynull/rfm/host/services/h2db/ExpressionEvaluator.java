package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.selection.Expression;
import com.offbynull.rfm.host.model.selection.InvocationExpression;
import com.offbynull.rfm.host.model.selection.LiteralExpression;
import com.offbynull.rfm.host.model.selection.SelectionFunction;
import com.offbynull.rfm.host.model.selection.SelectionFunctionBuiltIns;
import com.offbynull.rfm.host.model.selection.VariableExpression;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;

final class ExpressionEvaluator {
    private final UnmodifiableMap<String, Function<Object[], Object>> functions;

    public ExpressionEvaluator(Map<String, Function<Object[], Object>> functions) {
        Map<String, Function<Object[], Object>> functionsCopy = new HashMap<>(functions);
        
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.NOT_B_B_NAME, x -> {
            Boolean val = (Boolean) x[0];
            return !val;
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.DIVIDE_N_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.divide(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.MULTIPLY_N_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.multiply(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.ADD_N_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.add(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.SUB_N_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.subtract(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.GREATER_THAN_B_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.compareTo(rhs) > 0;
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.LESS_THAN_B_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.compareTo(rhs) < 0;
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.EQUAL_B_NN_NAME, x -> {
            BigDecimal lhs = (BigDecimal) x[0];
            BigDecimal rhs = (BigDecimal) x[1];
            return lhs.compareTo(rhs) == 0;
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.EQUAL_B_BB_NAME, x -> {
            Boolean lhs = (Boolean) x[0];
            Boolean rhs = (Boolean) x[1];
            return lhs.equals(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.EQUAL_B_SS_NAME, x -> {
            String lhs = (String) x[0];
            String rhs = (String) x[1];
            return lhs.equals(rhs);
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.AND_B_BB_NAME, x -> {
            Boolean lhs = (Boolean) x[0];
            Boolean rhs = (Boolean) x[1];
            return lhs && rhs;
        });
        functionsCopy.putIfAbsent(SelectionFunctionBuiltIns.OR_B_BB_NAME, x -> {
            Boolean lhs = (Boolean) x[0];
            Boolean rhs = (Boolean) x[1];
            return lhs || rhs;
        });
        
        this.functions = (UnmodifiableMap<String, Function<Object[], Object>>) unmodifiableMap(new HashMap<>(functionsCopy));
    }

    public Object evaluate(Expression expression, Map<String, Object> properties) {
        Set<String> varNames = new HashSet<>();
        collectVariableNames(expression, varNames);
        
        if (!varNames.containsAll(properties.keySet())) {
            return null;
        }
        
        return coreEvaluate(expression, properties);
    }
    
    private Object coreEvaluate(Expression expression, Map<String, Object> properties) {
        if (expression instanceof LiteralExpression) {
            return ((LiteralExpression) expression).getValue();
        } else if (expression instanceof VariableExpression) {
            String name = ((VariableExpression) expression).getName();
            return properties.get(name);
        } else if (expression instanceof InvocationExpression) {
            InvocationExpression invExpr = ((InvocationExpression) expression);
            
            SelectionFunction func = invExpr.getFunction();
            String funcName = func.getName();
            Function<Object[], Object> funcImpl = functions.get(funcName);
            
            Object[] funcArgs = invExpr.getArguments().stream().map(x -> evaluate(x, properties)).toArray();
            
            return funcImpl.apply(funcArgs);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    private void collectVariableNames(Expression expression, Set<String> varNames) {
        if (expression instanceof VariableExpression) {
            String name = ((VariableExpression) expression).getName();
            varNames.add(name);
        } else if (expression instanceof InvocationExpression) {
            for (Expression childExpression : ((InvocationExpression) expression).getArguments()) {
                collectVariableNames(childExpression, varNames);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.LiteralExpression;
import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import static com.offbynull.rfm.host.services.h2db.QuerySqlPrimitives.selectFlattenedProperties;
import static com.offbynull.rfm.host.services.h2db.QuerySqlPrimitives.selectWithinDesiredRange;
import static java.util.Arrays.stream;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import org.apache.commons.lang3.Validate;

final class QuerySqlEvaluators {
    private QuerySqlEvaluators() {
        // do nothing
    }

    public static void main(String[] args) {
        Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
        HostRequirement hr = parser.parseScriptReqs(EMPTY_MAP, ""
                + "[1,5] host {"
                + "  1 socket {"
                + "    2 cores {"
                + "      [2,999999] cpus {"
                + "        100000 capacity"
                + "      }"
                + "    }"
                + "  }"
                + "  1 gpu {"
                + "    available"
                + "  }"
                + "  [1,2] mounts {"
                + "    256gb capacity"
                + "  }"
                + "  ram where ram.n_speed>=3000 && ram.s_brand==\"samsung\" {"
                + "    64gb capacity"
                + "  }"
                + "}");
//        
//        SubqueryTracker st = new SubqueryTracker();
//        String query = selectPotentialWorkers(st, hr);
        
//        SubqueryTracker st = new SubqueryTracker();
//        String query = selectProp(st, "host", List.of("s_host", "n_port"), "b_active");

//        QueryTracker st = new QueryTracker();
//        String query = selectFlattenedProperties(st, "host", List.of("s_host", "n_port"), List.of("b_active", "s_region", "n_class"));

        QueryTracker qt = new QueryTracker();
        String query = selectRamRequirement(qt, hr.getRamRequirements().get(0).getWhereCondition());
        
        System.out.println(query);
    }
    
    private static String selectRamRequirement(QueryTracker qt, Expression ramWhere) {
        qt.enter();
        try {
            Set<String> hostKeys = HostSpecification.getKeyPropertyNames();
            Set<String> hostProps = new HashSet<>();
            collectVariableNames(ramWhere, "host", hostProps);            
            String id1 = qt.next();
            String qr1 = selectFlattenedProperties(qt, "host", hostKeys, hostProps);
            
            Set<String> ramKeys = RamSpecification.getKeyPropertyNames();
            Set<String> ramProps = new HashSet<>();
            collectVariableNames(ramWhere, "ram", hostProps);
            String id2 = qt.next();
            String qr2 = selectFlattenedProperties(qt, "ram", ramKeys, ramProps);
            
            Set<String> qr3SelectFields = new LinkedHashSet<>();
            hostKeys.stream().map(f -> id1 + "." + f + " AS " + "host" + "_" + f).forEachOrdered(qr3SelectFields::add);
            hostProps.stream().map(f -> id1 + "." + f + " AS " + "host" + "_" + f).forEachOrdered(qr3SelectFields::add);
            ramKeys.stream().map(f -> id2 + "." + f + " AS " + "ram" + "_" + f).forEachOrdered(qr3SelectFields::add);
            ramProps.stream().map(f -> id2 + "." + f + " AS " + "ram" + "_" + f).forEachOrdered(qr3SelectFields::add);
            String id3 = qt.next();
            String qr3 = ""
                    + "SELECT "
                    + qr3SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + qt.identLines(1, qr1)
                    + "\n) " + id1 + "\n"
                    + "RIGHT JOIN\n"
                    + "(\n"
                    + qt.identLines(1, qr2)
                    + "\n) " + id2 + "\n"
                    + "ON " + hostKeys.stream().map(k -> id1 + "." + k + "=" + id2 + "." + k).collect(joining(" AND "));

            String id4 = qt.next();
            String qr4 = ""
                    + "SELECT "
                    + expressionize(ramWhere, qt, id3, EMPTY_MAP) + "\n"
                    + "FROM (\n"
                    + qt.identLines(1, qr3)
                    + "\n) " + id3;
            
            return qr4;
        } finally {
            qt.exit();
        }
    }
    
    private static String selectPotentialWorkers(QueryTracker qt, HostRequirement hostReq) {
        qt.enter();
        try {
            NumberRange socketNr = hostReq.getSocketRequirements().stream()
                    .map(x -> x.getNumberRange())
                    .reduce(NumberRange::combineNumberRanges)
                    .orElse(NumberRange.NONE);
            NumberRange gpuNr = hostReq.getGpuRequirements().stream()
                    .map(x -> x.getNumberRange())
                    .reduce(NumberRange::combineNumberRanges)
                    .orElse(NumberRange.NONE);
            NumberRange mountNr = hostReq.getMountRequirements().stream()
                    .map(x -> x.getNumberRange())
                    .reduce(NumberRange::combineNumberRanges)
                    .orElse(NumberRange.NONE);
            NumberRange ramNr = hostReq.getRamRequirements().stream()
                    .map(x -> x.getNumberRange())
                    .reduce(NumberRange::combineNumberRanges)
                    .orElse(NumberRange.NONE);
            
            Set<String> hostPk = HostSpecification.getKeyPropertyNames();
            
            String id1 = qt.next();
            String qr1 = selectWithinDesiredRange(qt, "socket", hostPk, socketNr);
            
            String id2 = qt.next();
            String qr2 = selectWithinDesiredRange(qt, "gpu", hostPk, gpuNr);
            
            String id3 = qt.next();
            String qr3 = selectWithinDesiredRange(qt, "mount", hostPk, mountNr);
            
            String id4 = qt.next();
            String qr4 = selectWithinDesiredRange(qt, "ram", hostPk, ramNr);
            
            String qr5 =
                    "SELECT " + id1 + ".s_host, " + id1 + ".n_port FROM\n"
                    + "(\n" + qr1 + "\n) " + id1
                    + "\nINNER JOIN\n"
                    + "(\n" + qr2 + "\n) " + id2
                    + " ON " + hostPk.stream().map(x -> id1 + "." + x + "=" + id2 + "." + x).collect(joining(" AND "))
                    + "\nINNER JOIN\n"
                    + "(\n" + qr3 + "\n) " + id3
                    + " ON " + hostPk.stream().map(x -> id2 + "." + x + "=" + id3 + "." + x).collect(joining(" AND "))
                    + "\nINNER JOIN\n"
                    + "(\n" + qr4 + "\n) " + id4
                    + " ON " + hostPk.stream().map(x -> id3 + "." + x + "=" + id4 + "." + x).collect(joining(" AND "));
            
             return qt.identLines(qr5);
        } finally {
            qt.exit();
        }
    }
    
    
    
    
    
    
    
    public static String expressionize(
            Expression expression,
            QueryTracker queryTracker,
            String tableAlias,
            Map<String, String> funcNameMapping) {
        if (expression instanceof LiteralExpression) {
            Object value = ((LiteralExpression) expression).getValue();
            String placeholder = queryTracker.param(value);
            return placeholder;
        } else if (expression instanceof VariableExpression) {
            VariableExpression varExpression = (VariableExpression) expression;
            String scope = varExpression.getScope();
            String name = varExpression.getName();
            return tableAlias + "." + scope + "_" + name;
        } else if (expression instanceof InvocationExpression) {
            InvocationExpression invExpr = ((InvocationExpression) expression);
            
            RequirementFunction func = invExpr.getFunction();
            String name = func.getName();
            String[] args = invExpr.getArguments().stream()
                    .map(x -> expressionize(x, queryTracker, tableAlias, funcNameMapping))
                    .toArray(i -> new String[i]);
            
            switch (name) {
                case RequirementFunctionBuiltIns.NOT_B_B_NAME:
                    return "NOT(" + args[0] + ")";
                case RequirementFunctionBuiltIns.DIVIDE_N_NN_NAME:
                    return "(" + args[0] + " / " + args[1] + ")";
                case RequirementFunctionBuiltIns.MULTIPLY_N_NN_NAME:
                    return "(" + args[0] + " * " + args[1] + ")";
                case RequirementFunctionBuiltIns.ADD_N_NN_NAME:
                    return "(" + args[0] + " + " + args[1] + ")";
                case RequirementFunctionBuiltIns.SUB_N_NN_NAME:
                    return "(" + args[0] + " - " + args[1] + ")";
                case RequirementFunctionBuiltIns.GREATER_THAN_B_NN_NAME:
                    return "(" + args[0] + " > " + args[1] + ")";
                case RequirementFunctionBuiltIns.LESS_THAN_B_NN_NAME:
                    return "(" + args[0] + " < " + args[1] + ")";
                case RequirementFunctionBuiltIns.EQUAL_B_NN_NAME:
                case RequirementFunctionBuiltIns.EQUAL_B_BB_NAME:
                case RequirementFunctionBuiltIns.EQUAL_B_SS_NAME:
                    return "(" + args[0] + " = " + args[1] + ")";
                case RequirementFunctionBuiltIns.AND_B_BB_NAME:
                    return "(" + args[0] + " AND " + args[1] + ")";
                case RequirementFunctionBuiltIns.OR_B_BB_NAME:
                    return "(" + args[0] + " OR " + args[1] + ")";
                default: {
                    String sqlName = funcNameMapping.get(name);
                    Validate.isTrue(sqlName != null, "No mapping for function %s", sqlName);
                    
                    return sqlName + "(" + stream(args).collect(joining(",")) + ")";
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    public static void collectVariableNames(Expression expression, String scope, Set<String> names) {
        if (expression instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) expression;
            if (ve.getScope().equals(scope)) {
                names.add(ve.getName());
            }
        } else if (expression instanceof InvocationExpression) {
            for (Expression childExpression : ((InvocationExpression) expression).getArguments()) {
                collectVariableNames(childExpression, scope, names);
            }
        } else if (expression instanceof LiteralExpression) {
            // do nothing
        } else {
            throw new IllegalArgumentException();
        }
    }
}

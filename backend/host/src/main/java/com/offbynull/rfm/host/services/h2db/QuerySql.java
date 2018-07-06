package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.LiteralExpression;
import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementFullKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementName;
import java.math.BigDecimal;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.Collection;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementKey;

final class QuerySql {
    private QuerySql() {
        // do nothing
    }

    public static void main(String[] args) {
        Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
        HostRequirement hr = parser.parseScriptReqs(EMPTY_MAP, ""
                + "[1,5] host {"
                + "  1 socket where socket.s_brand==\"intel\" {"
                + "    2 core where !core.b_hyperthread {"
                + "      [2,999999] cpu 100000 capacity where cpu.b_avx==true && socket.s_model==\"xeon\" { }"
                + "      1 cpu 5000 capacity where socket.s_model==\"haswell\" { }"
                + "    }"
                + "  }"
                + "  1 gpu with 1 capacity { }"
                + "  1 mount with 256gb capacity where b_rotational==true" // for saving work
                + "  1 mount with 32gb capacity where b_rotational=false" // for temp storage
                + "  1 ram with 64gb capacity where ram.n_speed>=3000 && ram.s_brand==\"samsung\" { }"
                + "}");

        QueryTracker qt = new QueryTracker();
        
//        String rawData = readAllRequirements(
//                qt,
//                List.of(
//                        hr,
//                        hr.getSocketRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0)
//                )
//        );
////        String evalMatchingData = filterRequirementByWhereCondition(
////                qt,
////                rawData,
////                List.of(
////                        hr,
////                        hr.getSocketRequirements().get(0),
////                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0),
////                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0)
////                )
////        );
//        String countMatchingData = filterByCapacity(
//                qt,
//                rawData,
//                CpuRequirement.class,
//                List.of(
//                        hr,
//                        hr.getSocketRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0)
//                ),
//                List.of(
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(1)
//                )
//        );
//        
//        System.out.println(countMatchingData);
    }
    
    public static String readAllRequirements(QueryTracker qt, List<Requirement> requirementChain) {
        qt.enter();
        try {
            Requirement finalRequirement = requirementChain.get(requirementChain.size() - 1);
            Expression finalWhere = finalRequirement.getWhereCondition();
            
            
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            
            Set<String> lastDbKey = null;
            String lastId = null;
            String joinChain = "";
            

            for (Requirement localRequirement : requirementChain) {
                String name = getRequirementName(localRequirement);

                Set<String> key = getRequirementKey(localRequirement);
                Set<String> props = new HashSet<>();

                int idxInRequirementChain = requirementChain.indexOf(localRequirement);
                List<Requirement> localRequirementChain = requirementChain.subList(0, idxInRequirementChain + 1);
                Set<String> dbKey = getRequirementFullKey(localRequirementChain);

                collectVariableNames(finalWhere, name, props);

                String id = qt.alias();
                String qr = selectFlattenedProperties(qt, name, dbKey, props);

                key.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);
                props.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);

                key.stream().map(f -> name + "_" + f).forEachOrdered(id2SelectFields::add);

                if (lastId == null) {
                    joinChain += ""
                            + "(\n"
                            + qt.indentLines(1, qr)
                            + "\n) " + id + "\n";
                } else {
                    final String _lastId = lastId; // must be assigned to new final var becuase of use in lambda
                    joinChain += ""
                            + "RIGHT JOIN\n"
                            + "(\n"
                            + qt.indentLines(1, qr)
                            + "\n) " + id + "\n"
                            + "ON " + lastDbKey.stream().map(k -> _lastId + "." + k + "=" + id + "." + k).collect(joining(" AND "))
                            + "\n";
                }

                lastId = id;
                lastDbKey = dbKey;
            }
            
            
            String qr1 = ""
                    + "SELECT "
                    + id1SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM\n"
                    + joinChain;
            
            return qr1;
        } finally {
            qt.exit();
        }
    }
    
    public static String filterByWhereCondition(QueryTracker qt, String input, List<Requirement> requirementChain) {
        qt.enter();
        try {
            Requirement finalRequirement = requirementChain.get(requirementChain.size() - 1);
            Expression finalWhere = finalRequirement.getWhereCondition();
            
            
            String id1 = qt.alias();
            String id2 = qt.alias();
            String id3 = qt.alias();
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            Set<String> id3SelectFields = new LinkedHashSet<>();
            for (Requirement requirement : requirementChain) {
                String name = getRequirementName(requirement);
                Set<String> key = getRequirementKey(requirement);
                key.stream().map(f -> id1 + "." + name + "_" + f).forEachOrdered(id1SelectFields::add);
                key.stream().map(f -> id2 + "." + name + "_" + f).forEachOrdered(id2SelectFields::add);
                key.stream().map(f -> id3 + "." + name + "_" + f).forEachOrdered(id3SelectFields::add);
            }
            
            String qr1 = ""
                    + "SELECT "
                    + id1SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM\n"
                    + "(\n"
                    + qt.indentLines(1, input)
                    + "\n) " + id1;

            id2SelectFields.add(expressionize(finalWhere, qt, id2, EMPTY_MAP) + " AS expr");
            String qr2 = ""
                    + "SELECT "
                    + id2SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM\n"
                    + "(\n"
                    + qt.indentLines(1, qr1)
                    + "\n) " + id2;
            
            String qr3 = ""
                    + "SELECT "
                    + id3SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM\n"
                    + "(\n"
                    + qt.indentLines(1, qr2)
                    + "\n) " + id3 + " WHERE " + id3 + ".eval=true";
            
            return qr3;
        } finally {
            qt.exit();
        }
    }
    
    public static String selectFlattenedProperties(QueryTracker qt, String spec, Collection<String> pkCols, Collection<String> names) {
        qt.enter();
        try {
            String fullId = qt.alias();
            String fullQuery = "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name" + " FROM " + spec + "_props";
            
            LinkedHashMap<String, String> valCols = new LinkedHashMap<>();
            String output = ""
                    + "(\n"
                    + "  " + fullQuery
                    + "\n) " + fullId; 
            
            for (String name : names) {
                String nextId = qt.alias();
                String nextQuery = selectByProperty(qt, spec, pkCols, name);
                
                valCols.put(nextId + ".val", name);
                output += "\n"
                        + "LEFT JOIN\n"
                        + "(\n"
                        + nextQuery + ""
                        + "\n) " + nextId + "\n"
                        + "ON "
                        + pkCols.stream().map(x -> fullId + "." + x + "=" + nextId + "." + x).collect(joining(" AND "));
            }
            
            List<String> selectCols = new ArrayList<>();
            pkCols.stream().map(x -> fullId + "." + x).forEachOrdered(selectCols::add);
            selectCols.add(fullId + ".name");
            valCols.entrySet().stream().map(e -> e.getKey() + " AS " + e.getValue()).forEachOrdered(selectCols::add);
            
            output = "SELECT " + selectCols.stream().collect(joining(",")) + "\n"
                    + output;
            
            return output;
        } finally {
            qt.exit();
        }
    }
    
    public static String selectByProperty(QueryTracker qt, String spec, Collection<String> pkCols, String name) {
        qt.enter();
        try {
            String col;
            switch (name.substring(0, 2)) {
                case "b_":
                    col = "val_b";
                    break;
                case "n_":
                    col = "val_n";
                    break;
                case "s_":
                    col = "val_s";
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            
            String id1 = qt.alias();
            String qr1 = "  "
                    + "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name"
                    + " FROM " + spec + "_props";

            String id2 = qt.alias();
            String qr2 = "  "
                    + "SELECT " + pkCols.stream().collect(joining(",")) + ",name," + col
                    + " FROM " + spec + "_props"
                    + " WHERE name='" + name + "'";
            
            String id3 = qt.alias();
            String qr3 = ""
                    + "SELECT " + pkCols.stream().map(x -> id1 + "." + x).collect(joining(",")) + ",name," + id2 + "." + col + " AS val\n"
                    + "FROM\n"
                    + "(\n" + qr1 + "\n) " + id1 + "\n"
                    + "LEFT JOIN\n"
                    + "(\n" + qr2 + "\n) " + id2 + "\n"
                    + "ON " + pkCols.stream().map(x -> id1 + "." + x + "=" + id2 + "." + x).collect(joining(" AND "));
            
            return qt.indentLines(qr3);
        } finally {
            qt.exit();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    static Query filterHostsByCountAndCapacity(
            Map<String, BigDecimal> minCounts,
            Map<String, BigDecimal> minCapacities) {
        QueryTracker qt = new QueryTracker();
        
        String id1 = qt.alias();
        List<String> whereChain1 = Stream
                .concat(
                        minCounts.entrySet().stream().map(e -> e.getKey() + "_count=" + qt.param(e.getKey(), e.getValue())),
                        minCapacities.entrySet().stream().map(e -> e.getKey() + "_capacity_sum=" + qt.param(e.getKey(), e.getValue()))
                )
                .collect(toList());
        String qr1 = "SELECT s_host, n_port\n"
                + "FROM worker " + id1 + "\n"
                + "WHERE " + whereChain1.stream().collect(joining(" AND "));

        return new Query(qr1, qt.params());
    }
    
    static String filterHostsByExpression(
            QueryTracker qt,
            String hostQuery, // subquery that returns a set of (s_host,n_port)
            List<Requirement> parentChain,
            List<Requirement> children) {
        Validate.notNull(qt);
        Validate.notNull(hostQuery);
        Validate.notNull(parentChain);
        Validate.notNull(children);
        Validate.isTrue(!parentChain.isEmpty());
        Validate.isTrue(parentChain.get(0) instanceof HostRequirement);
        Validate.isTrue(!children.isEmpty());
        Validate.isTrue(children.stream().map(x -> x.getClass()).distinct().count() == 1L);
        
        readAllRequirements(qt, )
        
        // iterate through each parent and collect variable names
        // iterate through children and collect variable names
        // select all children that contain these variable names -- parent variables can be referenced as well, so join with parent chain
    }
    
    public static String performEvaluations(
            QueryTracker qt,
            String hostQuery,
            List<Requirement> parentChain,
            List<Requirement> children) {
        Validate.notNull(qt);
        Validate.notNull(hostQuery);
        Validate.notNull(parentChain);
        Validate.notNull(children);
        Validate.noNullElements(parentChain);
        Validate.noNullElements(children);
        Validate.isTrue(!children.isEmpty());
        Validate.isTrue(children.stream().map(x -> x.getClass()).distinct().count() == 1L);
        
        qt.enter();
        try {
            List<Expression> finalWheres = children.stream().map(x -> x.getWhereCondition()).collect(toList());
            
            
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            
            Set<String> lastDbKey = null;
            String lastId = null;
            String joinChain = "";
            

            for (Requirement localRequirement : requirementChain) {
                String name = getRequirementName(localRequirement);

                Set<String> key = getRequirementKey(localRequirement);
                Set<String> props = new HashSet<>();

                int idxInRequirementChain = requirementChain.indexOf(localRequirement);
                List<Requirement> localRequirementChain = requirementChain.subList(0, idxInRequirementChain + 1);
                Set<String> dbKey = getRequirementFullKey(localRequirementChain);

                collectVariableNames(finalWhere, name, props);

                String id = qt.alias();
                String qr = selectFlattenedProperties(qt, name, dbKey, props);

                key.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);
                props.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);

                key.stream().map(f -> name + "_" + f).forEachOrdered(id2SelectFields::add);

                if (lastId == null) {
                    joinChain += ""
                            + "(\n"
                            + qt.indentLines(1, qr)
                            + "\n) " + id + "\n";
                } else {
                    final String _lastId = lastId; // must be assigned to new final var becuase of use in lambda
                    joinChain += ""
                            + "RIGHT JOIN\n"
                            + "(\n"
                            + qt.indentLines(1, qr)
                            + "\n) " + id + "\n"
                            + "ON " + lastDbKey.stream().map(k -> _lastId + "." + k + "=" + id + "." + k).collect(joining(" AND "))
                            + "\n";
                }

                lastId = id;
                lastDbKey = dbKey;
            }
            
            
            String qr1 = ""
                    + "SELECT "
                    + id1SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM\n"
                    + joinChain;
            
            return qr1;
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

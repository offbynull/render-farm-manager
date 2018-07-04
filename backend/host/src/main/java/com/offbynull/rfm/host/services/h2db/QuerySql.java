package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.LiteralExpression;
import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.model.requirement.CapacityEnabledRequirement;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
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
import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

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
        
        String rawData = readAllRequirements(
                qt,
                List.of(
                        hr,
                        hr.getSocketRequirements().get(0),
                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0),
                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0)
                )
        );
//        String evalMatchingData = filterRequirementByWhereCondition(
//                qt,
//                rawData,
//                List.of(
//                        hr,
//                        hr.getSocketRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0),
//                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0)
//                )
//        );
        String countMatchingData = filterByCapacity(
                qt,
                rawData,
                CpuRequirement.class,
                List.of(
                        hr,
                        hr.getSocketRequirements().get(0),
                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0)
                ),
                List.of(
                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(0),
                        hr.getSocketRequirements().get(0).getCoreRequirements().get(0).getCpuRequirements().get(1)
                )
        );
        
        System.out.println(countMatchingData);
    }
    
    public static String readAllRequirements(QueryTracker qt, List<Requirement> requirementChain) {
        qt.enter();
        try {
            Requirement finalRequirement = requirementChain.get(requirementChain.size() - 1);
            Expression finalWhere = finalRequirement.getWhereCondition();
            
            
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            
            Set<String> lastDbKeys = null;
            String lastId = null;
            String joinChain = "";
            

            for (Requirement requirement : requirementChain) {
                String name = getName(requirement);

                Set<String> keys = getKeys(requirement);
                Set<String> props = new HashSet<>();

                Set<String> dbKeys = getDbKey(requirementChain, requirement);

                collectVariableNames(finalWhere, name, props);

                String id = qt.next();
                String qr = selectFlattenedProperties(qt, name, dbKeys, props);

                keys.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);
                props.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);

                keys.stream().map(f -> name + "_" + f).forEachOrdered(id2SelectFields::add);

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
                            + "ON " + lastDbKeys.stream().map(k -> _lastId + "." + k + "=" + id + "." + k).collect(joining(" AND "))
                            + "\n";
                }

                lastId = id;
                lastDbKeys = dbKeys;
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
            
            
            String id1 = qt.next();
            String id2 = qt.next();
            String id3 = qt.next();
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            Set<String> id3SelectFields = new LinkedHashSet<>();
            for (Requirement requirement : requirementChain) {
                String name = getName(requirement);
                Set<String> keys = getKeys(requirement);
                keys.stream().map(f -> id1 + "." + name + "_" + f).forEachOrdered(id1SelectFields::add);
                keys.stream().map(f -> id2 + "." + name + "_" + f).forEachOrdered(id2SelectFields::add);
                keys.stream().map(f -> id3 + "." + name + "_" + f).forEachOrdered(id3SelectFields::add);
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
            String fullId = qt.next();
            String fullQuery = "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name" + " FROM " + spec + "_props";
            
            LinkedHashMap<String, String> valCols = new LinkedHashMap<>();
            String output = ""
                    + "(\n"
                    + "  " + fullQuery
                    + "\n) " + fullId; 
            
            for (String name : names) {
                String nextId = qt.next();
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
            
            String id1 = qt.next();
            String qr1 = "  "
                    + "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name"
                    + " FROM " + spec + "_props";

            String id2 = qt.next();
            String qr2 = "  "
                    + "SELECT " + pkCols.stream().collect(joining(",")) + ",name," + col
                    + " FROM " + spec + "_props"
                    + " WHERE name='" + name + "'";
            
            String id3 = qt.next();
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
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static String filterByCount(QueryTracker qt, String input, Collection<String> parentPk, BigDecimal minCount) {
        qt.enter();
        try {
            if (parentPk.isEmpty()) {
                String id1 = qt.next();
                String qr1 = ""
                        + "SELECT *\n"
                        + "FROM (\n"
                        + qt.indentLines(1, input)
                        + "\n) " + id1 + "\n"
                        + "GROUP BY " + parentPk.stream().map(n -> id1 + "." + n).collect(joining(","));
                
                return qt.indentLines(qr1);
            } else {
                String id1 = qt.next();
                LinkedHashSet<String> id1SelectFields = parentPk.stream()
                        .map(n -> id1 + "." + n)
                        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
                id1SelectFields.add("count(*) AS cnt");
                String qr1 = ""
                        + "SELECT " + id1SelectFields.stream().collect(joining(",")) + "\n"
                        + "FROM (\n"
                        + qt.indentLines(3, input)
                        + "\n) " + id1 + "\n"
                        + "GROUP BY " + parentPk.stream().map(n -> id1 + "." + n).collect(joining(","));

                String id2 = qt.next();
                String qr2 = ""
                        + "SELECT *\n"
                        + "FROM (\n"
                        + qt.indentLines(2, qr1)
                        + "\n) " + id2 + "\n"
                        + "WHERE " + id2 + ".cnt >= " + qt.param("minCount", minCount);

                String id3 = qt.next();
                LinkedHashSet<String> id3SelectFields = parentPk.stream()
                        .map(n -> id3 + "." + n)
                        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
                String qr3 = ""
                        + "SELECT " + id3SelectFields.stream().collect(joining(",")) + "\n"
                        + "FROM (\n"
                        + qt.indentLines(1, qr2)
                        + "\n) " + id3;

                return qt.indentLines(qr3);
            }
        } finally {
            qt.exit();
        }
    }
    
    private static <U extends Requirement> String filterByCapacity(
            QueryTracker qt,
            String input,
            Class<U> childRequirementClass,
            List<Requirement> parentRequirementChain,
            List<U> childRequirements) {
        Validate.isTrue(isAssignable(childRequirementClass, CapacityEnabledRequirement.class));
        Validate.isTrue(!parentRequirementChain.isEmpty());
        
        qt.enter();
        try {
            BigDecimal minCapacity = childRequirements.stream()
                    .map(c -> (CapacityEnabledRequirement) c)
                    .map(c -> c.getCapacityRange().getStart())
                    .reduce(BigDecimal.ZERO, (a,b) -> a.add(b));
            
            String id1 = qt.next();
            String id2 = qt.next();
            Set<String> id1SelectFields = new LinkedHashSet<>();
            Set<String> id2SelectFields = new LinkedHashSet<>();
            Set<String> id1GroupByFields = new LinkedHashSet<>();
            for (Requirement requirement : parentRequirementChain) {
                String name = getName(requirement);
                Set<String> keys = getKeys(requirement);
                keys.stream().map(f -> id1 + "." + name + "_" + f).forEachOrdered(id1SelectFields::add);
                keys.stream().map(f -> id1 + "." + name + "_" + f).forEachOrdered(id1GroupByFields::add);
                keys.stream().map(f -> id1 + "." + name + "_" + f).forEachOrdered(id2SelectFields::add);
            }

            id1SelectFields.add("sum(" + id1 + ".capacity) AS capacity_sum");
            String qr1 = ""
                    + "SELECT " + id1SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + qt.indentLines(2, input)
                    + "\n) " + id1 + "\n"
                    + "GROUP BY " + id1GroupByFields.stream().collect(joining(","));

            String qr2 = ""
                    + "SELECT " + id2SelectFields.stream().collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + qt.indentLines(1, qr1)
                    + "\n) " + id2 + "\n"
                    + "WHERE " + id2 + ".capacity_sum >= " + qt.param("minCount", minCapacity);

            return qt.indentLines(qr2);
        } finally {
            qt.exit();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static Set<String> getDbKey(List<Requirement> requirementChain, Requirement requirement) {
        requirementChain = requirementChain.subList(0, requirementChain.indexOf(requirement)+1);
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Requirement req : requirementChain) {
            Set<String> key = getKeys(req);
            ret.addAll(key);
        }
        
        return ret;
    }

    private static Set<String> getDbKey(List<Requirement> requirementChain) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Requirement req : requirementChain) {
            Set<String> key = getKeys(req);
            ret.addAll(key);
        }
        
        return ret;
    }
    
    private static Set<String> getKeys(Requirement requirement) {
        return getKeys(requirement.getClass());
    }
    
    private static Set<String> getKeys(Class<?> requirementCls) {
        String name = capitalize(getName(requirementCls));

        String specClassStr = "com.offbynull.rfm.host.model.specification." + name + "Specification";
        try {
            Class<?> specClass = requirementCls.getClassLoader().loadClass(specClassStr);
            return (Set<String>) MethodUtils.invokeStaticMethod(specClass, "getKeyPropertyNames");
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException(roe);
        }
    }
    
    private static String getName(Requirement requirement) {
        return getName(requirement.getClass());
    }
    
    private static String getName(Class<?> requirementCls) {
        String className = requirementCls.getSimpleName();
        Validate.isTrue(className.endsWith("Requirement"));
                
        String reqName = uncapitalize(removeEnd(className, "Requirement"));
        
        return reqName;
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

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.LiteralExpression;
import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.model.requirement.Requirement;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.flattenRequirementHierarchy;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementFullKeyFromClasses;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementName;
import java.math.BigDecimal;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.localizeChain;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toClasses;

final class WorkerSearcherSql {
    private WorkerSearcherSql() {
        // do nothing
    }
    
    public static Query filter(
            Requirement requirement,
            Map<String, BigDecimal> minCounts,
            Map<String, BigDecimal> minCapacities) {
        Validate.notNull(requirement);
        Validate.notNull(minCounts);
        Validate.notNull(minCapacities);
        Validate.noNullElements(minCounts.keySet());
        Validate.noNullElements(minCounts.values());
        Validate.noNullElements(minCapacities.keySet());
        Validate.noNullElements(minCapacities.values());
        
        Requirement firstRequirement = requirement;
        Set<String> firstKey = getRequirementKey(firstRequirement);
        
        QueryTracker qt = new QueryTracker();
        
        String joinChain;
        
        String nextId = qt.alias();
        Query nextQuery = filterHostsByCountAndCapacity(minCounts, minCapacities);
        joinChain = ""
                + "(\n"
                + nextQuery.compose(2, qt)
                + "\n) " + nextId;
        
        for (List<? extends Requirement> requirementChain : flattenRequirementHierarchy(requirement)) {
            String lastId = nextId;
            String _nextId = qt.alias(); // hack so nextId can be used in lambda below (can't assign to nextId directly)
            
            nextQuery = filterHostsByWhereCondition(requirementChain);
            joinChain += ""
                    + "\n"
                    + "INNER JOIN\n"
                    + "(\n"
                    + nextQuery.compose(2, qt)
                    + "\n) " + _nextId + "\n"
                    + "ON " + firstKey.stream().map(k -> lastId + "." + k + "=" + _nextId + "." + k).collect(joining(" AND "));
            
            nextId = _nextId;
        }
        
        Query finalQuery = new Query(""
                + "SELECT "
                + firstKey.stream().collect(joining(",")) + "\n"
                + "FROM\n"
                + joinChain,
                qt.params()
        );
        
        return finalQuery;
    }
    
    public static Query filterHostsByWhereCondition(List<? extends Requirement> requirementChain) {
        Validate.notNull(requirementChain);
        Validate.noNullElements(requirementChain);
        Validate.isTrue(!requirementChain.isEmpty());
        
        Query exprQuery = selectWithExpressionProperties(requirementChain);
        
        Requirement firstRequirement = requirementChain.get(0);
        Requirement lastRequirement = requirementChain.get(requirementChain.size() - 1);
        Expression lastWhere = lastRequirement.getWhereCondition();

        String firstName = getRequirementName(firstRequirement);
        Set<String> firstKey = getRequirementKey(firstRequirement);
        
        Set<String> fullAugmentedKey = new LinkedHashSet<>();
        for (Requirement requirement : requirementChain) {
            String name = getRequirementName(requirement);
            Set<String> key = getRequirementKey(requirement);
            key.stream().map(f -> name + "_" + f).forEachOrdered(fullAugmentedKey::add);
        }
        

        QueryTracker qt;
        
        // select
        qt = new QueryTracker();
        String id1 = qt.alias();
        LinkedHashSet<String> sf1 = fullAugmentedKey.stream()
                .map(k -> id1 + "." + k)
                .collect(LinkedHashSet::new , LinkedHashSet::add, LinkedHashSet::addAll);
        Query qr1 = new Query(""
                + "SELECT "
                + sf1.stream().collect(joining(",")) + "\n"
                + "FROM\n"
                + "(\n"
                + exprQuery.compose(2, qt)
                + "\n) " + id1,
                qt.params()
        );

        // evaluate wherecondition of last requirement
        qt = new QueryTracker();
        String id2 = qt.alias();
        LinkedHashSet<String> sf2 = fullAugmentedKey.stream()
                .map(k -> id2 + "." + k)
                .collect(LinkedHashSet::new , LinkedHashSet::add, LinkedHashSet::addAll);
        sf2.add(expressionize(lastWhere, qt, id2, EMPTY_MAP) + " AS expr");
        Query qr2 = new Query(""
                + "SELECT "
                + sf2.stream().collect(joining(",")) + "\n"
                + "FROM\n"
                + "(\n"
                + qr1.compose(2, qt)
                + "\n) " + id2,
                qt.params()
        );

        // filter where wherecondition = true
        qt = new QueryTracker();
        String id3 = qt.alias();
        LinkedHashSet<String> sf3 = fullAugmentedKey.stream()
                .map(k -> id3 + "." + k)
                .collect(LinkedHashSet::new , LinkedHashSet::add, LinkedHashSet::addAll);
        Query qr3 = new Query(""
                + "SELECT "
                + sf3.stream().collect(joining(",")) + "\n"
                + "FROM\n"
                + "(\n"
                + qr2.compose(2, qt)
                + "\n) " + id3 + " WHERE " + id3 + ".eval=true",
                qt.params()
        );
        
        // grab only the top-most requirement key (e.g. the key for HostRequirement)
        qt = new QueryTracker();
        String id4 = qt.alias();
        LinkedHashSet<String> sf4 = firstKey.stream()
                .map(k -> firstName + "_" + k + " AS " + k)
                .collect(LinkedHashSet::new , LinkedHashSet::add, LinkedHashSet::addAll);
        Query qr4 = new Query(""
                + "SELECT DISTINCT "
                + sf4.stream().collect(joining(",")) + "\n"
                + "FROM\n"
                + "(\n"
                + qr3.compose(2, qt)
                + "\n) " + id4,
                qt.params()
        );

        return qr4;
    }
    
    public static Query selectWithExpressionProperties(List<? extends Requirement> requirementChain) {
        QueryTracker qt = new QueryTracker();
        
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

            List<Requirement> localRequirementChain = localizeChain(requirementChain, localRequirement);
            List<Class<? extends Requirement>> localRequirementChainClasses = toClasses(localRequirementChain);
            Set<String> dbKey = getRequirementFullKeyFromClasses(localRequirementChainClasses);

            collectVariableNames(finalWhere, name, props);

            String id = qt.alias();
            Query qr = selectFlattenedProperties(localRequirementChainClasses, props);

            key.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);
            props.stream().map(f -> id + "." + f + " AS " + name + "_" + f).forEachOrdered(id1SelectFields::add);

            key.stream().map(f -> name + "_" + f).forEachOrdered(id2SelectFields::add);

            if (lastId == null) {
                joinChain += ""
                        + "(\n"
                        + qr.compose(2, qt)
                        + "\n) " + id + "\n";
            } else {
                final String _lastId = lastId; // must be assigned to new final var becuase of use in lambda
                joinChain += ""
                        + "RIGHT JOIN\n"
                        + "(\n"
                        + qr.compose(2, qt)
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

        return new Query(qr1, qt.params());
    }
    
    public static Query selectFlattenedProperties(
            List<Class<? extends Requirement>> requirementChain,
            Set<String> propertyNames) {
        Validate.notNull(requirementChain);
        Validate.notNull(propertyNames);
        Validate.noNullElements(requirementChain);
        Validate.noNullElements(propertyNames);
        
        Class<? extends Requirement> finalReqClass = requirementChain.get(requirementChain.size() - 1);
        String name = getRequirementName(finalReqClass);
        Set<String> dbKeys = getRequirementFullKeyFromClasses(requirementChain);
        
        QueryTracker qt;
        
        
        qt = new QueryTracker();
        Query fullQuery = new Query(""
                + "SELECT DISTINCT " + dbKeys.stream().collect(joining(",")) + ",name"
                + " FROM " + name + "_props",
                qt.params()
        );


        List<String> selectCols = new ArrayList<>();
        String joinChain;
        
        qt = new QueryTracker();
        String fullQueryAlias = qt.alias("fullquery");
        joinChain = ""
                + "(\n"
                + "  " + fullQuery
                + "\n) " + fullQueryAlias;
        dbKeys.forEach(dbKey -> selectCols.add(fullQueryAlias + "." + dbKey));
        selectCols.add(fullQueryAlias + ".name");

        for (String propertyName : propertyNames) {
            String nextAlias = qt.alias();
            Query nextQuery = selectByProperty(requirementChain, propertyName);

            selectCols.add(nextAlias + ".val AS " + propertyName);
            joinChain += "\n"
                    + "LEFT JOIN\n"
                    + "(\n"
                    + nextQuery.compose(2, qt) + "\n"
                    + "\n) " + nextAlias + "\n"
                    + "ON "
                    + dbKeys.stream().map(x -> fullQueryAlias + "." + x + "=" + nextAlias + "." + x).collect(joining(" AND "));
        }

        Query finalQuery = new Query("SELECT " + selectCols.stream().collect(joining(",")) + "\n" + joinChain, qt.params());
        return finalQuery;
    }
    
    public static Query selectByProperty(
            List<Class<? extends Requirement>> requirementChain,
            String propertyName) {
        Validate.notNull(requirementChain);
        Validate.notNull(propertyName);
        Validate.noNullElements(requirementChain);
        Validate.isTrue(!requirementChain.isEmpty());
        
        Class<? extends Requirement> finalReqClass = requirementChain.get(requirementChain.size() - 1);
        String name = getRequirementName(finalReqClass);
        Set<String> dbKeys = getRequirementFullKeyFromClasses(requirementChain);

        String col;
        switch (propertyName.substring(0, 2)) {
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

        QueryTracker qt;
        
        qt = new QueryTracker();
        Query qr1 = new Query(""
                + "SELECT DISTINCT " + dbKeys.stream().collect(joining(",")) + ",name"
                + " FROM " + name + "_props",
                qt.params()
        );

        qt = new QueryTracker();
        String nameParamToken = qt.param("name", name);
        Query qr2 = new Query(""
                + "SELECT " + dbKeys.stream().collect(joining(",")) + ",name," + col
                + " FROM " + name + "_props"
                + " WHERE name=" + nameParamToken,
                qt.params()
        );

        qt = new QueryTracker();
        String id1 = qt.alias();
        String id2 = qt.alias();
        Query qr3 = new Query(""
                + "SELECT " + dbKeys.stream().map(x -> id1 + "." + x).collect(joining(",")) + ",name," + id2 + "." + col + " AS val\n"
                + "FROM\n"
                + "(\n" + qr1.compose(2, qt) + "\n) " + id1 + "\n"
                + "LEFT JOIN\n"
                + "(\n" + qr2.compose(2, qt) + "\n) " + id2 + "\n"
                + "ON " + dbKeys.stream().map(x -> id1 + "." + x + "=" + id2 + "." + x).collect(joining(" AND ")),
                qt.params()
        );
        
        return qr3;
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

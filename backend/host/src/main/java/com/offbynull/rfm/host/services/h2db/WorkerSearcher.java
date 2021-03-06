package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.model.expression.InvocationExpression;
import com.offbynull.rfm.host.model.expression.LiteralExpression;
import com.offbynull.rfm.host.model.expression.RequirementFunction;
import com.offbynull.rfm.host.model.expression.RequirementFunctionBuiltIns;
import com.offbynull.rfm.host.model.expression.VariableExpression;
import com.offbynull.rfm.host.model.requirement.CapacityEnabledRequirement;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.parser.Parser;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.flattenRequirementHierarchy;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementChildren;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementFullKeyFromClasses;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getRequirementName;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.localizeChain;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toClasses;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;

final class WorkerSearcher {
    private WorkerSearcher() {
        // do nothing
    }
    
    public static void main(String[] args) {
        Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
        HostRequirement hr = parser.parseScriptReqs(EMPTY_MAP, ""
                + "[1,5] host where host.n_class==5 && host.s_region==\"US-EAST\"{"
                + "  1 socket where socket.s_brand==\"intel\" {"
                + "    2 core where !core.b_hyperthread {"
                + "      [2,999999] cpu with 100000 capacity where cpu.b_avx==true && socket.s_model==\"xeon\" { }"
                + "      1 cpu with 5000 capacity where socket.s_model==\"haswell\" { }"
                + "    }"
                + "  }"
                + "  1 gpu with 1 capacity { }"
                + "  1 mount with 256gb capacity where mount.b_rotational==true" // for saving work
                + "  1 mount with 32gb capacity where mount.b_rotational==false" // for temp storage
                + "  1 ram with 64gb capacity where ram.n_speed>=3000 && ram.s_brand==\"samsung\" { }"
                + "}");
        
        Query qr = generateQuery(hr);
        System.out.println(qr.toJdbcQuery());
    }
    
    private static Query generateQuery(HostRequirement hostRequirement) {
        Validate.notNull(hostRequirement);
        
        // get hierarchy of hostRequirement, NOT INCLUDING hostRequirement itself
        //   we don't user hostRequirement directly because we don't want the query generated by filterByCachedCountAndCapacity() to take in
        //   the host count -- the count and capacity we're calculating is PER individual host, not a combination of multiple hosts.
        List<Requirement> parentRequirements = new ArrayList<>(getRequirementChildren(hostRequirement).values());
        
        // get host key -- s_host and n_port as of now, but done dynamically so this code will continue working if it changes
        Set<String> hostKey = getRequirementKey(hostRequirement);
        
        // generate query
        QueryTracker qt = new QueryTracker();
        String queryStr = ""
                + "SELECT " + hostKey.stream().collect(joining(",")) + "\n"
                + "FROM\n";
        
        
        String nextId = qt.alias("hostcount");
        Query nextQuery = filterByCachedCountAndCapacity("worker", parentRequirements);
        queryStr += ""
                + "(\n"
                + nextQuery.compose(2, qt)
                + "\n) " + nextId;
        
        for (List<Requirement> requirementChain : flattenRequirementHierarchy(hostRequirement)) {
            String aliasHint = requirementChain.stream().map(r -> getRequirementName(r)).collect(joining());
            
            String lastId = nextId;
            String _nextId = qt.alias(aliasHint); // hack so nextId can be used in lambda below (can't assign to nextId directly)
            
            nextQuery = filterByWhereCondition(requirementChain);
            queryStr += ""
                    + "\n"
                    + "INNER JOIN\n"
                    + "(\n"
                    + nextQuery.compose(2, qt)
                    + "\n) " + _nextId + "\n"
                    + "ON " + hostKey.stream().map(k -> lastId + "." + k + "=" + _nextId + "." + k).collect(joining(" AND "));
            
            nextId = _nextId;
        }
        
        Query query = new Query(queryStr, qt.params());
        return query;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static Query filterByWhereCondition(List<Requirement> requirementChain) {
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
    
    static Query selectWithExpressionProperties(List<Requirement> requirementChain) {
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
    
    static Query selectFlattenedProperties(
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
                + "  " + fullQuery.compose(2, qt)
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
    
    static Query selectByProperty(
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
    
    static String expressionize(
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
    
    static void collectVariableNames(Expression expression, String scope, Set<String> names) {
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
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static Query filterByCachedCountAndCapacity(
            String cacheTableName,
            List<Requirement> parentRequirements) {
        Validate.notNull(cacheTableName);
        Validate.notNull(parentRequirements);
        Validate.noNullElements(parentRequirements);
        Validate.notEmpty(cacheTableName);
        Validate.notEmpty(parentRequirements);
        
        Map<String, BigDecimal> minCounts = new HashMap<>();
        Map<String, BigDecimal> minCapacities = new HashMap<>();
        for (Requirement parentRequirement : parentRequirements) {
            recursiveCalculateTotals(parentRequirement, ONE, minCounts, minCapacities);
        }
        
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
    
    // Sums up the TOTAL start counts and TOTAL minimum capacities. For example...
    //     [3,4] socket {
    //       ? core {
    //         [10,999999] cpu with [50000,100000] capacity
    //         1 cpu with 1000 capacity
    //       }
    //     }
    //     mount with capacity [25gb,50gb]
    //     mount with capacity [100gb,120gb]
    // will result in...
    // count_socket   = 3                         = 3              = 3
    // count_core     = 3*1                       = 3              = 3
    // count_cpu      = 3*1*10 + 3*1*1            = 30 + 3         = 33
    // capacity_cpu   = 3*1*10*50000 + 3*1*1*1000 = 1500000 + 3000 = 1503000
    // count_mount    = 1 + 1                     = 1 + 1          = 2
    // capacity_mount = 1*25gb + 1*100gb          = 25gb + 100gb   = 125gb
    //
    // Then we grab 2 hosts that match those counts.
    private static void recursiveCalculateTotals(
            Requirement requirement,
            BigDecimal countMultiplier,
            Map<String, BigDecimal> minCounts,
            Map<String, BigDecimal> minCapacities) {
        String name = getRequirementName(requirement);
        
        // remember count of null means '?' was used when specifying the req
        BigDecimal countStart = requirement.getCount() == null ? ONE : requirement.getCount().getStart();
        BigDecimal countStartMultipliedByCount = countMultiplier.multiply(countStart);
        
        BigDecimal minCount = minCounts.getOrDefault(name, BigDecimal.ZERO).add(countStartMultipliedByCount);
        minCounts.put(name, minCount);
            
        // If requirement has capacity, add min capacity in minCapacities map.
        if (requirement instanceof CapacityEnabledRequirement) {
            BigDecimal capacityStart = ((CapacityEnabledRequirement) requirement).getCapacityRange().getStart();
            BigDecimal capacityStartMultipliedByCount = countStart.multiply(capacityStart);
            
            BigDecimal minCapacity = minCapacities.getOrDefault(name, BigDecimal.ZERO).add(capacityStartMultipliedByCount);
            minCapacities.put(name, minCapacity);
        }
        
        // Get children -- for each child, recurse. After this method returns the minCapacities map should contain the total minimum
        // capacity required by a requirement type. For example...
        //   1 host {
        //     2 socket {
        //       [3,4] cpu with [50000,100000] capacity
        //       1 cpu with 1000 capacity 
        //     }
        //   }
        // ... the minCapacities map should give back a cpu capacity of 151000 (50000 * 3) + (1 * 1000)
        MultiValuedMap<String, Requirement> requirementChildren = getRequirementChildren(requirement);
        for (Requirement childRequirement : requirementChildren.values()) {
            recursiveCalculateTotals(childRequirement, minCount, minCounts, minCapacities);
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.expression.Expression;
import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.joining;

final class WorkerFinder {
    private WorkerFinder() {
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

        QueryTracker st = new QueryTracker();
        String query = selectRamRequirement(st, hr.getWhereCondition(), hr.getRamRequirements().get(0).getWhereCondition());
        
        System.out.println(query);
    }
    
    private static String selectRamRequirement(QueryTracker st, Expression hostWhere, Expression ramWhere) {
        st.enter();
        try {
            Set<String> hostKeys = HostSpecification.getKeyPropertyNames();
            Set<String> hostProps = new HashSet<>();
            QueryExpressionizer.collectVariableNames(hostWhere, hostProps);            
            String id1 = st.next();
            String qr1 = selectFlattenedProperties(st, "host", hostKeys, hostProps);
            
            Set<String> ramKeys = RamSpecification.getKeyPropertyNames();
            Set<String> ramProps = new HashSet<>();
            QueryExpressionizer.collectVariableNames(ramWhere, ramProps);
            String id2 = st.next();
            String qr2 = selectFlattenedProperties(st, "ram", ramKeys, ramProps);
            
            Set<String> selectFields = new LinkedHashSet<>();
            hostKeys.stream().map(f -> id1 + "." + f + " AS " + "host" + "_" + f).forEachOrdered(selectFields::add);
            hostProps.stream().map(f -> id1 + "." + f + " AS " + "host" + "_" + f).forEachOrdered(selectFields::add);
            ramKeys.stream().map(f -> id2 + "." + f + " AS " + "ram" + "_" + f).forEachOrdered(selectFields::add);
            ramProps.stream().map(f -> id2 + "." + f + " AS " + "ram" + "_" + f).forEachOrdered(selectFields::add);
            String id3 = st.next();
            String qr3 = ""
                    + "SELECT "
                    + selectFields.stream().collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + qr1
                    + "\n) " + id1 + "\n"
                    + "INNER JOIN\n"
                    + "(\n"
                    + qr2
                    + "\n) " + id2 + "\n"
                    + "ON " + hostKeys.stream().map(k -> id1 + "." + k + "=" + id2 + "." + k).collect(joining(" AND "));
            
            return qr3;
        } finally {
            st.exit();
        }
    }
    
    private static String selectPotentialWorkers(QueryTracker st, HostRequirement hostReq) {
        st.enter();
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
            
            String id1 = st.next();
            String qr1 = selectWithinDesiredRange(st, "socket", hostPk, socketNr);
            
            String id2 = st.next();
            String qr2 = selectWithinDesiredRange(st, "gpu", hostPk, gpuNr);
            
            String id3 = st.next();
            String qr3 = selectWithinDesiredRange(st, "mount", hostPk, mountNr);
            
            String id4 = st.next();
            String qr4 = selectWithinDesiredRange(st, "ram", hostPk, ramNr);
            
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
            
             return st.identLines(qr5);
        } finally {
            st.exit();
        }
    } 
    
    private static String selectWithinDesiredRange(QueryTracker st, String spec, Collection<String> groupByCols, NumberRange numberRange) {
        st.enter();
        try {
            String id1 = st.next();
            String qr1 = ""
                    + "SELECT " + groupByCols.stream().map(n -> id1 + "." + n).collect(joining(",")) + ",count(*) AS cnt\n"
                    + "FROM (" + spec + "_spec) " + id1 + "\n"
                    + "GROUP BY " + groupByCols.stream().map(n -> id1 + "." + n).collect(joining(","));
            
            String id2 = st.next();
            String qr2 = ""
                    + "SELECT *\n"
                    + "FROM (\n"
                    + st.identLines(2, qr1)
                    + "\n) " + id2 + "\n"
                    + "WHERE " + id2 + ".cnt >= " + numberRange.getStart();
            
            String id3 = st.next();
            String qr3 = ""
                    + "SELECT " + groupByCols.stream().map(n -> id3 + "." + n).collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + st.identLines(1, qr2)
                    + "\n) " + id3;
            
            return st.identLines(qr3);
        } finally {
            st.exit();
        }
    }
    
    private static String selectFlattenedProperties(QueryTracker st, String spec, Collection<String> pkCols, Collection<String> names) {
        st.enter();
        try {
            String fullId = st.next();
            String fullQuery = "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name" + " FROM " + spec + "_props";
            
            LinkedHashMap<String, String> valCols = new LinkedHashMap<>();
            String output = ""
                    + "(\n"
                    + "  " + fullQuery
                    + "\n) " + fullId; 
            
            for (String name : names) {
                String nextId = st.next();
                String nextQuery = selectByProperty(st, spec, pkCols, name);
                
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
            st.exit();
        }
    }
    
    private static String selectByProperty(QueryTracker st, String spec, Collection<String> pkCols, String name) {
        st.enter();
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
            
            String id1 = st.next();
            String qr1 = "  "
                    + "SELECT DISTINCT " + pkCols.stream().collect(joining(",")) + ",name"
                    + " FROM " + spec + "_props";

            String id2 = st.next();
            String qr2 = "  "
                    + "SELECT " + pkCols.stream().collect(joining(",")) + ",name," + col
                    + " FROM " + spec + "_props"
                    + " WHERE name='" + name + "'";
            
            String id3 = st.next();
            String qr3 = ""
                    + "SELECT " + pkCols.stream().map(x -> id1 + "." + x).collect(joining(",")) + ",name," + id2 + "." + col + " AS val\n"
                    + "FROM\n"
                    + "(\n" + qr1 + "\n) " + id1 + "\n"
                    + "LEFT JOIN\n"
                    + "(\n" + qr2 + "\n) " + id2 + "\n"
                    + "ON " + pkCols.stream().map(x -> id1 + "." + x + "=" + id2 + "." + x).collect(joining(" AND "));
            
            return st.identLines(qr3);
        } finally {
            st.exit();
        }
    }
}

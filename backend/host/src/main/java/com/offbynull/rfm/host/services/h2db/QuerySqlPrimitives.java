package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import static java.util.stream.Collectors.joining;

final class QuerySqlPrimitives {
    private QuerySqlPrimitives() {
        // do nothing
    }
    
    public static String selectWithinDesiredRange(QueryTracker qt, String spec, Collection<String> groupByCols, NumberRange numberRange) {
        qt.enter();
        try {
            String id1 = qt.next();
            String qr1 = ""
                    + "SELECT " + groupByCols.stream().map(n -> id1 + "." + n).collect(joining(",")) + ",count(*) AS cnt\n"
                    + "FROM (" + spec + "_spec) " + id1 + "\n"
                    + "GROUP BY " + groupByCols.stream().map(n -> id1 + "." + n).collect(joining(","));
            
            String id2 = qt.next();
            String qr2 = ""
                    + "SELECT *\n"
                    + "FROM (\n"
                    + qt.identLines(2, qr1)
                    + "\n) " + id2 + "\n"
                    + "WHERE " + id2 + ".cnt >= " + numberRange.getStart();
            
            String id3 = qt.next();
            String qr3 = ""
                    + "SELECT " + groupByCols.stream().map(n -> id3 + "." + n).collect(joining(",")) + "\n"
                    + "FROM (\n"
                    + qt.identLines(1, qr2)
                    + "\n) " + id3;
            
            return qt.identLines(qr3);
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
            
            return qt.identLines(qr3);
        } finally {
            qt.exit();
        }
    }
}

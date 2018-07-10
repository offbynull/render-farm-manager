package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationChildClasses;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationFullKeyFromClasses;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationName;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import javax.sql.DataSource;
import static org.apache.commons.lang3.ClassUtils.isAssignable;

final class WorkerPrimer {
    private WorkerPrimer() {
        // do nothing
    }
    
    public static void prime(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            LinkedList<Class<? extends Specification>> specChainClses = new LinkedList<>();
            specChainClses.add(HostSpecification.class);
            
            // create top-level table (host)
            String pkName = getSpecificationName(specChainClses.getFirst());
            String[] pk = getSpecificationFullKeyFromClasses(specChainClses).stream().toArray(len -> new String[len]);
            createTableSpecSql(conn, pkName, pk, null, null, false, false);
            createTableSpecPropSql(conn, pkName, pk);
            
            // create child tables
            recursiveCreateTable(conn, new LinkedList<>(specChainClses));
            
            // create worker table (essentially a cache of counts and capacities of children)
            LinkedHashSet<String> names = new LinkedHashSet<>();
            LinkedHashSet<String> capacityEnabledNames = new LinkedHashSet<>();
            recursiveCollectNames(specChainClses, names, capacityEnabledNames);
            createTableWorkerSql(conn,
                    names.stream().toArray(len -> new String[len]),
                    capacityEnabledNames.stream().toArray(len -> new String[len]));
        }
    }
    
    private static void recursiveCollectNames(
            LinkedList<Class<? extends Specification>> specChainClses,
            Set<String> names,
            Set<String> capacityEnabledNames) {
        Class<? extends Specification> parentSpecCls = specChainClses.getLast();
        
        for (Class<? extends Specification> childSpecCls : getSpecificationChildClasses(parentSpecCls)) {
            String name = getSpecificationName(childSpecCls);
            
            names.add(name);
            if (isAssignable(childSpecCls, CapacityEnabledSpecification.class)) {
                capacityEnabledNames.add(name);
            }
            
            specChainClses.addLast(childSpecCls);
            recursiveCollectNames(specChainClses, names, capacityEnabledNames);
            specChainClses.removeLast();
        }
    }
    
    private static void recursiveCreateTable(
            Connection conn,
            LinkedList<Class<? extends Specification>> specChain) throws SQLException {
        Class<? extends Specification> parentSpecCls = specChain.getLast();
        
        String fkName = getSpecificationName(parentSpecCls);
        String[] fk = getSpecificationFullKeyFromClasses(specChain).stream().toArray(len -> new String[len]);
        
        // for child spec types
        for (Class<? extends Specification> childSpecCls : getSpecificationChildClasses(parentSpecCls)) {
            specChain.addLast(childSpecCls);
            
            // create tables for child
            String pkName = getSpecificationName(childSpecCls);
            String[] pk = getSpecificationFullKeyFromClasses(specChain).stream().toArray(len -> new String[len]);
            boolean capacityEnabled = isAssignable(childSpecCls, CapacityEnabledSpecification.class);
            createTableSpecSql(conn, pkName, pk, fkName, fk, capacityEnabled, true);
            createTableSpecPropSql(conn, pkName, pk);
            
            // recurse into child
            recursiveCreateTable(conn, specChain);
            
            specChain.removeLast();
        }
    }
    
    private static void createTableSpecSql(Connection conn, String pkName, String[] pk, String fkName, String[] fk,
            boolean capacityEnabled, boolean cached) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            LinkedHashSet<String> columns = new LinkedHashSet<>();
            columns.addAll(asList(pk));
            if (fkName != null && fk != null) {
                columns.addAll(asList(fk));
            }

            List<String> tableStmtElems = new ArrayList<>();

            for (String column : columns) {
                String type;
                switch (column.substring(0, 2)) {
                    case "b_":
                        type = "BOOLEAN";
                        break;
                    case "n_":
                        type = "NUMBER(38,10)";
                        break;
                    case "s_":
                        type = "VARCHAR(2048)";
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                tableStmtElems.add(column + " " + type + " NOT NULL");
            }

            if (capacityEnabled) {
                tableStmtElems.add("capacity NUMBER(38,10) NOT NULL CHECK(capacity >= 0)");
            }

            tableStmtElems.add("PRIMARY KEY(" + stream(pk).collect(joining(", ")) + ")");
            if (fkName != null && fk != null) {
                tableStmtElems.add(""
                        + "FOREIGN KEY(" + stream(fk).collect(joining(", ")) + ") "
                        + "REFERENCES " + fkName + "_spec(" + stream(fk).collect(joining(", ")) + ") "
                        + "ON DELETE CASCADE ON UPDATE CASCADE");
            }

            String table = tableStmtElems.stream().collect(joining(
                    ",\n",
                    "CREATE TABLE IF NOT EXISTS " + pkName + "_spec(\n",
                    "\n)")
            );
            stmt.execute(table);
            
            
            if (cached) {
                String countTriggerClsName = CountTrigger.class.getTypeName();
                String countTrigger = ""
                        + "CREATE TRIGGER IF NOT EXISTS " + pkName + "_spec_count_trigger\n"
                        + "AFTER UPDATE ON " + pkName + "_spec\n"
                        + "FOR EACH ROW AS $$org.h2.api.Trigger create() {\n"
                        + "  return new " + countTriggerClsName + "(\"" + pkName + "\");\n"
                        + "} $$";
                stmt.execute(countTrigger);
            }

            
            if (cached && capacityEnabled) {
                int capacityIdxInRow = columns.size(); // idx of capacity column -- it's added right after all the items in columns list
                String capacityTriggerClsName = CapacityTrigger.class.getName();
                String capacityTrigger = ""
                        + "CREATE TRIGGER IF NOT EXISTS " + pkName + "_spec_capacity_trigger\n"
                        + "AFTER UPDATE ON " + pkName + "_spec\n"
                        + "FOR EACH ROW AS $$org.h2.api.Trigger create() {\n"
                        + "  return new " + capacityTriggerClsName + "(" + capacityIdxInRow + ", \"" + pkName + "\");\n"
                        + "} $$";
                stmt.execute(capacityTrigger);
            }
        }
    }

    private static void createTableSpecPropSql(Connection conn, String pkName, String[] pk) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            List<String> tableStmtElems = new ArrayList<>();

            for (String column : pk) {
                String type;
                switch (column.substring(0, 2)) {
                    case "b_":
                        type = "BOOLEAN";
                        break;
                    case "n_":
                        type = "NUMBER(38,10)";
                        break;
                    case "s_":
                        type = "VARCHAR(2048)";
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                tableStmtElems.add(column + " " + type + "  NOT NULL");
            }
            tableStmtElems.add("name VARCHAR(2048) NOT NULL");
            tableStmtElems.add("val_b BOOLEAN");
            tableStmtElems.add("val_n NUMBER(38,10)");
            tableStmtElems.add("val_s VARCHAR(2048)");
            tableStmtElems.add("CONSTRAINT " + pkName + "_prop_valchk CHECK(\n"
                    + " (val_b != NULL AND val_n = NULL AND val_s = NULL) OR\n"
                    + " (val_b = NULL AND val_n != NULL AND val_s = NULL) OR\n"
                    + " (val_b = NULL AND val_n = NULL AND val_s != NULL)\n"
                    + ")");

            tableStmtElems.add("PRIMARY KEY(" + stream(pk).collect(joining(", ")) + ", name)");
            tableStmtElems.add(""
                    + "FOREIGN KEY(" + stream(pk).collect(joining(", ")) + ") "
                    + "REFERENCES " + pkName + "_spec(" + stream(pk).collect(joining(", ")) + ") "
                    + "ON DELETE CASCADE ON UPDATE CASCADE");

            String table = tableStmtElems.stream().collect(Collectors.joining(
                    ",\n",
                    "CREATE TABLE IF NOT EXISTS " + pkName + "_prop(\n",
                    "\n)")
            );
            stmt.execute(table);
        }
    }
    
    private static void createTableWorkerSql(Connection conn, String[] capacityNames, String[] countNames) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            List<String> tableStmtElems = new ArrayList<>();

            tableStmtElems.add("s_host VARCHAR(2048) NOT NULL");
            tableStmtElems.add("n_port NUMBER(38,10) NOT NULL");
            for (String capacityName : capacityNames) {
                tableStmtElems.add(capacityName + "_capacity_sum NUMBER(38,10) NOT NULL CHECK(" + capacityName + "_capacity_sum >= 0)");
            }
            for (String countName : countNames) {
                tableStmtElems.add(countName + "_count NUMBER(38,10) NOT NULL CHECK(" + countName + "_count >= 0)");
            }
            tableStmtElems.add("PRIMARY KEY(s_host,n_port)");
            tableStmtElems.add("FOREIGN KEY(s_host,n_port) REFERENCES host_spec(s_host,n_port) ON DELETE CASCADE ON UPDATE CASCADE");

            String table = tableStmtElems.stream().collect(Collectors.joining(
                    ",\n",
                    "CREATE TABLE IF NOT EXISTS worker(\n",
                    "\n)")
            );
            stmt.execute(table);
        }
    }
}

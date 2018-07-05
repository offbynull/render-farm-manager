package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import javax.sql.DataSource;
import static org.apache.commons.lang3.StringUtils.capitalize;

final class WorkerPrimer {
    private WorkerPrimer() {
        // do nothing
    }
    
    public static void prime(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            createTableSpecSql(conn,
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    null, null,
                    false);
            createTableSpecPropSql(conn,
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "socket", SocketSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    false);
            createTableSpecPropSql(conn,
                    "socket", SocketSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "core", CoreSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "socket", SocketSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    false);
            createTableSpecPropSql(conn,
                    "core", CoreSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "cpu", CpuSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "core", CoreSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    true);
            createTableSpecPropSql(conn,
                    "cpu", CpuSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "gpu", GpuSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    true);
            createTableSpecPropSql(conn,
                    "gpu", GpuSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "mount", MountSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    true);
            createTableSpecPropSql(conn,
                    "mount", MountSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableSpecSql(conn,
                    "ram", RamSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    "host", HostSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]),
                    true);
            createTableSpecPropSql(conn,
                    "ram", RamSpecification.getKeyPropertyNames().stream().toArray(i -> new String[i]));
            
            createTableWorkerSql(conn,
                    new String[] { "cpu", "gpu", "mount", "ram" },
                    new String[] { "host", "socket", "core", "cpu", "gpu", "mount", "ram" });
        }
    }
    
    private static void createTableSpecSql(Connection conn, String name, String[] pks, String fkTable, String[] fks,
            boolean capacityEnabled) throws SQLException {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        columns.addAll(asList(pks));
        if (fkTable != null && fks != null) {
            columns.addAll(asList(fks));
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
         
        tableStmtElems.add("PRIMARY KEY(" + stream(pks).collect(joining(", ")) + ")");
        if (fkTable != null && fks != null) {
            tableStmtElems.add(""
                    + "FOREIGN KEY(" + stream(fks).collect(joining(", ")) + ") "
                    + "REFERENCES " + fkTable + "_spec(" + stream(fks).collect(joining(", ")) + ") "
                    + "ON DELETE CASCADE ON UPDATE CASCADE");
        }
        
        String table = tableStmtElems.stream().collect(joining(
                ",\n",
                "CREATE TABLE IF NOT EXISTS " + name + "_spec(\n",
                "\n)")
        );
        
        String countTriggerClsName = CapacityTrigger.class.getName() + capitalize(name);
        String countTrigger = "CREATE TRIGGER IF NOT EXISTS " + name + "_spec_capacity_trigger"
                    + " AFTER UPDATE ON " + name + "_spec"
                    + " FOR EACH ROW CALL \"" + countTriggerClsName + "\"";
        
        String capacityTriggerClsName = CapacityTrigger.class.getName() + capitalize(name);
        String capacityTrigger = "CREATE TRIGGER IF NOT EXISTS " + name + "_spec_capacity_trigger"
                    + " AFTER UPDATE ON " + name + "_spec"
                    + " FOR EACH ROW CALL \"" + capacityTriggerClsName + "\"";
                
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
            stmt.execute(countTrigger);
            if (capacityEnabled) {
                stmt.execute(capacityTrigger);
            }
        }
    }

    private static void createTableSpecPropSql(Connection conn, String name, String[] pk) throws SQLException {
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
        tableStmtElems.add("CONSTRAINT " + name + "_prop_valchk CHECK(\n"
                + " (val_b != NULL AND val_n = NULL AND val_s = NULL) OR\n"
                + " (val_b = NULL AND val_n != NULL AND val_s = NULL) OR\n"
                + " (val_b = NULL AND val_n = NULL AND val_s != NULL)\n"
                + ")");

        tableStmtElems.add("PRIMARY KEY(" + stream(pk).collect(joining(", ")) + ", name)");
        tableStmtElems.add(""
                + "FOREIGN KEY(" + stream(pk).collect(joining(", ")) + ") "
                + "REFERENCES " + name + "_spec(" + stream(pk).collect(joining(", ")) + ") "
                + "ON DELETE CASCADE ON UPDATE CASCADE");
        
        String table = tableStmtElems.stream().collect(Collectors.joining(
                ",\n",
                "CREATE TABLE IF NOT EXISTS " + name + "_prop(\n",
                "\n)")
        );
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
        }
    }
    
    private static void createTableWorkerSql(Connection conn, String[] capacityNames, String[] countNames) throws SQLException {
        List<String> tableStmtElems = new ArrayList<>();

        tableStmtElems.add("s_host VARCHAR(2048) NOT NULL");
        tableStmtElems.add("n_port BIGDECIMAL(38,10) NOT NULL");
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
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
        }
    }
}

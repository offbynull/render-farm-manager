package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.io.IOException;
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

final class H2dbSetupDataUtils {
    private H2dbSetupDataUtils() {
        // do nothing
    }
    
    public static void create(DataSource dataSource) throws IOException {
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
            
            createTableWorkSql(conn);
            createTableWorkTagSql(conn);
            createTableWorkParentSql(conn);
            createTableWorkWriteTimeSql(conn);
            
            createTableBindSql(conn);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
    
    private static void createTableSpecSql(Connection conn, String name, String[] pks, String fkTable, String[] fks,
            boolean capacityEnabled) throws IOException {
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
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableSpecPropSql(Connection conn, String name, String[] pk) throws IOException {
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
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
    
    private static void createTableWorkerSql(Connection conn, String[] capacityNames, String[] countNames) throws IOException {
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
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableWorkSql(Connection conn) throws IOException {
        String table = ""
                + "CREATE TABLE IF NOT EXISTS work(\n"
                + "id VARCHAR(2048) NOT NULL,\n"
                + "priority BIGDECIMAL(38,10) NOT NULL,\n"
                + "script VARCHAR(2048) NOT NULL,\n"
                + "state VARCHAR(2048) NOT NULL CHECK(state='PAUSED' or state='WAITING' or state='RUNNING'),\n"
                + "PRIMARY KEY(id)\n"
                + ")";
        String index1 = "CREATE INDEX IF NOT EXISTS work_priority_idx ON TABLE work(priority)";
        String index2 = "CREATE INDEX IF NOT EXISTS work_id_priority_idx ON TABLE work(id, priority)";
        String index3 = "CREATE INDEX IF NOT EXISTS work_state_idx ON TABLE work(state)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
            stmt.execute(index1);
            stmt.execute(index2);
            stmt.execute(index3);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableWorkTagSql(Connection conn) throws IOException {
        String table = ""
                + "CREATE TABLE IF NOT EXISTS work_tag(\n"
                + "id VARCHAR(2048) NOT NULL,\n"
                + "name VARCHAR(2048) NOT NULL,\n"
                + "val_b BOOLEAN,\n"
                + "val_n NUMBER(38,10),\n"
                + "val_s VARCHAR(2048),\n"
                + "CONSTRAINT work_tag_valchk CHECK(\n"
                + " (val_b != NULL AND val_n = NULL AND val_s = NULL) OR\n"
                + " (val_b = NULL AND val_n != NULL AND val_s = NULL) OR\n"
                + " (val_b = NULL AND val_n = NULL AND val_s != NULL),\n"
                + "PRIMARY KEY(id, name),\n"
                + "FOREIGN KEY(id) REFERENCES work(id) ON DELETE CASCADE ON UPDATE CASCADE\n"
                + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableWorkParentSql(Connection conn) throws IOException {
        String table = ""
                + "CREATE TABLE IF NOT EXISTS work_parent(\n"
                + "id VARCHAR(2048) NOT NULL,\n"
                + "parent_id VARCHAR(2048) NOT NULL,\n"
                + "FOREIGN KEY(id) REFERENCES work(id) ON DELETE CASCADE ON UPDATE CASCADE,\n"
                + "FOREIGN KEY(parent_id) REFERENCES work(id) ON DELETE CASCADE ON UPDATE CASCADE\n"
                + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableWorkWriteTimeSql(Connection conn) throws IOException {
        String table = ""
                + "CREATE TABLE IF NOT EXISTS work_writetime(\n"
                + "id INTEGER NOT NULL,\n"
                + "write_time TIMESTAMP WITH TIME ZONE NOT NULL,\n"
                + "priority NUMBER(38,10) NOT NULL,\n"
                + "CONSTRAINT work_dependency_singlechk CHECK(id = 0),\n"
                + "PRIMARY KEY(id)\n"
                + ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static void createTableBindSql(Connection conn) throws IOException {
        String table = ""
                + "CREATE TABLE IF NOT EXISTS bind(\n"
                + "work_id VARCHAR(2048) NOT NULL\n,"
                + "host VARCHAR(2048) NOT NULL\n,"
                + "port NUMBER(38,10) NOT NULL\n,"
                + "PRIMARY KEY(work_id, host, port)\n"
                + "FOREIGN KEY(work_id) REFERENCES work(id) ON DELETE NO ACTION ON UPDATE NO ACTION,\n"
                + "FOREIGN KEY(host, port) REFERENCES host_spec(s_host, n_port) ON DELETE NO ACTION ON UPDATE NO ACTION\n"
                + ")";
        String index1 = "CREATE INDEX IF NOT EXISTS bind_work_idx1 ON TABLE bind(work_id)";
        String index2 = "CREATE INDEX IF NOT EXISTS bind_work_idx2 ON TABLE bind(host,port)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(table);
            stmt.execute(index1);
            stmt.execute(index2);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
}

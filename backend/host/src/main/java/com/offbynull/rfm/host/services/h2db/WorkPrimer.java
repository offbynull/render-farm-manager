package com.offbynull.rfm.host.services.h2db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

final class WorkPrimer {
    private WorkPrimer() {
        // do nothing
    }
    
    public static void prime(DataSource dataSource) throws IOException {
        try (Connection conn = dataSource.getConnection()) {
            createTableWorkSql(conn);
            createTableWorkTagSql(conn);
            createTableWorkParentSql(conn);
            createTableWorkWriteTimeSql(conn);
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
}

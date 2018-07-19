package com.offbynull.rfm.host.services.h2db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.lang3.Validate;

final class WorkerDeleter {
    private WorkerDeleter() {
        // do nothing
    }
    
    public static void deleteWorker(Connection conn, String host, int port) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        
        // will delete worker
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM host_spec WHERE s_host=? AND n_port=?")) {
            ps.setString(1, host);
            ps.setBigDecimal(2, BigDecimal.valueOf(port));
            ps.executeUpdate();
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Worker;
import java.io.Closeable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_SCROLL_SENSITIVE;
import java.sql.SQLException;
import org.apache.commons.lang3.Validate;

// This lock should only be used when creating/deleting/writing, to stop writes from clobbering over eachother.
//
// You need to be extra careful not to commit until AFTER you've closed this lock. If you commit while the lock is still active, the lock
// automatically goes away.
final class WorkerLock implements Closeable {
    private final String host;
    private final BigDecimal port;
    
    private final PreparedStatement sfuLockPs;
    private final PreparedStatement dLockPs;
    private final PreparedStatement scWorkerPs;
    private final ResultSet sfuLockRs;

    private WorkerLock(String host, BigDecimal port, PreparedStatement sfuLockPs, ResultSet sfuLockRs, PreparedStatement dLockPs,
            PreparedStatement scWorkerPs) {
        this.sfuLockPs = sfuLockPs;
        this.sfuLockRs = sfuLockRs;
        this.dLockPs = dLockPs;
        this.scWorkerPs = scWorkerPs;
        this.host = host;
        this.port = port;
    }
    
    public static WorkerLock lock(Connection conn, Worker worker) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(worker);
        
        String host = (String) worker.getHostSpecification().getProperties().get("s_host");
        int port = ((BigDecimal) worker.getHostSpecification().getProperties().get("n_port")).intValueExact();
        return lock(conn, host, port);
    }
    
    public static WorkerLock lock(Connection conn, String host, int port) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        Validate.isTrue(conn.getAutoCommit() == false);
        Validate.isTrue(conn.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED);
        
        BigDecimal portBd = BigDecimal.valueOf(port);
        
        PreparedStatement sfuLockPs = null;
        ResultSet sfuLockRs = null;
        PreparedStatement scWorkerPs = null;
        PreparedStatement dLockPs = null;
        try {
            sfuLockPs = conn.prepareStatement("SELECT * FROM worker_lock WHERE s_host=? AND n_port=? FOR UPDATE",
                    TYPE_SCROLL_SENSITIVE, CONCUR_UPDATABLE);
            sfuLockPs.setString(1, host);
            sfuLockPs.setBigDecimal(2, portBd);
            sfuLockRs = sfuLockPs.executeQuery();
            boolean found = sfuLockRs.next();

            if (!found) {
                sfuLockRs.moveToInsertRow();
                sfuLockRs.updateString(1, host);
                sfuLockRs.updateBigDecimal(2, portBd);
                sfuLockRs.insertRow();
            }

            dLockPs = conn.prepareStatement("DELETE FROM worker_lock WHERE s_host=? AND n_port=?");
            
            scWorkerPs = conn.prepareStatement("SELECT * FROM worker WHERE s_host=? AND n_port=?");

            return new WorkerLock(host, portBd, sfuLockPs, sfuLockRs, dLockPs, scWorkerPs);
        } catch (RuntimeException | SQLException e) {
            if (sfuLockRs != null) {
                try {
                    sfuLockRs.close();
                } catch (SQLException sqle) {
                    // do nothing
                }
            }
            if (sfuLockPs != null) {
                try {
                    sfuLockPs.close();
                } catch (SQLException sqle) {
                    // do nothing
                }
            }
            if (dLockPs != null) {
                try {
                    dLockPs.close();
                } catch (SQLException sqle) {
                    // do nothing
                }
            }
            if (scWorkerPs != null) {
                try {
                    scWorkerPs.close();
                } catch (SQLException sqle) {
                    // do nothing
                }
            }
            throw e;
        }
    }

    @Override
    public void close() {
        // Check to see if there's a worker associated with this lock. If there is no worker, delete the lock row -- there's no reason it
        // should exist and keeping it around will accrue trash in the db.
        try {
            scWorkerPs.setString(1, host);
            scWorkerPs.setBigDecimal(2, port);
            boolean deleteLockRow;
            try (ResultSet rs = scWorkerPs.executeQuery()) {
                deleteLockRow = !rs.next();
            }
            
            if (deleteLockRow) {
                dLockPs.setString(1, host);
                dLockPs.setBigDecimal(2, port);
                dLockPs.executeUpdate();
            }
        } catch (SQLException sqle) {
            // do nothing
        }
        
        
        try {
            sfuLockRs.close();
        } catch (SQLException sqle) {
            // do nothing
        }
        
        try {
            sfuLockPs.close();
        } catch (SQLException sqle) {
            // do nothing
        }
        
        try {
            dLockPs.close();
        } catch (SQLException sqle) {
            // do nothing
        }
        
        try {
            scWorkerPs.close();
        } catch (SQLException sqle) {
            // do nothing
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Work;
import java.io.Closeable;
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
final class WorkLock implements Closeable {
    private final String id;
    
    private final PreparedStatement sfuLockPs;
    private final PreparedStatement dLockPs;
    private final PreparedStatement scWorkPs;
    private final ResultSet sfuLockRs;

    private WorkLock(String id, PreparedStatement sfuLockPs, ResultSet sfuLockRs, PreparedStatement dLockPs, PreparedStatement scWorkPs) {
        this.sfuLockPs = sfuLockPs;
        this.sfuLockRs = sfuLockRs;
        this.dLockPs = dLockPs;
        this.scWorkPs = scWorkPs;
        this.id = id;
    }
    
    public static WorkLock lock(Connection conn, Work work) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(work);
        
        String id = work.getId();
        return lock(conn, id);
    }
    
    public static WorkLock lock(Connection conn, String id) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(id);
        Validate.notEmpty(id);
        Validate.isTrue(conn.getAutoCommit() == false);
        Validate.isTrue(conn.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED);
        
        PreparedStatement sfuLockPs = null;
        ResultSet sfuLockRs = null;
        PreparedStatement scWorkPs = null;
        PreparedStatement dLockPs = null;
        try {
            sfuLockPs = conn.prepareStatement("SELECT * FROM work_lock WHERE id=? FOR UPDATE",
                    TYPE_SCROLL_SENSITIVE, CONCUR_UPDATABLE);
            sfuLockPs.setString(1, id);
            sfuLockRs = sfuLockPs.executeQuery();
            boolean found = sfuLockRs.next();

            if (!found) {
                sfuLockRs.moveToInsertRow();
                sfuLockRs.updateString(1, id);
                sfuLockRs.insertRow();
            }

            dLockPs = conn.prepareStatement("DELETE FROM work_lock WHERE id=?");
            
            scWorkPs = conn.prepareStatement("SELECT * FROM work WHERE id=?");

            return new WorkLock(id, sfuLockPs, sfuLockRs, dLockPs, scWorkPs);
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
            if (scWorkPs != null) {
                try {
                    scWorkPs.close();
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
            scWorkPs.setString(1, id);
            boolean deleteLockRow;
            try (ResultSet rs = scWorkPs.executeQuery()) {
                deleteLockRow = !rs.next();
            }
            
            if (deleteLockRow) {
                dLockPs.setString(1, id);
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
            scWorkPs.close();
        } catch (SQLException sqle) {
            // do nothing
        }
    }
}

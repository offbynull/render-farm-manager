package com.offbynull.rfm.host.services.h2db;

import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.h2.api.Trigger;

abstract class CountTrigger implements Trigger {
    private final String name;

    public CountTrigger(String name) {
        Validate.notNull(name);
        Validate.notEmpty(name);
        Validate.isTrue(StringUtils.containsOnly(name, "abcdefghijklmnopqrstuvwxyz"));
        this.name = name;
    }

    @Override
    public void init(Connection conn, String schemaName,
                String triggerName, String tableName, boolean before, int type) throws SQLException {
        // do nothing
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (oldRow != null && newRow == null) {
            Validate.isTrue(oldRow.length >= 2);
            Validate.isTrue(oldRow[0] instanceof String);
            Validate.isTrue(oldRow[1] instanceof BigDecimal);
        } else if (oldRow == null && newRow != null) {
            Validate.isTrue(newRow.length >= 2);
            Validate.isTrue(newRow[0] instanceof String);
            Validate.isTrue(newRow[1] instanceof BigDecimal);
        } else {
            throw new IllegalArgumentException();
        }
        Validate.isTrue(conn.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED); // MUST be set to readcommitted
        
        
        String host = (String) newRow[0];
        BigDecimal port = (BigDecimal) newRow[1];
        
        // What does this loop do? It...
        //   1. get the existing summed count from the WORKER table
        //   2. apply the change (+1 if a row is being added, -1 if a row is being deleted)
        //   3. put the new summed capacity into the WORKER table
        // and repeats if the change wasn't applied.
        //
        // Remember that the update to the WORKER isn't being done atomically. If 2+ updates are done at the same time, one of them will end
        // up overwriting the other one. That's why the actual UPDATE operation at the end of the loop has a WHERE clause that ensures the
        // old value is the same as when it was read -- if it isn't, the upate won't be applied and it'll retry.
        while (true) {
            BigDecimal difference;
            if (oldRow != null && newRow == null) {
                // row deleted, diff is -1
                difference = ONE.negate();
            } else if (oldRow == null && newRow != null) {
                // row inserted, diff is 1
                difference = ONE;
            } else {
                throw new IllegalStateException(); // sanity check -- should never happen, we check above
            }

            BigDecimal oldSummedCount;
            BigDecimal newSummedCount;

            // step 1
            String selectOldSummedCapacityStmt = "SELECT " + name + "_count FROM worker WHERE s_host=? AND n_port=?";
            try (PreparedStatement ps = conn.prepareStatement(selectOldSummedCapacityStmt)) {
                ps.setString(1, host);
                ps.setBigDecimal(2, port);
                try (ResultSet rs = ps.executeQuery()) {
                    Validate.validState(rs.next(), "Row is missing from worker table"); // should never occur due to chain of FK constraints
                    oldSummedCount = rs.getBigDecimal(1);
                }
            }

            // step 2
            newSummedCount = oldSummedCount.add(difference);
            
            // step 3
            boolean changeApplied;
            String updateStmt = "UPDATE worker SET " + name + "_count=? WHERE s_host=? AND n_port=? AND " + name + "_count=?";
            try (PreparedStatement ps = conn.prepareStatement(updateStmt)) {
                ps.setBigDecimal(1, newSummedCount);
                ps.setString(2, host);
                ps.setBigDecimal(3, port);
                ps.setBigDecimal(4, oldSummedCount);
                changeApplied = ps.executeUpdate() != 0;
            }
            
            if (changeApplied) {
                break;
            }
        }
    }

    @Override
    public void close() throws SQLException {
        // do nothing
    }

    @Override
    public void remove() throws SQLException {
        // do nothing
    }
    
}

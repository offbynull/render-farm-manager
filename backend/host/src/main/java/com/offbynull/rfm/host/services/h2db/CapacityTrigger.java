package com.offbynull.rfm.host.services.h2db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.h2.api.Trigger;

abstract class CapacityTrigger implements Trigger {
    private final int idx;
    private final String name;

    public CapacityTrigger(int idx, String name) {
        Validate.notNull(name);
        Validate.isTrue(idx >= 2); // starts with s_host and n_port, so idx of capacity column can't be 0 or 1
        Validate.notEmpty(name);
        Validate.isTrue(StringUtils.containsOnly(name, "abcdefghijklmnopqrstuvwxyz"));
        this.idx = idx;
        this.name = name;
    }

    @Override
    public void init(Connection conn, String schemaName,
                String triggerName, String tableName, boolean before, int type) throws SQLException {
        // do nothing
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        Validate.notNull(oldRow);
        Validate.notNull(newRow);
        Validate.isTrue(oldRow.length >= 2);
        Validate.isTrue(idx < oldRow.length);
        Validate.isTrue(oldRow[0] instanceof String);
        Validate.isTrue(oldRow[1] instanceof BigDecimal);
        Validate.isTrue(newRow.length >= 2);
        Validate.isTrue(idx < newRow.length);
        Validate.isTrue(newRow[0] instanceof String);
        Validate.isTrue(newRow[1] instanceof BigDecimal);
        Validate.isTrue(conn.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED); // MUST be set to readcommitted
        
        
        String host = (String) newRow[0];
        BigDecimal port = (BigDecimal) newRow[1];
        
        // What does this loop do? It...
        //   1. get the existing summed capacity from the WORKER table
        //   2. apply the capacity change (subtract old row's capacity and add new row's capacity)
        //   3. put the new summed capacity into the WORKER table
        // and repeats if the change wasn't applied.
        //
        // Remember that the update to the WORKER isn't being done atomically. If 2+ updates are done at the same time, one of them will end
        // up overwriting the other one. That's why the actual UPDATE operation at the end of the loop has a WHERE clause that ensures the
        // old value is the same as when it was read -- if it isn't, the upate won't be applied and it'll retry.
        while (true) {
            BigDecimal oldCapacity = (BigDecimal) oldRow[idx];
            BigDecimal newCapacity = (BigDecimal) newRow[idx];
            BigDecimal oldSummedCapacity;
            BigDecimal newSummedCapacity;

            // step 1
            String selectOldSummedCapacityStmt = "SELECT " + name + "_capacity_sum FROM worker WHERE s_host=? AND n_port=?";
            try (PreparedStatement ps = conn.prepareStatement(selectOldSummedCapacityStmt)) {
                ps.setString(1, host);
                ps.setBigDecimal(2, port);
                try (ResultSet rs = ps.executeQuery()) {
                    Validate.validState(rs.next(), "Row is missing from worker table"); // should never occur due to chain of FK constraints
                    oldSummedCapacity = rs.getBigDecimal(1);
                }
            }

            // step 2
            newSummedCapacity = oldSummedCapacity;
            newSummedCapacity = newSummedCapacity.subtract(oldCapacity);
            newSummedCapacity = newSummedCapacity.add(newCapacity);
            
            // step 3
            boolean changeApplied;
            String updateStmt = "UPDATE worker SET " + name + "_capacity_sum=? WHERE s_host=? AND n_port=? AND " + name + "_capacity_sum=?";
            try (PreparedStatement ps = conn.prepareStatement(updateStmt)) {
                ps.setBigDecimal(1, newSummedCapacity);
                ps.setString(2, host);
                ps.setBigDecimal(3, port);
                ps.setBigDecimal(4, oldSummedCapacity);
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

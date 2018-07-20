package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Direction;
import static com.offbynull.rfm.host.service.Direction.BACKWARD;
import static com.offbynull.rfm.host.service.Direction.FORWARD;
import com.offbynull.rfm.host.services.h2db.InternalUtils.DecomposedWorkerKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.fromWorkerKey;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class WorkScanner {
    
    static List<String> scanWorkers(Connection conn, Direction direction, int max) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(direction);
        Validate.isTrue(max >= 0);
        
        String selectWorkStr = "select id from work";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " order by id asc";
                break;
            case BACKWARD:
                selectWorkStr += " order by id desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }
        selectWorkStr += " limit ?";
        
        try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkIdPs.setInt(1, max);
            return executeAndRead(selectWorkIdPs);
        }
    }
    
    static List<String> scanWorkers(Connection conn, String lastKey, Direction direction, int max) throws SQLException, IOException {
        Validate.notNull(conn);
        Validate.notNull(lastKey);
        Validate.notNull(direction);
        Validate.notEmpty(lastKey);
        Validate.isTrue(max >= 0);
        
        DecomposedWorkerKey decomposedLastKey = fromWorkerKey(lastKey);
        String lastHost = decomposedLastKey.getHost();
        int lastPort = decomposedLastKey.getPort();
        
        String selectWorkStr = "select id from work";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " where id>? order by id asc";
                break;
            case BACKWARD:
                selectWorkStr += " where id<? order by id desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }
        selectWorkStr += " limit ?";

        try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkIdPs.setString(1, lastHost);
            selectWorkIdPs.setBigDecimal(2, BigDecimal.valueOf(lastPort));
            selectWorkIdPs.setString(3, lastHost);
            selectWorkIdPs.setInt(4, max);
            return executeAndRead(selectWorkIdPs);
        }
    }

    private static List<String> executeAndRead(PreparedStatement selectWorkIdPs) throws SQLException {
        try (ResultSet selectWorkIdRs = selectWorkIdPs.executeQuery()) {
            List<String> ret = new LinkedList<>();
            while (selectWorkIdRs.next()) {
                String id = selectWorkIdRs.getString("id");
                String key = id;
                ret.add(key);
            }
            return ret;
        }
    }
}

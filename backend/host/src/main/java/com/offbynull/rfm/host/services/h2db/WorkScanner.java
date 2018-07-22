package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Direction;
import static com.offbynull.rfm.host.service.Direction.BACKWARD;
import static com.offbynull.rfm.host.service.Direction.FORWARD;
import com.offbynull.rfm.host.services.h2db.InternalUtils.DecomposedWorkCursor;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.toWorkCursor;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.fromWorkCursor;

final class WorkScanner {
    
    private WorkScanner() {
        // do nothing
    }
    
    static List<String> scanWorks(Connection conn, Direction direction, int max) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(direction);
        Validate.isTrue(max >= 0);
        
        String selectWorkStr = "select id,priority from work";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " order by priority asc,id asc";
                break;
            case BACKWARD:
                selectWorkStr += " order by priority desc,id desc";
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
    
    static List<String> scanWorks(Connection conn, String cursor, Direction direction, int max) throws SQLException, IOException {
        Validate.notNull(conn);
        Validate.notNull(cursor);
        Validate.notNull(direction);
        Validate.notEmpty(cursor);
        Validate.isTrue(max >= 0);
        
        DecomposedWorkCursor decomposedCursor = fromWorkCursor(cursor);
        String id = decomposedCursor.getId();
        BigDecimal priority = decomposedCursor.getPriority();
        
        String selectWorkStr = "select priority,id from work";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " where (priority=? and id>?) or (priority>?) order by priority asc,id asc";
                break;
            case BACKWARD:
                selectWorkStr += " where (priority=? and id<?) or (priority<?) order by priority desc,id desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }
        selectWorkStr += " limit ?";

        try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkIdPs.setBigDecimal(1, priority);
            selectWorkIdPs.setString(2, id);
            selectWorkIdPs.setBigDecimal(3, priority);
            selectWorkIdPs.setInt(4, max);
            return executeAndRead(selectWorkIdPs);
        }
    }

    private static List<String> executeAndRead(PreparedStatement selectWorkIdPs) throws SQLException {
        try (ResultSet selectWorkCursorRs = selectWorkIdPs.executeQuery()) {
            List<String> ret = new LinkedList<>();
            while (selectWorkCursorRs.next()) {
                BigDecimal priority = selectWorkCursorRs.getBigDecimal("priority");
                String id = selectWorkCursorRs.getString("id");
                String cursor = toWorkCursor(priority, id);
                ret.add(cursor);
            }
            return ret;
        }
    }
}

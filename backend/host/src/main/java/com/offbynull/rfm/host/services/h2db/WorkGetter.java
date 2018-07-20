package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Work;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class WorkGetter {
    private WorkGetter() {
        // do nothing
    }

    public static Work getWork(Connection conn, String id) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(id);
        Validate.notEmpty(id);

        InternalCore core = readCore(conn, id);
        Map<String, Object> tags = readTags(conn, id);
        Set<String> parentIds = readParents(conn, id);
        
        return new Work(id, core.priority, parentIds, tags, core.requirementsScript);
    }
    
    private static InternalCore readCore(Connection conn, String id) throws SQLException {
        String selectWorkStr = "select priority,script from work where id=?";
        try (PreparedStatement selectWorkPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkPs.setString(1, id);
            try (ResultSet rs = selectWorkPs.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                
                BigDecimal priority = rs.getBigDecimal("priority");
                String requirementsScript = rs.getString("script");
                Validate.validState(priority != null);
                Validate.validState(requirementsScript != null);
                
                return new InternalCore(priority, requirementsScript);
            }
        }
    }
    
    private static Map<String, Object> readTags(Connection conn, String id) throws SQLException {
        String selectWorkStr = "select name,val_b,val_n,val_s from work_tag where id=?";
        try (PreparedStatement selectWorkPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkPs.setString(1, id);
            try (ResultSet rs = selectWorkPs.executeQuery()) {
                Map<String, Object> tags = new HashMap<>();
                while (rs.next()) {
                    String name = rs.getString("name");
                    Object value;
                    
                    Validate.validState(name.length() >= 2);
                    switch (name.substring(0, 2)) {
                        case "b_":
                            value = rs.getBoolean("val_b");
                            break;
                        case "n_":
                            value = rs.getBigDecimal("val_n");
                            break;
                        case "s_":
                            value = rs.getString("val_s");
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    
                    tags.put(name, value);
                }
                return tags;
            }
        }
    }
    
    private static Set<String> readParents(Connection conn, String id) throws SQLException {
        String selectWorkStr = "select parent_id from work_parent where id=?";
        try (PreparedStatement selectWorkPs = conn.prepareStatement(selectWorkStr)) {
            selectWorkPs.setString(1, id);
            try (ResultSet rs = selectWorkPs.executeQuery()) {
                Set<String> parents = new HashSet<>();
                while (rs.next()) {
                    String parentId = rs.getString("parent_id");
                    Validate.validState(parentId != null);
                    
                    parents.add(parentId);
                }
                return parents;
            }
        }
    }
    
    private static final class InternalCore {
        private final BigDecimal priority;
        private final String requirementsScript;

        public InternalCore(BigDecimal priority, String requirementsScript) {
            this.priority = priority;
            this.requirementsScript = requirementsScript;
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Work;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;

final class WorkSetter {
    private WorkSetter() {
        // do nothing
    }
 
    public static void setWork(Connection conn, Work work) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(work);

        String deleteWorkStr = "delete from work where id=?";
        try (PreparedStatement deleteWorkPs = conn.prepareStatement(deleteWorkStr)) {
            deleteWorkPs.setString(1, work.getId());
            deleteWorkPs.executeUpdate();
        }

        String insertWorkStr = "insert into work values(?,?,?)";
        try (PreparedStatement insertWorkPs = conn.prepareStatement(insertWorkStr)) {
            insertWorkPs.setString(1, work.getId());
            insertWorkPs.setBigDecimal(2, work.getPriority());
            insertWorkPs.setString(3, work.getRequirementsScript());
            insertWorkPs.executeUpdate();
        }

        String insertWorkParentStr = "insert into work_parent values(?,?)";
        try (PreparedStatement insertWorkParentPs = conn.prepareStatement(insertWorkParentStr)) {
            for (String parentId : work.getParents()) {
                insertWorkParentPs.setString(1, work.getId());
                insertWorkParentPs.setString(2, parentId);
                insertWorkParentPs.executeUpdate();
            }
        }

        String insertWorkTagStr = "insert into work_tag values(?,?,?,?,?)";
        try (PreparedStatement insertWorkTagPs = conn.prepareStatement(insertWorkTagStr)) {
            for (Entry<String, Object> tag : work.getTags().entrySet()) {
                insertWorkTagPs.setString(1, work.getId());
                
                String name = tag.getKey();
                Object val = tag.getValue();
                
                Validate.validState(name.length() >= 2);
                
                Boolean bVal = null;
                BigDecimal nVal = null;
                String sVal = null;
                switch (name.substring(0, 2)) {
                    case "b_":
                        Validate.validState(val instanceof Boolean);
                        bVal = (Boolean) val;
                        break;
                    case "n_":
                        Validate.validState(val instanceof BigDecimal);
                        nVal = (BigDecimal) val;
                        break;
                    case "s_":
                        Validate.validState(val instanceof String);
                        sVal = (String) val;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                
                insertWorkTagPs.setString(1, work.getId());
                insertWorkTagPs.setString(2, name);
                insertWorkTagPs.setObject(3, bVal);
                insertWorkTagPs.setObject(4, nVal);
                insertWorkTagPs.setObject(5, sVal);
                insertWorkTagPs.executeUpdate();
            }
        }
    }
}

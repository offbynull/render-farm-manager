package com.offbynull.rfm.host.services.h2db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.lang3.Validate;

final class WorkDeleter {
    private WorkDeleter() {
        // do nothing
    }
 
    public static void deleteWork(Connection conn, String id) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(id);
        Validate.notEmpty(id);

        String deleteWorkStr = "delete from work where id=?";
        try (PreparedStatement deleteWorkPs = conn.prepareStatement(deleteWorkStr)) {
            deleteWorkPs.setString(1, id);
            deleteWorkPs.executeUpdate();
        }
    }
}

/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.service.Bind;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

final class H2dbBindDataUtils {
    private H2dbBindDataUtils() {
        // do nothing
    }
    
    public static void bind(DataSource dataSource, String id, String host, int port) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(id);
        Validate.notNull(host);
        Validate.notEmpty(id);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);

        String insertStr = "merge into bind key(work_id, host, port) values(?,?,?)";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectWorkIdPs = conn.prepareStatement(insertStr)) {
                selectWorkIdPs.setString(1, id);
                selectWorkIdPs.setString(2, host);
                selectWorkIdPs.setInt(3, port);
                selectWorkIdPs.executeUpdate();
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
    
    public static void unbind(DataSource dataSource, String id, String host, int port) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(id);
        Validate.notNull(host);
        Validate.notEmpty(id);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        
        String deleteStr = "delete from bind where work_id=? and host=? and port=?";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectWorkIdPs = conn.prepareStatement(deleteStr)) {
                selectWorkIdPs.setString(1, id);
                selectWorkIdPs.setString(2, host);
                selectWorkIdPs.setInt(3, port);
                selectWorkIdPs.executeUpdate();
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static Set<Bind> listWorkBinds(DataSource dataSource, String id) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(id);
        Validate.notEmpty(id);

        String selectStr = "select host,port from bind where work_id=?";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectPs = conn.prepareStatement(selectStr)) {
                Set<Bind> ret = new HashSet<>();
                
                selectPs.setString(1, id);
                try (ResultSet selectRs = selectPs.executeQuery()) {
                    while (selectRs.next()) {
                        String host = selectRs.getString("host");
                        BigDecimal port = selectRs.getBigDecimal("port");

                        Bind bind = new Bind(id, host, port.intValueExact());
                        ret.add(bind);
                    }
                }
                
                return ret;
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static Set<Bind> listWorkerBinds(DataSource dataSource, String host, int port) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);

        String selectStr = "select work_id from bind where host=? and port=?";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectPs = conn.prepareStatement(selectStr)) {
                Set<Bind> ret = new HashSet<>();
                
                selectPs.setString(1, host);
                selectPs.setBigDecimal(2, BigDecimal.valueOf(port));
                try (ResultSet selectRs = selectPs.executeQuery()) {
                    while (selectRs.next()) {
                        String workId = selectRs.getString("work_id");

                        Bind bind = new Bind(workId, host, port);
                        ret.add(bind);
                    }
                }
                
                return ret;
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
}

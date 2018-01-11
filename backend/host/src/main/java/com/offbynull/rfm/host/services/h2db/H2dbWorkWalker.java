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

import com.offbynull.rfm.host.service.StoredWork;
import static com.offbynull.rfm.host.services.h2db.H2dbWorkDataUtils.getWork;
import java.io.IOException;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

final class H2dbWorkWalker {
    
    private H2dbWorkWalker() {
        // do nothing
    }
    
    public static StoredWork walk(DataSource dataSource, Predicate<StoredWork> stopCondition) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(stopCondition);
        
        String selectStr
                = "select id"
                + " from work"
                + " where state='WAITING'"
                + " order by priority asc";

        top:
        while (true) {
            try (Connection conn = dataSource.getConnection()) {
                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
                
                try (PreparedStatement psSelect = conn.prepareStatement(selectStr)) {
                    try (ResultSet rsSelect = psSelect.executeQuery()) {
                        while (rsSelect.next()) {
                            // Read next work
                            String id = rsSelect.getString("id");
                            StoredWork storedWork = getWork(conn, id);
                            if (storedWork == null) { // Work got removed or something was wrong with it, skip.
                                continue;
                            }

                            // Ingest work
                            if (stopCondition.test(storedWork)) {
                                return storedWork;
                            }
                        }
                    }
                }
                
                return null;
            } catch (SQLException sqle) {
                throw new IOException(sqle);
            }
        }
    }
}

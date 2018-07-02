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

import com.offbynull.rfm.host.service.Core;
import com.offbynull.rfm.host.service.Work;
import com.offbynull.rfm.host.service.Direction;
import com.offbynull.rfm.host.service.StoredWork;
import com.offbynull.rfm.host.service.WorkState;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

public final class H2dbWorkDataUtils {

    private H2dbWorkDataUtils() {
    }
    
    public static void updateWork(DataSource dataSource, Work work) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(work);

        String deleteWorkStr = "delete from work where id=?";
        String insertWorkStr = "insert into work (id,priority,script,state) values(?,?,?,'PAUSED')";
        String deleteWorkTagsStr = "delete from work_tag where id=?";
        String insertWorkTagStr = "insert into work_tag (id,name,val_b,val_n,val_s) values(?,?,?,?,?)";
        String deleteWorkParentsStr = "delete from work_parent where id=?";
        String insertWorkParentStr = "insert into work_parent (id,parent_id) values(?,?)";
        String deleteWritetimeStr = "delete from work_writetime where id=0";
        String insertWritetimeStr = "insert into work_writetime (id, write_time, priority) values(0,CURRENT_TIMESTAMP,?)";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement deleteWorkPs = conn.prepareStatement(deleteWorkStr);
                    PreparedStatement insertWorkPs = conn.prepareStatement(insertWorkStr);
                    PreparedStatement deleteWorkTagsPs = conn.prepareStatement(deleteWorkTagsStr);
                    PreparedStatement insertWorkTagPs = conn.prepareStatement(insertWorkTagStr);
                    PreparedStatement deleteWorkParentsPs = conn.prepareStatement(deleteWorkParentsStr);
                    PreparedStatement insertWorkParentPs = conn.prepareStatement(insertWorkParentStr);
                    PreparedStatement deleteWritetimePs = conn.prepareStatement(deleteWritetimeStr);
                    PreparedStatement insertWritetimePs = conn.prepareStatement(insertWritetimeStr)) {
                deleteWorkPs.setObject(1, work.getCore().getId());
                deleteWorkPs.executeUpdate();
                insertWorkPs.setObject(1, work.getCore().getId());
                insertWorkPs.setObject(2, work.getCore().getPriority());
                insertWorkPs.setObject(3, work.getRequirementsScript());
                insertWorkPs.executeUpdate();
                
                deleteWorkTagsPs.setObject(1, work.getCore().getId());
                deleteWorkTagsPs.executeUpdate();
                for (Entry<String, Object> e : work.getTags().entrySet()) {
                    insertWorkTagPs.setObject(1, work.getCore().getId());
                    insertWorkTagPs.setObject(2, e.getKey());
                    switch (e.getKey().substring(0, 2)) { // will always have atleast 2 characters
                        case "b_":
                            insertWorkTagPs.setObject(3, e.getValue());
                            insertWorkTagPs.setObject(4, null);
                            insertWorkTagPs.setObject(5, null);
                            break;
                        case "n_":
                            insertWorkTagPs.setObject(3, null);
                            insertWorkTagPs.setObject(4, e.getValue());
                            insertWorkTagPs.setObject(5, null);
                            break;
                        case "s_":
                            insertWorkTagPs.setObject(3, null);
                            insertWorkTagPs.setObject(4, null);
                            insertWorkTagPs.setObject(5, e.getValue());
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    insertWorkTagPs.executeUpdate();
                }
                
                deleteWorkParentsPs.setObject(1, work.getCore().getId());
                deleteWorkParentsPs.executeUpdate();
                for (String depId : work.getCore().getParents()) {
                    insertWorkParentPs.setObject(1, work.getCore().getId());
                    insertWorkParentPs.setObject(2, depId);
                    insertWorkParentPs.executeUpdate();
                }
                
                deleteWritetimePs.executeUpdate();
                insertWritetimePs.setObject(1, work.getCore().getPriority());
                insertWritetimePs.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static void deleteWork(DataSource dataSource, String id) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(id);
        Validate.notEmpty(id);
        
        String deleteWorkStr = "delete from work where id=?";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement deleteWorkPs = conn.prepareStatement(deleteWorkStr)) {
                deleteWorkPs.setObject(1, id);
                deleteWorkPs.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static StoredWork getWork(DataSource dataSource, String id) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(id);
        Validate.notEmpty(id);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            return getWork(conn, id);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static StoredWork getWork(Connection conn, String id) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(id);
        Validate.notEmpty(id);
        
        String selectWorkStr = "select id,priority,script,state from work where id=?";
        String selectWorkTagStr = "select id,name,val_b,val_n,val_s from work_tag where id=?";
        String selectWorkParentStr = "select id,parent_id from work_parent where id=?";
        
        try (PreparedStatement selectWorkPs = conn.prepareStatement(selectWorkStr);
                PreparedStatement selectWorkTagPs = conn.prepareStatement(selectWorkTagStr);
                PreparedStatement selectWorkParentPs = conn.prepareStatement(selectWorkParentStr)) {
            BigDecimal priority;
            String script;
            WorkState state;
            Map<String, Object> tags = new HashMap<>();
            Set<String> parents = new HashSet<>();

            selectWorkPs.setObject(1, id);
            try (ResultSet selectWorkRs = selectWorkPs.executeQuery()) {
                if (!selectWorkRs.next()) {
                    return null;
                }
                priority = selectWorkRs.getBigDecimal("priority");
                script = selectWorkRs.getString("script");
                try {
                    state = WorkState.valueOf(selectWorkRs.getString("state"));
                } catch (IllegalArgumentException iae) {
                    // the work data is invalid, return null and maybe log
                    return null;
                }
            }

            selectWorkTagPs.setObject(1, id);
            try (ResultSet selectWorkTagRs = selectWorkTagPs.executeQuery()) {
                while (selectWorkTagRs.next()) {
                    String name = selectWorkTagRs.getString("name");
                    Object value;
                    if (name.length() < 2) {
                        // the work data is invalid, return null and maybe log
                        return null;
                    }
                    switch (name.substring(0, 2)) {
                        case "b_":
                            value = selectWorkTagRs.getObject("val_b");
                            break;
                        case "n_":
                            value = selectWorkTagRs.getObject("val_n");
                            break;
                        case "s_":
                            value = selectWorkTagRs.getObject("val_s");
                            break;
                        default:
                            // the work data is invalid, return null and maybe log
                            return null;
                    }
                    tags.put(name, value);
                }
            }

            selectWorkParentPs.setObject(1, id);
            try (ResultSet selectWorkParentsRs = selectWorkParentPs.executeQuery()) {
                while (selectWorkParentsRs.next()) {
                    String parent = selectWorkParentsRs.getString("parent_id");
                    parents.add(parent);
                }
            }

            try {
                Core core = new Core(id, priority, parents);
                Work work = new Work(core, tags, script);
                return new StoredWork(id, work, state);
            } catch (RuntimeException re) {
                // the work data is invalid in some form, return null and maybe log
                return null;
            }
        }
    }
    
    public static List<StoredWork> getWorks(DataSource dataSource, String key, Direction direction, int max) throws IOException {
        Validate.notNull(dataSource);
        // key CAN be null -- it means start at the beginning
        Validate.notNull(direction);
        Validate.notEmpty(key);
        Validate.isTrue(max >= 0);
        
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

        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
                selectWorkIdPs.setObject(1, key);
                try (ResultSet selectWorkIdRs = selectWorkIdPs.executeQuery()) {
                    List<StoredWork> ret = new LinkedList<>();
                    while (selectWorkIdRs.next() && ret.size() < max) {
                        String id = selectWorkIdRs.getString("id");
                        StoredWork work = getWork(conn, id);
                        
                        if (work != null) { // it may have been deleted while scanning
                            ret.add(work);
                        }
                    }
                    return ret;
                }
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
    
    public static List<StoredWork> getWorksPrioritized(DataSource dataSource, String key, BigDecimal minPriority, Direction direction,
            int max) throws IOException {
        Validate.notNull(dataSource);
        // key CAN be null -- it means start at the beginning
        Validate.notNull(direction);
        Validate.notEmpty(key);
        Validate.isTrue(max >= 0);
        
        String selectWorkStr = "select id from work";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " where state='WAITING' and id>? and priority>=? order by priority,id asc";
                break;
            case BACKWARD:
                selectWorkStr += " where state='WAITING' and id<? and priority<=? order by priority,id desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }

        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
                selectWorkIdPs.setObject(1, key);
                try (ResultSet selectWorkIdRs = selectWorkIdPs.executeQuery()) {
                    List<StoredWork> ret = new LinkedList<>();
                    while (selectWorkIdRs.next() && ret.size() < max) {
                        String id = selectWorkIdRs.getString("id");
                        StoredWork work = getWork(conn, id);
                        
                        if (work != null) { // it may have been deleted while scanning
                            ret.add(work);
                        }
                    }
                    return ret;
                }
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
}

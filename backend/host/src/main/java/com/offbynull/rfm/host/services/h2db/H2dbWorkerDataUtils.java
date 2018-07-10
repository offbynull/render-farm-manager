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

import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.service.Worker;
import com.offbynull.rfm.host.service.Direction;
import com.offbynull.rfm.host.service.StoredWorker;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.math.BigDecimal;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

final class H2dbWorkerDataUtils {
    
    private H2dbWorkerDataUtils() {
    }
    
    public static void updateWorker(DataSource dataSource, Worker worker) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(worker);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            HostSpecification hostSpecification = worker.getHostSpecification();
            
            writeSpecs(conn, HostSpecification.class, List.of(hostSpecification));
            writeSpecs(conn, SocketSpecification.class, hostSpecification.getSocketSpecifications());
            for (SocketSpecification socketSpecification : hostSpecification.getSocketSpecifications()) {
                writeSpecs(conn, CoreSpecification.class, socketSpecification.getCoreSpecifications());
                for (CoreSpecification coreSpecification : socketSpecification.getCoreSpecifications()) {
                    writeSpecs(conn, CpuSpecification.class, coreSpecification.getCpuSpecifications());
                }
            }
            writeSpecs(conn, GpuSpecification.class, hostSpecification.getGpuSpecifications());
            writeSpecs(conn, MountSpecification.class, hostSpecification.getMountSpecifications());
            writeSpecs(conn, RamSpecification.class, hostSpecification.getRamSpecifications());
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static void deleteWorker(DataSource dataSource, String host, int port) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        
        String deleteWorkStr = "delete from host_spec where s_host=? and n_port=?";
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement deleteWorkPs = conn.prepareStatement(deleteWorkStr)) {
                deleteWorkPs.setObject(1, host);
                deleteWorkPs.setBigDecimal(2, BigDecimal.valueOf(port));
                deleteWorkPs.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    public static StoredWorker getWorker(DataSource dataSource, String host, int port) throws IOException {
        Validate.notNull(dataSource);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try {
                return getWorker(conn, host, port);
            } catch (RuntimeException re) {
                // the worker pulled in has some invalid attribute, so return nothing and maybe log here?
                return null;
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }

    private static StoredWorker getWorker(Connection conn, String host, int port) throws SQLException {
        throw new UnsupportedOperationException();
//        Validate.notNull(conn);
//        Validate.notNull(host);
//        Validate.notEmpty(host);
//        Validate.isTrue(port >= 1 && port <= 65535);
//
//        List<RamSpecification> rams = readSpecs(conn, RamSpecification.class, host, port,
//                (props,capacity) -> new RamSpecification(capacity, props));
//        List<MountSpecification> mounts = readSpecs(conn, MountSpecification.class, host, port,
//                (props,capacity) -> new MountSpecification(capacity, props));
//        List<GpuSpecification> gpus = readSpecs(conn, GpuSpecification.class, host, port,
//                (props,capacity) -> new GpuSpecification(capacity, props));
//        List<CpuSpecification> cpus = readSpecs(conn, CpuSpecification.class, host, port,
//                (props,capacity) -> new CpuSpecification(capacity, props));
//        List<CoreSpecification> cores = readSpecs(conn, CoreSpecification.class, host, port,
//                (props,capacity) -> new CoreSpecification(
//                        cpus.stream()
//                                .filter(x -> Objects.equals(props.get("n_cpu_id"), x.getProperties().get("n_cpu_id")))
//                                .collect(toList()),
//                        props));
//        List<SocketSpecification> sockets = readSpecs(conn, SocketSpecification.class, host, port,
//                (props,capacity) -> new SocketSpecification(
//                        cores.stream()
//                                .filter(x -> Objects.equals(props.get("n_socket_id"), x.getProperties().get("n_socket_id")))
//                                .collect(toList()),
//                        props));
//        List<HostSpecification> hosts = readSpecs(conn, HostSpecification.class, host, port,
//                (props,capacity) -> new HostSpecification(
//                        sockets,
//                        gpus,
//                        mounts,
//                        rams,
//                        props));
//
//        if (hosts.isEmpty()) {
//            return null;
//        }
//        
//        try {
//            Worker worker = new Worker(hosts.get(0));
//            StoredWorker storedWorker = new StoredWorker(host + ":" + port, worker);
//            return storedWorker;
//        } catch (RuntimeException re) {
//            // the work data is invalid, return null and maybe log
//            return null;
//        }
    }

    private static StoredWorker getWorker(Connection conn, String host, BigDecimal port) throws SQLException {
        Validate.notNull(conn);
        Validate.notNull(host);
        Validate.notNull(port);
        Validate.notEmpty(host);
        try {
            return getWorker(conn, host, port.intValueExact());
        } catch (ArithmeticException ae) {
            throw new IllegalArgumentException(ae);
        }
    }
    
    private static <T extends Specification> List<T> readSpecs(Connection conn, Class<T> cls, String host, int port,
            SpecificationCreator<T> creator) throws SQLException {
        String name = cls.getSimpleName();
        Validate.validState(cls.getName().endsWith("Specification")); // sanity check
        name = StringUtils.removeEnd(name, "Specification");
        name = StringUtils.uncapitalize(name);
        
        Set<String> keyNames;
        try {
            keyNames = (Set<String>) MethodUtils.invokeStaticMethod(cls, "getKeyPropertyNames");
        } catch (ClassCastException | ReflectiveOperationException roe) {
            throw new IllegalStateException(roe); // should never happen
        }
        
        String selectSpecStr = "select * from " + name + "_spec where s_host=? and n_port=?";
        String selectPropStr = "select * from " + name + "_prop where s_host=? and n_port=? "
                + keyNames.stream().map(x -> x + "=?").collect(joining(" and "));
        
        List<T> ret = new ArrayList<>();
        
        try (PreparedStatement selectSpecPs = conn.prepareStatement(selectSpecStr);
                PreparedStatement selectPropPs = conn.prepareStatement(selectPropStr)) {
            selectSpecPs.setString(1, host);
            selectSpecPs.setBigDecimal(2, BigDecimal.valueOf(port));
            try (ResultSet selectSpecRs = selectSpecPs.executeQuery()) {
                Map<String, Object> props = new HashMap<>();
                
                // Read key props + capacity
                BigDecimal capacity = null;
                while (selectSpecRs.next()) {
                    for (String keyName : keyNames) {
                        Object keyValue = selectSpecRs.getObject(keyName);
                        props.put(keyName, keyValue);
                    }

                    if (stream(cls.getInterfaces()).filter(x -> x == CapacityEnabledSpecification.class).count() == 1) {
                        capacity = selectSpecRs.getBigDecimal("capacity");
                    }
                }
                
                // Read user props
                selectPropPs.setString(1, host);
                selectPropPs.setBigDecimal(2, BigDecimal.valueOf(port));
                int nextIdx = 3;
                for (String keyName : keyNames) {
                    selectPropPs.setObject(nextIdx, keyName);
                    nextIdx++;
                }
                try (ResultSet selectPropRs = selectPropPs.executeQuery()) {
                    while (selectPropRs.next()) {
                        String propName = selectPropRs.getString("name");
                        Object propVal;
                        Validate.validState(propName.length() >= 2);
                        switch (propName.substring(0, 2)) {
                            case "b_":
                                propVal = selectPropRs.getObject("val_b");
                                break;
                            case "n_":
                                propVal = selectPropRs.getObject("val_n");
                                break;
                            case "s_":
                                propVal = selectPropRs.getObject("val_s");
                                break;
                            default:
                                throw new IllegalStateException(); // should never happen
                        }
                        props.put(propName, propVal);
                    }
                }
                
                T spec = creator.create(props, capacity);
                ret.add(spec);
            }
        }
        
        return ret;
    }
    
    private interface SpecificationCreator<T> {
        T create(Map<String, Object> properties, BigDecimal capacity);
    }
    
    private static <T extends Specification> void writeSpecs(Connection conn, Class<T> cls, List<T> specs) throws SQLException {
        String name = cls.getSimpleName();
        Validate.validState(cls.getName().endsWith("Specification")); // sanity check
        name = StringUtils.removeEnd(name, "Specification");
        name = StringUtils.uncapitalize(name);
        
        Set<String> keyNames;
        try {
            keyNames = (Set<String>) MethodUtils.invokeStaticMethod(cls, "getKeyPropertyNames");
        } catch (ClassCastException | ReflectiveOperationException roe) {
            throw new IllegalStateException(roe); // should never happen
        }
        
        boolean capacityEnabled = stream(cls.getInterfaces()).filter(x -> x == CapacityEnabledSpecification.class).count() == 1;
        String mergeSpecStr = "merge into " + name + "_spec"
                + " key(" + keyNames.stream().collect(joining(",")) + ")"
                + " values(" + keyNames.stream().map(x -> "?").collect(joining(",")) + (capacityEnabled ? ",?" : "") + ")";
        String mergePropStr = "merge into " + name + "_prop"
                + " key(" + keyNames.stream().collect(joining(",")) + ",name)"
                + " values(" + keyNames.stream().map(x -> "?").collect(joining(",")) + ",?,?,?,?)";

        try (PreparedStatement mergeSpecPs = conn.prepareStatement(mergeSpecStr);
                PreparedStatement mergePropPs = conn.prepareStatement(mergePropStr)) {
            for (T spec : specs) {
                Map<String, Object> props = spec.getProperties();
                
                // write spec row
                int keyIdx = 1;
                for (String keyName : keyNames) {
                    Object keyValue = props.get(keyName);
                    mergeSpecPs.setObject(keyIdx, keyValue);
                    keyIdx++;
                }
                
                BigDecimal capacity;
                try {
                    capacity = (BigDecimal) MethodUtils.invokeMethod(spec, "getCapacity");
                } catch (ClassCastException | ReflectiveOperationException roe) {
                    throw new IllegalStateException(roe); // should never happen
                }
                if (capacityEnabled) {
                    mergeSpecPs.setBigDecimal(keyIdx, capacity);
                }
                
                mergeSpecPs.executeUpdate();
                
                // write prop rows
                for (Entry<String, Object> prop : props.entrySet()) {
                    String propName = prop.getKey();
                    Object propValue = prop.getValue();
                    
                    mergePropPs.setString(1, propName);
                    switch(propName.substring(0, 2)) {
                        case "b_":
                            mergePropPs.setObject(2, propValue);
                            mergePropPs.setObject(3, null);
                            mergePropPs.setObject(4, null);
                            break;
                        case "n_":
                            mergePropPs.setObject(2, null);
                            mergePropPs.setObject(3, propValue);
                            mergePropPs.setObject(4, null);
                            break;
                        case "s_":
                            mergePropPs.setObject(2, null);
                            mergePropPs.setObject(3, null);
                            mergePropPs.setObject(4, propValue);
                            break;
                        default:
                            throw new IllegalStateException(); // should never happen
                    }
                }
                mergePropPs.executeUpdate();
            }
        }
    }
    
    public static List<StoredWorker> getWorkers(DataSource dataSource, String key, Direction direction, int max) throws IOException {
        Validate.notNull(dataSource);
        // key CAN be null -- it means start at the beginning
        Validate.notNull(direction);
        Validate.notEmpty(key);
        Validate.isTrue(max >= 0);
        
        String selectWorkStr = "select s_host,n_port from host_spec";
        switch (direction) {
            case FORWARD:
                selectWorkStr += " where s_host>=? and n_port>? order by s_host,n_port asc";
                break;
            case BACKWARD:
                selectWorkStr += " where s_host<=? and n_port<? order by s_host,n_port desc";
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }

        int splitIdx = key.lastIndexOf(':');
        if (splitIdx == -1) {
            throw new IOException("Bad key");
        }
        String keyHost = key.substring(0, splitIdx);
        BigDecimal keyPort;
        try {
            keyPort = new BigDecimal(key.substring(splitIdx+1));
        } catch (NumberFormatException e) {
            throw new IOException("Bad key", e);
        }
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            try (PreparedStatement selectWorkIdPs = conn.prepareStatement(selectWorkStr)) {
                selectWorkIdPs.setString(1, keyHost);
                selectWorkIdPs.setBigDecimal(2, keyPort);
                try (ResultSet selectWorkIdRs = selectWorkIdPs.executeQuery()) {
                    List<StoredWorker> ret = new LinkedList<>();
                    while (selectWorkIdRs.next() && ret.size() < max) {
                        String host = selectWorkIdRs.getString("s_host");
                        BigDecimal port = selectWorkIdRs.getBigDecimal("n_port");
                        
                        StoredWorker worker = getWorker(conn, host, port);
                        if (worker != null) { // if worker was removed or worker had bad attributes, skip.
                            ret.add(worker);
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

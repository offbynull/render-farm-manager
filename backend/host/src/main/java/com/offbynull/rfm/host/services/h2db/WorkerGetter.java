package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.constructSpecification;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationChildClasses;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationName;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.isSpecificationCapacityEnabled;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.reduceToSpecificationKey;
import java.math.BigDecimal;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

final class WorkerGetter {
    private WorkerGetter() {
        // do nothing
    }
    
    public static Worker getWorker(DataSource dataSource, String host, int port) throws SQLException {
        Validate.notNull(dataSource);
        Validate.notNull(host);
        Validate.notEmpty(host);
        Validate.isTrue(port >= 1 && port <= 65535);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            Map<String, Object> dbKeyValues = new LinkedHashMap<>();
            dbKeyValues.put("s_host", host);
            dbKeyValues.put("n_port", BigDecimal.valueOf(port));
            
            Set<String> expectedKeys = getSpecificationKey(HostSpecification.class);
            Validate.isTrue(dbKeyValues.keySet().equals(expectedKeys));
            
            HostSpecification hostSpecification = (HostSpecification) readSpec(conn, dbKeyValues, HostSpecification.class);
            if (hostSpecification == null) {
                return null;
            }
            
            Worker worker = new Worker(hostSpecification);
            return worker;
        }
    }

    private static Specification readSpec(
            Connection conn,
            Map<String, Object> dbKeyValues,
            Class<? extends Specification> specCls) throws SQLException {
        String name = getSpecificationName(specCls);
        boolean capacityEnabled = isSpecificationCapacityEnabled(specCls);
        
        // read spec
        List<SpecificationRow> specRows = selectSpecificationRows(conn, name, dbKeyValues.keySet(), dbKeyValues, capacityEnabled);
        switch (specRows.size()) {
            case 0:
                return null;
            case 1:
                // get spec
                SpecificationRow specRow = specRows.get(0);
                
                // read spec's user properties
                Map<String, Object> specUserProps = selectSpecificationUserProperties(conn, name, specRow.dbKeyProperties);
                
                // create spec
                Specification spec = createSpecification(conn, specCls, specRow, specUserProps);
                
                // add spec to collection
                return spec;
            default:
                throw new IllegalStateException("" + specRows.size());
        }
    }
    
    private static Specification[] readSpecsByParent(
            Connection conn,
            Map<String, Object> parentDbKeyValues,
            Class<? extends Specification> specCls) throws SQLException {
        String name = getSpecificationName(specCls);
        Set<String> key = getSpecificationKey(specCls);
        boolean capacityEnabled = isSpecificationCapacityEnabled(specCls);
        
        Set<String> dbKey = new LinkedHashSet<>(parentDbKeyValues.keySet());
        dbKey.addAll(key);
        
        List<Specification> ret = new ArrayList<>();
        
        // read specs -- for each spec
        List<SpecificationRow> specRows = selectSpecificationRows(conn, name, dbKey, parentDbKeyValues, capacityEnabled);
        for (SpecificationRow specRow : specRows) {
            // read spec's user properties
            Map<String, Object> specUserProps = selectSpecificationUserProperties(conn, name, specRow.dbKeyProperties);
            
            // create spec
            Specification spec = createSpecification(conn, specCls, specRow, specUserProps);
            
            // add spec to collection
            ret.add(spec);
        }
        
        return ret.stream().toArray(len -> new Specification[len]);
    }
    
    private static Specification createSpecification(
            Connection conn,
            Class<? extends Specification> specCls,
            SpecificationRow specRow,
            Map<String, Object> specUserProps) throws SQLException {
        // read spec's child specs
        Set<Specification> childSpecs = new LinkedHashSet<>();
        Set<Class<? extends Specification>> childSpecClses = getSpecificationChildClasses(specCls);
        for (Class<? extends Specification> childSpecCls : childSpecClses) {
            Specification[] childSpec = readSpecsByParent(conn, specRow.dbKeyProperties, childSpecCls);
            childSpecs.addAll(asList(childSpec));
        }

        // get key properties
          // specRow.dbKeyProperties contains the key of ALL specifications in the hierarchy/chain (the full key). For example, if this was
          // for a cpu spec, it would contain the keys for host, socket, core, and cpu (enough to identify it in the database). We reduce it
          // to JUST the cpu key, because that's what the Specification expects: the key for just it, not it and everything in its
          // hierarchy.
        Map<String, Object> specKeyProps = reduceToSpecificationKey(specCls, specRow.dbKeyProperties);
        
        // construct spec
        Map<String, Object> specProps = new HashMap<>();
        specProps.putAll(specKeyProps);
        specProps.putAll(specUserProps);
        Validate.isTrue(specProps.size() == specKeyProps.size() + specUserProps.size()); // make sure no colliding properties
        BigDecimal specCapacity = specRow.capacity;
        Specification spec = constructSpecification(specCls, childSpecs, specProps, specCapacity);
        
        return spec;
    }
    
    private static List<SpecificationRow> selectSpecificationRows(
            Connection conn,
            String specName,
            Set<String> selectKeys,
            Map<String, Object> searchKeys,
            boolean capacityEnabled) throws SQLException {
        String selectSpecStr = ""
                + "SELECT"
                + " " + selectKeys.stream().collect(joining(",")) + (capacityEnabled ? ",capacity" : "")
                + " FROM " + specName + "_spec";
        if (!searchKeys.isEmpty()) {
            selectSpecStr += " WHERE " + searchKeys.keySet().stream().map(k -> k + "=?").collect(joining(" AND "));
        }
        
        try (PreparedStatement ps = conn.prepareStatement(selectSpecStr)) {
            int idx = 1;
            for (Object value : searchKeys.values()) {
                ps.setObject(idx, value);
                idx++;
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<SpecificationRow> specRows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> keyProperties = new LinkedHashMap<>();
                    BigDecimal capacity = null;

                    for (String name : selectKeys) {
                        Object value = rs.getObject(name);
                        keyProperties.put(name, value);
                    }

                    if (capacityEnabled) {
                        capacity = rs.getBigDecimal("capacity");
                    }
                    
                    SpecificationRow specRow = new SpecificationRow(keyProperties, capacity);
                    specRows.add(specRow);
                }
                return specRows;
            }
        }
    }
    
    private static Map<String, Object> selectSpecificationUserProperties(
            Connection conn,
            String specName,
            Map<String, Object> searchKeys) throws SQLException {
        String selectPropStr = ""
                + "SELECT"
                + " name,val_b,val_n,val_s"
                + " FROM " + specName + "_prop";
        if (!searchKeys.isEmpty()) {
            selectPropStr += " WHERE " + searchKeys.keySet().stream().map(k -> k + "=?").collect(joining(" AND "));
        }
        
        Map<String, Object> userProperties = new HashMap<>();
        
        try (PreparedStatement ps = conn.prepareStatement(selectPropStr)) {
            int idx = 1;
            for (Object value : searchKeys.values()) {
                ps.setObject(idx, value);
                idx++;
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    Object value;
                    Validate.validState(name.length() >= 2);
                    switch(name.substring(0, 2)) {
                        case "b_":
                            value = rs.getObject("val_b");
                            break;
                        case "n_":
                            value = rs.getObject("val_n");
                            break;
                        case "s_":
                            value = rs.getObject("val_s");
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    userProperties.put(name, value);
                }
            }
        }
        
        return userProperties;
    }
    
    private static class SpecificationRow {
        private final Map<String, Object> dbKeyProperties;
        private final BigDecimal capacity;

        public SpecificationRow(Map<String, Object> keys, BigDecimal capacity) {
            this.dbKeyProperties = keys;
            this.capacity = capacity;
        }
    }
}

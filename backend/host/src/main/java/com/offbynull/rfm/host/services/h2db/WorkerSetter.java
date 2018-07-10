package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationCapacity;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationChildren;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationFullKeyValues;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationKey;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationName;
import java.math.BigDecimal;
import java.sql.Connection;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;

final class WorkerSetter {
    private WorkerSetter() {
        // do nothing
    }
    
    public static void setWorker(DataSource dataSource, Worker worker) throws SQLException {
        Validate.notNull(dataSource);
        Validate.notNull(worker);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
            
            HostSpecification hostSpecification = worker.getHostSpecification();
            
            LinkedList<Specification> specChain = new LinkedList<>();
            specChain.add(hostSpecification);
            recurseWriteSpecs(conn, specChain);
            
            conn.commit();
        }
    }
    
    private static void recurseWriteSpecs(Connection conn, LinkedList<Specification> specChain) throws SQLException {
        Specification finalSpec = specChain.get(specChain.size() - 1);
        writeSpec(conn, specChain);
        for (Specification child : getSpecificationChildren(finalSpec).values()) {
            specChain.addLast(child);
            recurseWriteSpecs(conn, specChain);
            specChain.removeLast();
        }
    }
    
    private static void writeSpec(Connection conn, List<Specification> specChain) throws SQLException {
        Specification finalSpec = specChain.get(specChain.size() - 1);
        
        String name = getSpecificationName(finalSpec);
        Set<String> key = getSpecificationKey(finalSpec);
        
        Map<String, Object> dbKeyValues = getSpecificationFullKeyValues(specChain);
        Set<String> dbKey = dbKeyValues.keySet(); // NOT the same as key -- this is key of entire specChain, not the individual spec
        
        BigDecimal capacity = getSpecificationCapacity(finalSpec);
        
        String mergeSpecStr = "merge into " + name + "_spec"
                + " key(" + dbKey.stream().collect(joining(",")) + ")"
                + " values(" + dbKey.stream().map(x -> "?").collect(joining(",")) + (capacity != null ? ",?" : "") + ")";
        String mergePropStr = "merge into " + name + "_prop"
                + " key(" + dbKey.stream().collect(joining(",")) + ",name)"
                + " values(" + dbKey.stream().map(x -> "?").collect(joining(",")) + ",?,?,?,?)";

        try (PreparedStatement mergeSpecPs = conn.prepareStatement(mergeSpecStr);
                PreparedStatement mergePropPs = conn.prepareStatement(mergePropStr)) {
            Map<String, Object> props = finalSpec.getProperties();

            
            // write spec row
            int specColIdx = 1;
            for (String keyName : dbKey) {
                Object keyValue = dbKeyValues.get(keyName);
                mergeSpecPs.setObject(specColIdx, keyValue);
                specColIdx++;
            }

            if (capacity != null) {
                mergeSpecPs.setBigDecimal(specColIdx, capacity);
            }

            mergeSpecPs.executeUpdate();

            
            // write prop rows
            for (Entry<String, Object> prop : props.entrySet()) {
                String propName = prop.getKey();
                Object propValue = prop.getValue();

                int propColIdx = 1;
                for (String keyName : dbKey) {
                    Object keyValue = dbKeyValues.get(keyName);
                    mergePropPs.setObject(propColIdx, keyValue);
                    propColIdx++;
                }
                
                if (key.contains(propName)) { // skip keys -- DONT use dbKeys, we can have props that have the same name as a parent's key 
                    continue;
                }
                
                mergePropPs.setString(propColIdx, propName);
                propColIdx++;
                
                Object bVal = null;
                Object nVal = null;
                Object sVal = null;
                switch(propName.substring(0, 2)) {
                    case "b_":
                        bVal = propValue;
                        break;
                    case "n_":
                        nVal = propValue;
                        break;
                    case "s_":
                        sVal = propValue;
                        break;
                    default:
                        throw new IllegalStateException(); // should never happen
                }
                mergePropPs.setObject(propColIdx, bVal);
                propColIdx++;
                mergePropPs.setObject(propColIdx, nVal);
                propColIdx++;
                mergePropPs.setObject(propColIdx, sVal);
                
                mergePropPs.executeUpdate();
            }
        }
    }
}

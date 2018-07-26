package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.CorePartition;
import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.partition.GpuPartition;
import com.offbynull.rfm.host.model.partition.MountPartition;
import com.offbynull.rfm.host.model.partition.Partition;
import com.offbynull.rfm.host.model.partition.RamPartition;
import com.offbynull.rfm.host.model.partition.SocketPartition;
import com.offbynull.rfm.host.model.requirement.CapacityEnabledRequirement;
import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.parser.Parser;
import com.offbynull.rfm.host.service.Work;
import com.offbynull.rfm.host.service.Worker;
import static com.offbynull.rfm.host.services.h2db.InternalUtils.getSpecificationKeyValues;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.Validate;

final class Binder {
    private Binder() {
        // do nothing
    }
    
    public static boolean bind(Connection conn, Parser parser, String workId, String workerHost, int workerPort)
            throws SQLException, IOException {
        Validate.notNull(workId);
        Validate.notNull(workerHost);
        Validate.notEmpty(workId);
        Validate.notEmpty(workerHost);
        Validate.isTrue(workerPort >= 1 && workerPort <= 65535);
        
        Worker worker = WorkerGetter.getWorker(conn, workerHost, workerPort);
        if (worker == null) {
            return false;
        }
        
        Work work = WorkGetter.getWork(conn, workId);
        if (work == null) {
            return false;
        }
        
        String bindCheckSql = "select count(*) bind where work_id=?";
        try (PreparedStatement ps = conn.prepareStatement(bindCheckSql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            int count = rs.getInt(1);
            if (count > 0) {
                return false;
            }
        }
        
        String parentCheckSql = "select count(*) from work_parent where id=?";
        try (PreparedStatement ps = conn.prepareStatement(parentCheckSql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            int count = rs.getInt(1);
            if (count > 0) {
                return false;
            }
        }
        
        
        Map<String, Object> tags = work.getTags();
        String reqScript = work.getRequirementsScript();
        HostRequirement preBindHostReq = parser.parseScriptReqs(tags, reqScript);
        
        String insertBindSql = "insert into bind values (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertBindSql)) {
            ps.setString(1, workId);
            ps.setString(2, workerHost);
            ps.setBigDecimal(3, BigDecimal.valueOf(workerPort));
            ps.executeUpdate();
        }
        
        return false;
    }
    

//    private static HostPartition partitionHost(
//            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
//            HostRequirement hostReq,
//            HostSpecification hostSpec) {
//        // we need to copy updatableCapacities becuase it may get modified even if a match isn't found... if a match is found the copy will
//        // get put back into the original
//        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy;        
//
//        // for each socket requirement
//        List<SocketPartition> socketPartitions = new LinkedList<>();
//        for (SocketRequirement socketReq : hostReq.getSocketRequirements()) {
//            for (SocketSpecification socketSpec : hostSpec.getSocketSpecifications()) {
//                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
//                List<SocketPartition> _socketPartitions = partitionAcrossSockets(updatableCapacitiesCopy, socketReq, List.of(socketSpec));
//                socketPartitions.addAll(_socketPartitions);
//                updatableCapacities.putAll(updatableCapacitiesCopy);
//            }
//        }
//
//        // for each gpu requirement
//        List<GpuPartition> gpuPartitions = new LinkedList<>();
//        for (GpuRequirement gpuReq : hostReq.getGpuRequirements()) {
//            for (GpuSpecification gpuSpec : hostSpec.getGpuSpecifications()) {
//                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
//                GpuPartition _gpuPartition = partitionGpu(updatableCapacitiesCopy, gpuReq, gpuSpec);
//                gpuPartitions.add(_gpuPartition);
//                updatableCapacities.putAll(updatableCapacitiesCopy);
//            }
//        }
//
//        // for each mount requirement
//        List<MountPartition> mountPartitions = new LinkedList<>();
//        for (MountRequirement mountReq : hostReq.getMountRequirements()) {
//            for (MountSpecification mountSpec : hostSpec.getMountSpecifications()) {
//                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
//                MountPartition _mountPartition = partitionMount(updatableCapacitiesCopy, mountReq, mountSpec);
//                mountPartitions.add(_mountPartition);
//                updatableCapacities.putAll(updatableCapacitiesCopy);
//            }
//        }
//
//        // for each ram requirement
//        List<RamPartition> ramPartitions = new LinkedList<>();
//        for (RamRequirement ramReq : hostReq.getRamRequirements()) {
//            for (RamSpecification ramSpec : hostSpec.getRamSpecifications()) {
//                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
//                RamPartition _ramPartition = partitionRam(updatableCapacitiesCopy, ramReq, ramSpec);
//                ramPartitions.add(_ramPartition);
//                updatableCapacities.putAll(updatableCapacitiesCopy);
//            }
//        }
//        
//        Map<String, Object> specificationId = getSpecificationKeyValues(hostSpec);
//        return new HostPartition(specificationId,
//                socketPartitions.stream().toArray(i -> new SocketPartition[i]),
//                gpuPartitions.stream().toArray(i -> new GpuPartition[i]),
//                ramPartitions.stream().toArray(i -> new RamPartition[i]),
//                mountPartitions.stream().toArray(i -> new MountPartition[i]));
//    }
    
    static SocketPartition partitionIndividualSocket(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            SocketRequirement socketReq,
            SocketSpecification socketSpec) {
        Validate.validState(socketReq.getCount() != null);
        
        List<CorePartition> socketPartitions = new ArrayList<>();
        
        for (CoreRequirement coreReq : socketReq.getCoreRequirements()) {
            // we need operate on a copy of updatableCapacities because we may end up discarding the partitions we acquire
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);
        
            NumberRange coreReqCount = coreReq.getCount();
            List<CorePartition> socketCorePartitions = new LinkedList<>();
            
            if (coreReqCount == null) {
                List<CoreSpecification> coreSpecs = socketSpec.getCoreSpecifications();
                List<CorePartition> foundCorePartitions = partitionAcrossAllCores(updatableCapacitiesCopy, coreReq, coreSpecs);
                socketCorePartitions.addAll(foundCorePartitions);
            } else {
                while (true) {
                    int oldSize = socketCorePartitions.size();

                    for (CoreSpecification coreSpec : socketSpec.getCoreSpecifications()) {
                        // have we found the max cores for the req? if so break
                        int foundCount = socketCorePartitions.size();
                        if (!coreReqCount.isBeforeEnd(foundCount)) {
                            break;
                        }
                        // partition this core and add that partition to the found list
                        CorePartition foundCorePartition = partitionIndividualCore(updatableCapacitiesCopy, coreReq, coreSpec);
                        if (foundCorePartition != null) {
                            socketCorePartitions.add(foundCorePartition);
                        }
                    }

                    // if nothing was added in this pass, break out of loop
                    int newSize = socketCorePartitions.size();
                    if (oldSize == newSize) {
                        break;
                    }
                }

                // if we don't have atleast the min cores for the req, skip
                int foundCount = socketCorePartitions.size();
                if (coreReqCount.isBeforeStart(foundCount)) {
                    continue;
                }
            }
            
            // otherwise, apply
            updatableCapacities.putAll(updatableCapacitiesCopy);
            socketPartitions.addAll(socketCorePartitions);
        }


        Map<String, Object> specificationId = getSpecificationKeyValues(socketSpec);
        CorePartition[] corePartitions = socketPartitions.stream().toArray(i -> new CorePartition[i]);
        return new SocketPartition(specificationId, corePartitions);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    static CorePartition partitionIndividualCore(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CoreRequirement coreReq,
            CoreSpecification coreSpec) {
        Validate.validState(coreReq.getCount() != null);
        
        List<CpuPartition> corePartitions = new ArrayList<>();
        
        for (CpuRequirement cpuReq : coreReq.getCpuRequirements()) {
            // we need operate on a copy of updatableCapacities because we may end up discarding the partitions we acquire
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);
        
            NumberRange cpuReqCount = cpuReq.getCount();
            List<CpuPartition> coreCpuPartitions = new LinkedList<>();
            while (true) {
                int oldSize = coreCpuPartitions.size();

                for (CpuSpecification cpuSpec : coreSpec.getCpuSpecifications()) {
                    // have we found the max cpus for the req? if so break
                    int foundCount = coreCpuPartitions.size();
                    if (!cpuReqCount.isBeforeEnd(foundCount)) {
                        break;
                    }
                    // partition this cpu and add that partition to the found list
                    CpuPartition foundCpuPartition = partitionCpu(updatableCapacitiesCopy, cpuReq, cpuSpec);
                    if (foundCpuPartition != null) {
                        coreCpuPartitions.add(foundCpuPartition);
                    }
                }
                
                // if nothing was added in this pass, break out of loop
                int newSize = coreCpuPartitions.size();
                if (oldSize == newSize) {
                    break;
                }
            }
            
            // if we don't have atleast the min cpus for the req, skip
            int foundCount = coreCpuPartitions.size();
            if (cpuReqCount.isBeforeStart(foundCount)) {
                continue;
            }
            
            // otherwise, apply
            updatableCapacities.putAll(updatableCapacitiesCopy);
            corePartitions.addAll(coreCpuPartitions);
        }


        Map<String, Object> specificationId = getSpecificationKeyValues(coreSpec);
        CpuPartition[] cpuPartitions = corePartitions.stream().toArray(i -> new CpuPartition[i]);
        return new CorePartition(specificationId, cpuPartitions);
    }
    
    static List<CorePartition> partitionAcrossAllCores(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CoreRequirement coreReq,
            List<CoreSpecification> coreSpecs) {
        Validate.validState(coreReq.getCount() == null);
        
        // we need to copy updatableCapacities becuase it may get modified even if a match isn't found... if a match is found the copy will
        // get put back into the original
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);
        MultiValuedMap<CoreSpecification, CpuPartition> corePartitions = new ArrayListValuedHashMap<>();
        
        for (CpuRequirement cpuReq : coreReq.getCpuRequirements()) {
            NumberRange cpuReqCount = cpuReq.getCount();
            while (true) {
                int oldSize = corePartitions.size();
                
                for (CoreSpecification coreSpec : coreSpecs) {
                    List<CpuPartition> coreCpuPartitions = new LinkedList<>();
                    for (CpuSpecification cpuSpec : coreSpec.getCpuSpecifications()) {
                        // have we found the max cpus for the req? if so break
                        int foundCount = coreCpuPartitions.size();
                        if (!cpuReqCount.isBeforeEnd(foundCount)) {
                            break;
                        }
                        // partition this cpu and add that partition to the found list
                        CpuPartition foundCpuPartition = partitionCpu(updatableCapacitiesCopy, cpuReq, cpuSpec);
                        if (foundCpuPartition != null) {
                            coreCpuPartitions.add(foundCpuPartition);
                        }
                    }
                    
                    // if we don't have atleast the min cpus for the req, skip
                    int foundCount = coreCpuPartitions.size();
                    if (cpuReqCount.isBeforeStart(foundCount)) {
                        continue;
                    }
                    corePartitions.putAll(coreSpec, coreCpuPartitions);
                }
                
                // if nothing was added in this pass, break out of loop
                int newSize = corePartitions.size();
                if (oldSize == newSize) {
                    break;
                }
            }
        }

        updatableCapacities.putAll(updatableCapacitiesCopy);
        List<CorePartition> ret = new LinkedList<>();
        for (CoreSpecification key : corePartitions.keySet()) {
            CpuPartition[] cpuPartitions = corePartitions.get(key).stream().toArray(i -> new CpuPartition[i]);
            Map<String, Object> specificationId = getSpecificationKeyValues(key);
            
            CorePartition corePartition = new CorePartition(specificationId, cpuPartitions);
            ret.add(corePartition);
        }
        return ret;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    //
    // All of the following requirements are terminal nodes -- they cannot have a count range of null.
    //
    
    static CpuPartition partitionCpu(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CpuRequirement cpuReq,
            CpuSpecification cpuSpec) {
        return partitionLeafNode(
                updatableCapacities,
                cpuReq,
                cpuSpec,
                (id, cap) -> new CpuPartition(id, cap));
    }
    
    static GpuPartition partitionGpu(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            GpuRequirement gpuReq,
            GpuSpecification gpuSpec) {
        return partitionLeafNode(
                updatableCapacities,
                gpuReq,
                gpuSpec,
                (id, cap) -> new GpuPartition(id, cap));
    }
    
    static MountPartition partitionMount(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            MountRequirement mountReq,
            MountSpecification mountSpec) {
        return partitionLeafNode(
                updatableCapacities,
                mountReq,
                mountSpec,
                (id, cap) -> new MountPartition(id, cap));
    }
    
    static RamPartition partitionRam(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            RamRequirement ramReq,
            RamSpecification ramSpec) {
        return partitionLeafNode(
                updatableCapacities,
                ramReq,
                ramSpec,
                (id, cap) -> new RamPartition(id, cap));
    }
    
    private static <P extends Partition, S extends CapacityEnabledSpecification, R extends CapacityEnabledRequirement> P partitionLeafNode(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            R req,
            S spec,
            LeafConstructor<P> constructor) {
        Validate.validState(((Requirement) req).getCount() != null);
        
        Map<String, Object> specificationId = getSpecificationKeyValues((Specification) spec);
        BigDecimal consumeCapacity = consumeCapacity(updatableCapacities, spec, req);
        if (consumeCapacity == null) {
            return null;
        }
        P partition = constructor.construct(specificationId, consumeCapacity);
        return partition;
    }
    
    private interface LeafConstructor<T extends Partition> {
        T construct(Map<String, Object> specificationId, BigDecimal capacity);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static BigDecimal consumeCapacity(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CapacityEnabledSpecification spec,
            CapacityEnabledRequirement req) {
        BigDecimal availableCapacity = updatableCapacities.get(spec);
        BigDecimal consumeCapacity = calculateCapacityToConsume(availableCapacity, req.getCapacityRange());

        if (consumeCapacity == null) {
            return null;
        }

        BigDecimal newCapacity = availableCapacity.subtract(consumeCapacity);
        updatableCapacities.put(spec, newCapacity);

        return consumeCapacity;
    }

    private static BigDecimal calculateCapacityToConsume(BigDecimal specificationCapacity, NumberRange requirementRange) {
        if (specificationCapacity.compareTo(requirementRange.getEnd()) >= 0) { // if we can acquire the max, do so
            return requirementRange.getEnd();
        }

        if (specificationCapacity.compareTo(requirementRange.getStart()) < 0) { // can we even acquire the min? if no, return null 
            return null;
        }

        // eat all the available capacity
        return specificationCapacity;
    }
}

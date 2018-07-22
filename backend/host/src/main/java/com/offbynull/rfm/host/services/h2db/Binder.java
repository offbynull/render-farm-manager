package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.CorePartition;
import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.partition.GpuPartition;
import com.offbynull.rfm.host.model.partition.HostPartition;
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
import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    

    private static HostPartition partitionHost(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            HostRequirement hostReq,
            HostSpecification hostSpec) {
        // we need to copy updatableCapacities becuase it may get modified even if a match isn't found... if a match is found the copy will
        // get put back into the original
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy;        

        // for each socket requirement
        List<SocketPartition> socketPartitions = new LinkedList<>();
        for (SocketRequirement socketReq : hostReq.getSocketRequirements()) {
            for (SocketSpecification socketSpec : hostSpec.getSocketSpecifications()) {
                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
                List<SocketPartition> _socketPartitions = partitionAcrossSockets(updatableCapacitiesCopy, socketReq, List.of(socketSpec));
                socketPartitions.addAll(_socketPartitions);
                updatableCapacities.putAll(updatableCapacitiesCopy);
            }
        }

        // for each gpu requirement
        List<GpuPartition> gpuPartitions = new LinkedList<>();
        for (GpuRequirement gpuReq : hostReq.getGpuRequirements()) {
            for (GpuSpecification gpuSpec : hostSpec.getGpuSpecifications()) {
                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
                List<GpuPartition> _gpuPartitions = partitionGpu(updatableCapacitiesCopy, gpuReq, List.of(gpuSpec));
                gpuPartitions.addAll(_gpuPartitions);
                updatableCapacities.putAll(updatableCapacitiesCopy);
            }
        }

        // for each mount requirement
        List<MountPartition> mountPartitions = new LinkedList<>();
        for (MountRequirement mountReq : hostReq.getMountRequirements()) {
            for (MountSpecification mountSpec : hostSpec.getMountSpecifications()) {
                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
                List<MountPartition> _mountPartitions = partitionMount(updatableCapacitiesCopy, mountReq, List.of(mountSpec));
                mountPartitions.addAll(_mountPartitions);
                updatableCapacities.putAll(updatableCapacitiesCopy);
            }
        }

        // for each ram requirement
        List<RamPartition> ramPartitions = new LinkedList<>();
        for (RamRequirement ramReq : hostReq.getRamRequirements()) {
            for (RamSpecification ramSpec : hostSpec.getRamSpecifications()) {
                updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
                List<RamPartition> _ramPartitions = partitionRam(updatableCapacitiesCopy, ramReq, List.of(ramSpec));
                ramPartitions.addAll(_ramPartitions);
                updatableCapacities.putAll(updatableCapacitiesCopy);
            }
        }
        
        Map<String, Object> specificationId = getSpecificationKeyValues(hostSpec);
        return new HostPartition(specificationId,
                socketPartitions.stream().toArray(i -> new SocketPartition[i]),
                gpuPartitions.stream().toArray(i -> new GpuPartition[i]),
                ramPartitions.stream().toArray(i -> new RamPartition[i]),
                mountPartitions.stream().toArray(i -> new MountPartition[i]));
    }
    
    
    
    private static List<SocketPartition> partitionAcrossSockets(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            SocketRequirement socketReq,
            List<SocketSpecification> socketSpecs) {
        // we need to copy updatableCapacities becuase it may get modified even if a match isn't found... if a match is found the copy will
        // get put back into the original
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
        List<SocketPartition> socketPartitions = new LinkedList<>();        

        // for each socket requirement
        for (CoreRequirement coreReq : socketReq.getCoreRequirements()) {
            List<SocketPartition> socketPartitionsForReq = new LinkedList<>();
            List<CorePartition> corePartitionsForReq = new LinkedList<>();
            NumberRange coreCountRangeForReq = coreReq.getCount();
            // for each socket
            for (SocketSpecification socketSpec : socketSpecs) {
                Map<String, Object> specificationId = getSpecificationKeyValues(socketSpec);
                // for each core in the socket
                for (CoreSpecification coreSpec : socketSpec.getCoreSpecifications()) {
                    // have we found the max core for the req? if so break
                    int foundCount = corePartitionsForReq.size();
                    if (coreCountRangeForReq.compareEnd(foundCount) >= 0) {
                        break;
                    }
                    // partition this core and add that partition to the found list
                    List<CorePartition> corePartitions = partitionAcrossCores(updatableCapacitiesCopy, coreReq, List.of(coreSpec));
                    corePartitionsForReq.addAll(corePartitions);
                }
                // add core
                SocketPartition socketPartition = new SocketPartition(specificationId,
                        corePartitionsForReq.stream().toArray(i -> new CorePartition[i]));
                socketPartitionsForReq.add(socketPartition);
            }
            // if the number of found cpus is within the req bounds, add the cores they were found in it to the return list
            long coreCount = socketPartitions.stream().flatMap(x -> x.getCorePartitions().stream()).count();
            if (coreCountRangeForReq.isInRange(coreCount)) {
                socketPartitions.addAll(socketPartitionsForReq);
            }
        }
        
        if (!socketPartitions.isEmpty()) {
            updatableCapacities.putAll(updatableCapacitiesCopy);
        }
        
        return socketPartitions;
    }
    
    static List<CorePartition> partitionAcrossCores(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CoreRequirement coreReq,
            List<CoreSpecification> coreSpecs) {
        // we need to copy updatableCapacities becuase it may get modified even if a match isn't found... if a match is found the copy will
        // get put back into the original
        Map<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new LinkedHashMap<>(updatableCapacities);
        List<CorePartition> corePartitions = new LinkedList<>();        

        // for each cpu requirement
        for (CpuRequirement cpuReq : coreReq.getCpuRequirements()) {
            List<CorePartition> corePartitionsForReq = new LinkedList<>();
            List<CpuPartition> cpuPartitionsForReq = new LinkedList<>();
            NumberRange cpuCountRangeForReq = cpuReq.getCount();
            // for each core
            for (CoreSpecification coreSpec : coreSpecs) {
                Map<String, Object> specificationId = getSpecificationKeyValues(coreSpec);
                // for each cpu in the core
                for (CpuSpecification cpuSpec : coreSpec.getCpuSpecifications()) {
                    // have we found the max cpus for the req? if so break
                    int foundCount = cpuPartitionsForReq.size();
                    if (cpuCountRangeForReq.compareEnd(foundCount) <= 0) {
                        break;
                    }
                    // partition this cpu and add that partition to the found list
                    List<CpuPartition> cpuPartitions = partitionCpu(updatableCapacitiesCopy, cpuReq, List.of(cpuSpec));
                    cpuPartitionsForReq.addAll(cpuPartitions);
                }
                // add core
                CorePartition corePartition = new CorePartition(specificationId,
                        cpuPartitionsForReq.stream().toArray(i -> new CpuPartition[i]));
                corePartitionsForReq.add(corePartition);
            }
            // if the number of found cpus is within the req bounds, add the cores they were found in it to the return list
            long foundCount = cpuPartitionsForReq.size();
            if (cpuCountRangeForReq.compareStart(foundCount) <= 0) {
                corePartitions.addAll(corePartitionsForReq);
            }
        }
        
        int coreCount = corePartitions.size();
        if (coreReq.getCount().isInRange(coreCount)) {
            updatableCapacities.putAll(updatableCapacitiesCopy);
            return corePartitions;
        } else {
            return null;
        }
    }
    
    static List<CpuPartition> partitionCpu(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CpuRequirement cpuReq,
            List<CpuSpecification> cpuSpecs) {
        return partitionLeafNode(updatableCapacities,
                cpuReq,
                cpuSpecs,
                (id, cap) -> new CpuPartition(id, cap));
    }
    
    private static List<GpuPartition> partitionGpu(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            GpuRequirement gpuReq,
            List<GpuSpecification> gpuSpecs) {
        return partitionLeafNode(
                updatableCapacities,
                gpuReq,
                gpuSpecs,
                (id, cap) -> new GpuPartition(id, cap));
    }
    
    private static List<MountPartition> partitionMount(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            MountRequirement mountReq,
            List<MountSpecification> mountSpecs) {
        return partitionLeafNode(
                updatableCapacities,
                mountReq,
                mountSpecs,
                (id, cap) -> new MountPartition(id, cap));
    }
    
    private static List<RamPartition> partitionRam(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            RamRequirement ramReq,
            List<RamSpecification> ramSpecs) {
        return partitionLeafNode(
                updatableCapacities,
                ramReq,
                ramSpecs,
                (id, cap) -> new RamPartition(id, cap));
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static <P extends Partition, S extends CapacityEnabledSpecification, R extends CapacityEnabledRequirement> List<P>
            partitionLeafNode(
            Map<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            R req,
            List<S> specs,
            LeafConstructor<P> constructor) {
        List<P> partitions = new LinkedList<>();
        for (S spec : specs) {
            while (true) {
                Map<String, Object> specificationId = getSpecificationKeyValues((Specification) spec);
                BigDecimal consumeCapacity = consumeCapacity(updatableCapacities, spec, req);
                if (consumeCapacity == null) {
                    break;
                }
                P partition = constructor.construct(specificationId, consumeCapacity);
                partitions.add(partition);
            }
        }
        return partitions;
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

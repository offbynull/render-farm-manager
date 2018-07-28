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
import static java.math.BigDecimal.ONE;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);

        Map<String, Object> socketSpecKey = getSpecificationKeyValues(socketSpec);
        SocketPartition socketPartition = new SocketPartition(socketSpecKey);

        for (CoreRequirement coreReq : socketReq.getCoreRequirements()) {
            List<CoreSpecification> coreSpecs = socketSpec.getCoreSpecifications();
            if (coreReq.getCount() == null) {
                List<CorePartition> corePartitions = partitionAcrossAllCores(updatableCapacitiesCopy, coreReq, coreSpecs);
                socketPartition.addCorePartitions(corePartitions);
            } else {
                top:
                for (CoreSpecification coreSpec : coreSpecs) {
                    while (true) {
                        CorePartition corePartition = partitionIndividualCore(updatableCapacitiesCopy, coreReq, coreSpec);
                        if (corePartition == null) {
                            break;
                        }
                        socketPartition = socketPartition.addCorePartitions(corePartition);

                        coreReq = reduceRequirementCountRange(coreReq, 1);
                        if (coreReq == null) {
                            break top;
                        }
                    }
                }
            }
        }

        if (socketPartition.getCorePartitions().isEmpty()) {
            return null;
        }

        updatableCapacities.putAll(updatableCapacitiesCopy);
        return socketPartition;
    }
    
    private static CoreRequirement reviseRequirementCountRangeToHaveNoMinimum(CoreRequirement coreReq) {
        NumberRange countRange = coreReq.getCount();
        countRange = new NumberRange(ONE, countRange.getEnd());
        return new CoreRequirement(
                countRange,
                coreReq.getWhereCondition(),
                coreReq.getCpuRequirements().stream().toArray(i -> new CpuRequirement[i]));
    }
    
    private static CoreRequirement reduceRequirementCountRange(CoreRequirement coreReq, int count) {
        NumberRange countRange = coreReq.getCount();
        countRange = reduceRange(countRange, count);
        if (countRange == null) {
            return null;
        }
        return new CoreRequirement(
                countRange,
                coreReq.getWhereCondition(),
                coreReq.getCpuRequirements().stream().toArray(i -> new CpuRequirement[i]));
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    static CorePartition partitionIndividualCore(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CoreRequirement coreReq,
            CoreSpecification coreSpec) {
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);

        Map<String, Object> coreSpecKey = getSpecificationKeyValues(coreSpec);
        CorePartition corePartition = new CorePartition(coreSpecKey);

        for (CpuRequirement cpuReq : coreReq.getCpuRequirements()) {
            top:
            for (CpuSpecification cpuSpec : coreSpec.getCpuSpecifications()) {
                while (true) {
                    CpuPartition cpuPartition = partitionCpu(updatableCapacitiesCopy, cpuReq, cpuSpec);
                    if (cpuPartition == null) {
                        break;
                    }
                    corePartition = corePartition.addCpuPartitions(cpuPartition);
                    
                    cpuReq = reduceRequirementCountRange(cpuReq, 1);
                    if (cpuReq == null) {
                        break top;
                    }
                }
            }
        }

        if (corePartition.getCpuPartitions().isEmpty()) {
            return null;
        }

        updatableCapacities.putAll(updatableCapacitiesCopy);
        return corePartition;
    }
    
    static List<CorePartition> partitionAcrossAllCores(
            IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacities,
            CoreRequirement coreReq,
            List<CoreSpecification> coreSpecs) {
        IdentityHashMap<CapacityEnabledSpecification, BigDecimal> updatableCapacitiesCopy = new IdentityHashMap<>(updatableCapacities);

        Map<CoreSpecification, CorePartition> ret = new HashMap<>();
        for (CpuRequirement cpuReq : coreReq.getCpuRequirements()) {
            for (CoreSpecification coreSpec : coreSpecs) {
                List<CpuPartition> cpuPartitions = new LinkedList<>();
                
                // grab as many cpus as possible from all cores (up until the max)
                CpuRequirement noMinCpuReq = reviseRequirementCountRangeToHaveNoMinimum(cpuReq);
                top:
                for (CpuSpecification cpuSpec : coreSpec.getCpuSpecifications()) {
                    while (true) {
                        CpuPartition cpuPartition = partitionCpu(updatableCapacitiesCopy, noMinCpuReq, cpuSpec);
                        if (cpuPartition == null) { // can't partition this cpu anymore? skip to next cpu
                            break;
                        }
                        cpuPartitions.add(cpuPartition);
                        
                        noMinCpuReq = reduceRequirementCountRange(noMinCpuReq, cpuPartitions.size());
                        if (noMinCpuReq == null) { // reached the max count for this cpu req? break out
                            break top;
                        }
                    }
                }
                
                // if the number of cpus grabbed above doesn't meet the cpu requirement's count range, bail out of method
                //   we need to have a bind for all cpu reqs in the core req for this to be a successful bind, that's why we bail out
                int cpuCount = cpuPartitions.size();
                if (!cpuReq.getCount().isInRange(cpuCount)) {
                    return null;
                }
                
                // otherwise, add in the cpu partitions for that core spec
                CorePartition corePartition = ret.get(coreSpec);
                if (corePartition == null) {
                    corePartition = new CorePartition(coreSpec, cpuPartitions.stream().toArray(i -> new CpuPartition[i]));
                } else {
                    corePartition = corePartition.addCpuPartitions(cpuPartitions);
                }
                ret.put(coreSpec, corePartition);
                
                // reached the max count for this cpu req? break out so we don't scan over the other cores
                if (noMinCpuReq == null) {
                    break;
                }
            }
        }
        updatableCapacities.putAll(updatableCapacitiesCopy);
        return new ArrayList<>(ret.values());
    }
    
    private static CpuRequirement reviseRequirementCountRangeToHaveNoMinimum(CpuRequirement cpuReq) {
        NumberRange countRange = cpuReq.getCount();
        countRange = new NumberRange(ONE, countRange.getEnd());
        return new CpuRequirement(countRange, cpuReq.getWhereCondition(), cpuReq.getCapacityRange());
    }
    
    private static CpuRequirement reduceRequirementCountRange(CpuRequirement cpuReq, int count) {
        NumberRange countRange = cpuReq.getCount();
        countRange = reduceRange(countRange, count);
        if (countRange == null) {
            return null;
        }
        return new CpuRequirement(countRange, cpuReq.getWhereCondition(), cpuReq.getCapacityRange());
    }
    
    private static NumberRange reduceRange(NumberRange numberRange, int count) {
        BigDecimal newStart = numberRange.getStart().subtract(BigDecimal.valueOf(count));
        if (newStart.signum() <= 0) {
            newStart = ONE;
        }
        
        BigDecimal newEnd = numberRange.getEnd().subtract(BigDecimal.valueOf(count));
        if (newEnd.signum() <= 0) {
            return null;
        }
        
        return new NumberRange(newStart, newEnd);
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

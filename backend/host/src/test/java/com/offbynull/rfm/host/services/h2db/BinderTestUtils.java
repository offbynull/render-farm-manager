package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.partition.CorePartition;
import com.offbynull.rfm.host.model.partition.CpuPartition;
import com.offbynull.rfm.host.model.partition.GpuPartition;
import com.offbynull.rfm.host.model.partition.MountPartition;
import com.offbynull.rfm.host.model.partition.RamPartition;
import com.offbynull.rfm.host.model.partition.SocketPartition;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;
import static org.junit.Assert.assertEquals;

final class BinderTestUtils {
    private BinderTestUtils() {
        // do nothing
    }
    
    static void assertSocketPartition(SocketPartition socketPartition, long id, Consumer<CorePartition>... corePartitionTesters) {
        assertEquals(corePartitionTesters.length, socketPartition.getCorePartitions().size());
        assertEquals(
                Map.of("n_socket_id", BigDecimal.valueOf(id)),
                socketPartition.getSpecificationId());
        
        int idx = 0;
        for (CorePartition corePartition : socketPartition.getCorePartitions()) {
            corePartitionTesters[idx].accept(corePartition);
            idx++;
        }
    }
    
    static void assertCorePartition(CorePartition corePartition, long id, Consumer<CpuPartition>... cpuPartitionTesters) {
        assertEquals(cpuPartitionTesters.length, corePartition.getCpuPartitions().size());
        assertEquals(
                Map.of("n_core_id", BigDecimal.valueOf(id)),
                corePartition.getSpecificationId());
        
        int idx = 0;
        for (CpuPartition cpuPartition : corePartition.getCpuPartitions()) {
            cpuPartitionTesters[idx].accept(cpuPartition);
            idx++;
        }
    }
    
    static void assertCpuPartition(CpuPartition cpuPartition, long id, long capacity) {
        assertEquals(
                capacity,
                cpuPartition.getCapacity().longValueExact());
        assertEquals(
                Map.of("n_cpu_id", BigDecimal.valueOf(id)),
                cpuPartition.getSpecificationId());
    }
    
    static void assertGpuPartition(GpuPartition gpuPartition, String id, long capacity) {
        assertEquals(
                capacity,
                gpuPartition.getCapacity().longValueExact());
        assertEquals(
                Map.of("s_gpu_id", id),
                gpuPartition.getSpecificationId());
    }
    
    static void assertMountPartition(MountPartition mountPartition, String id, long capacity) {
        assertEquals(
                capacity,
                mountPartition.getCapacity().longValueExact());
        assertEquals(
                Map.of("s_target", id),
                mountPartition.getSpecificationId());
    }
    
    static void assertRamPartition(RamPartition ramPartition, long id, long capacity) {
        assertEquals(
                capacity,
                ramPartition.getCapacity().longValueExact());
        assertEquals(
                Map.of("n_ram_id", BigDecimal.valueOf(id)),
                ramPartition.getSpecificationId());
    }
}

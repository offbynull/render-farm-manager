package com.offbynull.rfm.host.model.partition;

import com.offbynull.rfm.host.model.specification.HostSpecification;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Map;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

public final class HostPartition extends Partition {
    private final UnmodifiableList<SocketPartition> socketPartitions;
    private final UnmodifiableList<GpuPartition> gpuPartitions;
    private final UnmodifiableList<RamPartition> ramPartitions;
    private final UnmodifiableList<MountPartition> mountPartitions;
    
    public HostPartition(Map<String, Object> specificationId,
            SocketPartition[] socketPartitions,
            GpuPartition[] gpuPartitions,
            RamPartition[] ramPartitions,
            MountPartition[] mountPartitions) {
        super(specificationId);
        
        Validate.notNull(socketPartitions);
        Validate.notNull(gpuPartitions);
        Validate.notNull(ramPartitions);
        Validate.notNull(mountPartitions);
        Validate.noNullElements(socketPartitions);
        Validate.noNullElements(gpuPartitions);
        Validate.noNullElements(ramPartitions);
        Validate.noNullElements(mountPartitions);
        Validate.isTrue(specificationId.keySet().equals(HostSpecification.getKeyPropertyNames()));
        
        this.socketPartitions = (UnmodifiableList<SocketPartition>) unmodifiableList(new ArrayList<>(asList(socketPartitions)));
        this.gpuPartitions = (UnmodifiableList<GpuPartition>) unmodifiableList(new ArrayList<>(asList(gpuPartitions)));
        this.ramPartitions = (UnmodifiableList<RamPartition>) unmodifiableList(new ArrayList<>(asList(ramPartitions)));
        this.mountPartitions = (UnmodifiableList<MountPartition>) unmodifiableList(new ArrayList<>(asList(mountPartitions)));
    }

    public UnmodifiableList<SocketPartition> getSocketPartitions() {
        return socketPartitions;
    }

    public UnmodifiableList<GpuPartition> getGpuPartitions() {
        return gpuPartitions;
    }

    public UnmodifiableList<RamPartition> getRamPartitions() {
        return ramPartitions;
    }

    public UnmodifiableList<MountPartition> getMountPartitions() {
        return mountPartitions;
    }
}

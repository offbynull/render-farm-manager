package com.offbynull.rfm.host.model.partition;

import com.offbynull.rfm.host.model.specification.CoreSpecification;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Map;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

public final class CorePartition extends Partition {
    private final UnmodifiableList<CpuPartition> cpuPartitions;
    
    public CorePartition(Map<String, Object> specificationId,
            CpuPartition[] cpuPartitions) {
        super(specificationId);
        
        Validate.notNull(cpuPartitions);
        Validate.noNullElements(cpuPartitions);
        Validate.isTrue(specificationId.keySet().equals(CoreSpecification.getKeyPropertyNames()));
        
        this.cpuPartitions = (UnmodifiableList<CpuPartition>) unmodifiableList(new ArrayList<>(asList(cpuPartitions)));
    }

    public UnmodifiableList<CpuPartition> getCpuPartitions() {
        return cpuPartitions;
    }
}

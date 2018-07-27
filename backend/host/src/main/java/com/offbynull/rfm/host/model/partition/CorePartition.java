package com.offbynull.rfm.host.model.partition;

import com.offbynull.rfm.host.model.specification.CoreSpecification;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

public final class CorePartition extends Partition {
    private final UnmodifiableList<CpuPartition> cpuPartitions;
    
    public CorePartition(CoreSpecification specification) {
        this(extractIdFromSpecification(specification));
    }
    
    public CorePartition(Map<String, Object> specificationId) {
        this(specificationId, new CpuPartition[0]);
    }

    public CorePartition(CoreSpecification specification,
            CpuPartition[] cpuPartitions) {
        this(extractIdFromSpecification(specification), cpuPartitions);
    }
    
    public CorePartition(Map<String, Object> specificationId,
            CpuPartition[] cpuPartitions) {
        super(specificationId);
        
        Validate.notNull(cpuPartitions);
        Validate.noNullElements(cpuPartitions);
        Validate.isTrue(specificationId.keySet().equals(CoreSpecification.getKeyPropertyNames()));
        
        this.cpuPartitions = (UnmodifiableList<CpuPartition>) unmodifiableList(new ArrayList<>(asList(cpuPartitions)));
    }

    private static Map<String, Object> extractIdFromSpecification(CoreSpecification spec) {
        Map<String, Object> id = new LinkedHashMap<>(spec.getProperties());
        id.keySet().retainAll(CoreSpecification.getKeyPropertyNames());
        return id;
    }
    
    public UnmodifiableList<CpuPartition> getCpuPartitions() {
        return cpuPartitions;
    }
    
    public CorePartition addCpuPartitions(CpuPartition... cpuPartitions) {
        return addCpuPartitions(asList(cpuPartitions));
    }
    
    public CorePartition addCpuPartitions(List<CpuPartition> cpuPartitions) {
        return new CorePartition(
                getSpecificationId(),
                ListUtils.union(this.cpuPartitions, cpuPartitions).stream().toArray(i -> new CpuPartition[i])
        );
    }
}

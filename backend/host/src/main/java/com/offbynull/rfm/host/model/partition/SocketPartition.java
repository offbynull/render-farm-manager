package com.offbynull.rfm.host.model.partition;

import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

public final class SocketPartition extends Partition {
    private final UnmodifiableList<CorePartition> corePartitions;
    
    public SocketPartition(SocketSpecification specification) {
        this(extractIdFromSpecification(specification));
    }
    
    public SocketPartition(Map<String, Object> specificationId) {
        this(specificationId, new CorePartition[0]);
    }

    public SocketPartition(SocketSpecification specification,
            CorePartition[] corePartitions) {
        this(extractIdFromSpecification(specification), corePartitions);
    }
    
    public SocketPartition(Map<String, Object> specificationId,
            CorePartition[] corePartitions) {
        super(specificationId);
        
        Validate.notNull(corePartitions);
        Validate.noNullElements(corePartitions);
        Validate.isTrue(specificationId.keySet().equals(SocketSpecification.getKeyPropertyNames()));
        
        this.corePartitions = (UnmodifiableList<CorePartition>) unmodifiableList(new ArrayList<>(asList(corePartitions)));
    }
    
    private static Map<String, Object> extractIdFromSpecification(SocketSpecification spec) {
        Map<String, Object> id = new LinkedHashMap<>(spec.getProperties());
        id.keySet().retainAll(SocketSpecification.getKeyPropertyNames());
        return id;
    }

    public UnmodifiableList<CorePartition> getCorePartitions() {
        return corePartitions;
    }
    
    public SocketPartition addCorePartitions(CorePartition... corePartitions) {
        return addCorePartitions(asList(corePartitions));
    }
    
    public SocketPartition addCorePartitions(List<CorePartition> corePartitions) {
        return new SocketPartition(
                getSpecificationId(),
                ListUtils.union(this.corePartitions, corePartitions).stream().toArray(i -> new CorePartition[i])
        );
    }
}

package com.offbynull.rfm.host.model.partition;

import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Map;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

public final class SocketPartition extends Partition {
    private final UnmodifiableList<CorePartition> corePartitions;
    
    public SocketPartition(Map<String, Object> specificationId,
            CorePartition[] corePartitions) {
        super(specificationId);
        
        Validate.notNull(corePartitions);
        Validate.noNullElements(corePartitions);
        Validate.isTrue(specificationId.keySet().equals(SocketSpecification.getKeyPropertyNames()));
        
        this.corePartitions = (UnmodifiableList<CorePartition>) unmodifiableList(new ArrayList<>(asList(corePartitions)));
    }

    public UnmodifiableList<CorePartition> getCorePartitions() {
        return corePartitions;
    }
}

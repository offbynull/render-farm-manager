package com.offbynull.rfm.host.model.partition;

import static com.offbynull.rfm.host.model.common.NumberCheckUtils.isAtLeast0;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class GpuPartition extends Partition implements CapacityEnabledPartition {
    private final BigDecimal capacity;
    
    public GpuPartition(Map<String, Object> specificationId, BigDecimal capacity) {
        super(specificationId);
        
        Validate.notNull(capacity);
        isAtLeast0(capacity);
        Validate.isTrue(specificationId.keySet().equals(GpuSpecification.getKeyPropertyNames()));
        
        this.capacity = capacity;
    }

    @Override
    public BigDecimal getCapacity() {
        return capacity;
    }
}

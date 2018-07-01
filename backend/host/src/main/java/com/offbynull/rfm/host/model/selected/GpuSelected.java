package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class GpuSelected extends Selected<GpuRequirement, GpuSpecification> {
    
    public GpuSelected(GpuRequirement selection, Set<GpuSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

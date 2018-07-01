package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.GpuRequirement;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class GpuSelection extends Selection<GpuRequirement, GpuSpecification> {
    
    public GpuSelection(GpuRequirement selection, Set<GpuSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

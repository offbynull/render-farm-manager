package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class CpuSelection extends Selection<CpuRequirement, CpuSpecification> {
    
    public CpuSelection(CpuRequirement selection, Set<CpuSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

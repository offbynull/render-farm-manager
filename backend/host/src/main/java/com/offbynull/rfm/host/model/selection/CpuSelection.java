package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.CpuRequirement;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class CpuSelection extends Selection<CpuRequirement, CpuSpecification> {
    
    public CpuSelection(CpuRequirement requirement, Set<CpuSpecification> specification) {
        super(requirement, specification);
        
        Validate.isTrue(requirement.getNumberRange().isInRange(specification.size()));

        NumberRange capacityRange = requirement.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

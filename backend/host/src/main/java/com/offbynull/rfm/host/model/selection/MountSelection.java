package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MountSelection extends Selection<MountRequirement, MountSpecification> {
    
    public MountSelection(MountRequirement requirement, Set<MountSpecification> specification) {
        super(requirement, specification);
        
        Validate.isTrue(requirement.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = requirement.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

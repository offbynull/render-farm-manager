package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MountSelection extends Selection<MountRequirement, MountSpecification> {
    
    public MountSelection(MountRequirement selection, Set<MountSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

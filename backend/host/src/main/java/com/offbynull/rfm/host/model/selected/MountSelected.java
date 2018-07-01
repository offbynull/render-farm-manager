package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.MountRequirement;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MountSelected extends Selected<MountRequirement, MountSpecification> {
    
    public MountSelected(MountRequirement selection, Set<MountSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

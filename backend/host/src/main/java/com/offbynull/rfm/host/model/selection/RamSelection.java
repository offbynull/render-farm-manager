package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.RamRequirement;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class RamSelection extends Selection<RamRequirement, RamSpecification> {
    
    public RamSelection(RamRequirement selection, Set<RamSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacityRequirement().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.selection.NumberRange;
import com.offbynull.rfm.host.model.selection.MountSelection;
import com.offbynull.rfm.host.model.specification.MountSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MountSelected extends Selected<MountSelection, MountSpecification> {
    
    public MountSelected(MountSelection selection, Set<MountSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacitySelection().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

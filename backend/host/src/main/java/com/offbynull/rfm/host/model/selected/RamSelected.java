package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.selection.NumberRange;
import com.offbynull.rfm.host.model.selection.RamSelection;
import com.offbynull.rfm.host.model.specification.RamSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class RamSelected extends Selected<RamSelection, RamSpecification> {
    
    public RamSelected(RamSelection selection, Set<RamSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacitySelection().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

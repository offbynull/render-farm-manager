package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.selection.NumberRange;
import com.offbynull.rfm.host.model.selection.CpuSelection;
import com.offbynull.rfm.host.model.specification.CpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class CpuSelected extends Selected<CpuSelection, CpuSpecification> {
    
    public CpuSelected(CpuSelection selection, Set<CpuSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacitySelection().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

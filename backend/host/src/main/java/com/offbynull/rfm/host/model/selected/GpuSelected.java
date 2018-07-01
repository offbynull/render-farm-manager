package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.selection.NumberRange;
import com.offbynull.rfm.host.model.selection.GpuSelection;
import com.offbynull.rfm.host.model.specification.GpuSpecification;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class GpuSelected extends Selected<GpuSelection, GpuSpecification> {
    
    public GpuSelected(GpuSelection selection, Set<GpuSpecification> specification) {
        super(selection, specification);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
        
        NumberRange capacityRange = selection.getCapacitySelection().getNumberRange();
        Validate.isTrue(specification.stream().allMatch(s -> capacityRange.isInRange(s.getCapacity())));
    }
    
}

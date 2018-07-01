package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class CoreSelected extends Selected<CoreRequirement, CoreSpecification> {
    
    private final UnmodifiableSet<CpuSelected> cpusSelected;
    
    public CoreSelected(CoreRequirement selection, Set<CoreSpecification> specification, Set<CpuSelected> cpusSelected) {
        super(selection, specification);
        
        this.cpusSelected = (UnmodifiableSet<CpuSelected>) unmodifiableSet(new HashSet<>(cpusSelected));
        
        isDistinctSpecifications(cpusSelected);
        isDistinctSelections(cpusSelected);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CpuSelected> getCpusSelected() {
        return cpusSelected;
    }

}

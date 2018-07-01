package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class CoreSelection extends Selection<CoreRequirement, CoreSpecification> {
    
    private final UnmodifiableSet<CpuSelection> cpusSelected;
    
    public CoreSelection(CoreRequirement selection, Set<CoreSpecification> specification, Set<CpuSelection> cpusSelected) {
        super(selection, specification);
        
        this.cpusSelected = (UnmodifiableSet<CpuSelection>) unmodifiableSet(new HashSet<>(cpusSelected));
        
        isDistinctSpecifications(cpusSelected);
        isDistinctSelections(cpusSelected);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CpuSelection> getCpusSelected() {
        return cpusSelected;
    }

}

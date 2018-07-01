package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.CoreRequirement;
import com.offbynull.rfm.host.model.specification.CoreSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class CoreSelection extends Selection<CoreRequirement, CoreSpecification> {
    
    private final UnmodifiableSet<CpuSelection> cpusSelection;
    
    public CoreSelection(CoreRequirement requirement, Set<CoreSpecification> specification, Set<CpuSelection> cpusSelection) {
        super(requirement, specification);
        
        this.cpusSelection = (UnmodifiableSet<CpuSelection>) unmodifiableSet(new HashSet<>(cpusSelection));
        
        isDistinctSpecifications(cpusSelection);
        isDistinctSelections(cpusSelection);
        
        Validate.isTrue(requirement.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CpuSelection> getCpusSelection() {
        return cpusSelection;
    }

}

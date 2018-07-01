package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class SocketSelecion extends Selection<SocketRequirement, SocketSpecification> {
    
    private final UnmodifiableSet<CoreSelection> coresSelection;
    
    public SocketSelecion(SocketRequirement requirement, Set<SocketSpecification> specification, Set<CoreSelection> coresSelection) {
        super(requirement, specification);
        
        this.coresSelection = (UnmodifiableSet<CoreSelection>) unmodifiableSet(new HashSet<>(coresSelection));
        
        isDistinctSpecifications(coresSelection);
        isDistinctSelections(coresSelection);
        
        Validate.isTrue(requirement.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CoreSelection> getCoresSelection() {
        return coresSelection;
    }
    
}

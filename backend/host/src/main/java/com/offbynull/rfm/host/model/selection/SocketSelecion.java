package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class SocketSelecion extends Selection<SocketRequirement, SocketSpecification> {
    
    private final UnmodifiableSet<CoreSelection> coresSelected;
    
    public SocketSelecion(SocketRequirement selection, Set<SocketSpecification> specification, Set<CoreSelection> coresSelected) {
        super(selection, specification);
        
        this.coresSelected = (UnmodifiableSet<CoreSelection>) unmodifiableSet(new HashSet<>(coresSelected));
        
        isDistinctSpecifications(coresSelected);
        isDistinctSelections(coresSelected);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CoreSelection> getCoresSelected() {
        return coresSelected;
    }
    
}

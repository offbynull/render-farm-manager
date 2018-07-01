package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.requirement.SocketRequirement;
import com.offbynull.rfm.host.model.specification.SocketSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class SocketSelected extends Selected<SocketRequirement, SocketSpecification> {
    
    private final UnmodifiableSet<CoreSelected> coresSelected;
    
    public SocketSelected(SocketRequirement selection, Set<SocketSpecification> specification, Set<CoreSelected> coresSelected) {
        super(selection, specification);
        
        this.coresSelected = (UnmodifiableSet<CoreSelected>) unmodifiableSet(new HashSet<>(coresSelected));
        
        isDistinctSpecifications(coresSelected);
        isDistinctSelections(coresSelected);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
    }

    public UnmodifiableSet<CoreSelected> getCoresSelected() {
        return coresSelected;
    }
    
}

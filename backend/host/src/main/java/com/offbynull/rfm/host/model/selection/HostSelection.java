package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class HostSelection extends Selection<HostRequirement, HostSpecification> {
    
    private final Set<SocketSelecion> socketsSelected;
    private final Set<GpuSelection> gpusSelected;
    private final Set<MountSelection> mountsSelected;
    private final Set<RamSelection> ramsSelected;

    public HostSelection(HostRequirement selection, Set<HostSpecification> specification,
            Set<SocketSelecion> socketsSelected,
            Set<GpuSelection> gpusSelected,
            Set<MountSelection> mountsSelected,
            Set<RamSelection> ramsSelected) {
        super(selection, specification);

        this.socketsSelected = (UnmodifiableSet<SocketSelecion>) unmodifiableSet(new HashSet<>(socketsSelected));
        this.gpusSelected = (UnmodifiableSet<GpuSelection>) unmodifiableSet(new HashSet<>(gpusSelected));
        this.mountsSelected = (UnmodifiableSet<MountSelection>) unmodifiableSet(new HashSet<>(mountsSelected));
        this.ramsSelected = (UnmodifiableSet<RamSelection>) unmodifiableSet(new HashSet<>(ramsSelected));
        
        isDistinctSpecifications(socketsSelected);
        isDistinctSpecifications(gpusSelected);
        isDistinctSpecifications(mountsSelected);
        isDistinctSpecifications(ramsSelected);
        isDistinctSelections(socketsSelected);
        isDistinctSelections(gpusSelected);
        isDistinctSelections(mountsSelected);
        isDistinctSelections(ramsSelected);
        
        Validate.isTrue(selection.getNumberRange().isInRange(specification.size()));
    }

    public Set<SocketSelecion> getSocketsSelected() {
        return socketsSelected;
    }

    public Set<GpuSelection> getGpusSelected() {
        return gpusSelected;
    }

    public Set<MountSelection> getMountsSelected() {
        return mountsSelected;
    }

    public Set<RamSelection> getRamsSelected() {
        return ramsSelected;
    }
}

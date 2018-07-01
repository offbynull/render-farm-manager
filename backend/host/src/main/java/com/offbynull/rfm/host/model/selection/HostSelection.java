package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class HostSelection extends Selection<HostRequirement, HostSpecification> {
    
    private final Set<SocketSelecion> socketsSelection;
    private final Set<GpuSelection> gpusSelection;
    private final Set<MountSelection> mountsSelection;
    private final Set<RamSelection> ramsSelection;

    public HostSelection(HostRequirement requirement, Set<HostSpecification> specification,
            Set<SocketSelecion> socketsSelection,
            Set<GpuSelection> gpusSelection,
            Set<MountSelection> mountsSelection,
            Set<RamSelection> ramsSelection) {
        super(requirement, specification);

        this.socketsSelection = (UnmodifiableSet<SocketSelecion>) unmodifiableSet(new HashSet<>(socketsSelection));
        this.gpusSelection = (UnmodifiableSet<GpuSelection>) unmodifiableSet(new HashSet<>(gpusSelection));
        this.mountsSelection = (UnmodifiableSet<MountSelection>) unmodifiableSet(new HashSet<>(mountsSelection));
        this.ramsSelection = (UnmodifiableSet<RamSelection>) unmodifiableSet(new HashSet<>(ramsSelection));
        
        isDistinctSpecifications(socketsSelection);
        isDistinctSpecifications(gpusSelection);
        isDistinctSpecifications(mountsSelection);
        isDistinctSpecifications(ramsSelection);
        isDistinctSelections(socketsSelection);
        isDistinctSelections(gpusSelection);
        isDistinctSelections(mountsSelection);
        isDistinctSelections(ramsSelection);
        
        Validate.isTrue(requirement.getNumberRange().isInRange(specification.size()));
    }

    public Set<SocketSelecion> getSocketsSelection() {
        return socketsSelection;
    }

    public Set<GpuSelection> getGpusSelection() {
        return gpusSelection;
    }

    public Set<MountSelection> getMountsSelection() {
        return mountsSelection;
    }

    public Set<RamSelection> getRamsSelection() {
        return ramsSelection;
    }
}

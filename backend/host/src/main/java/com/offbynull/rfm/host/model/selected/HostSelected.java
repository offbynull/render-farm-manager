package com.offbynull.rfm.host.model.selected;

import com.offbynull.rfm.host.model.selection.HostSelection;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class HostSelected extends Selected<HostSelection, HostSpecification> {
    
    private final Set<SocketSelected> socketsSelected;
    private final Set<GpuSelected> gpusSelected;
    private final Set<MountSelected> mountsSelected;
    private final Set<RamSelected> ramsSelected;

    public HostSelected(HostSelection selection, Set<HostSpecification> specification,
            Set<SocketSelected> socketsSelected,
            Set<GpuSelected> gpusSelected,
            Set<MountSelected> mountsSelected,
            Set<RamSelected> ramsSelected) {
        super(selection, specification);

        this.socketsSelected = (UnmodifiableSet<SocketSelected>) unmodifiableSet(new HashSet<>(socketsSelected));
        this.gpusSelected = (UnmodifiableSet<GpuSelected>) unmodifiableSet(new HashSet<>(gpusSelected));
        this.mountsSelected = (UnmodifiableSet<MountSelected>) unmodifiableSet(new HashSet<>(mountsSelected));
        this.ramsSelected = (UnmodifiableSet<RamSelected>) unmodifiableSet(new HashSet<>(ramsSelected));
        
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

    public Set<SocketSelected> getSocketsSelected() {
        return socketsSelected;
    }

    public Set<GpuSelected> getGpusSelected() {
        return gpusSelected;
    }

    public Set<MountSelected> getMountsSelected() {
        return mountsSelected;
    }

    public Set<RamSelected> getRamsSelected() {
        return ramsSelected;
    }
}

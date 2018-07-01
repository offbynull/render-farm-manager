package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.specification.Specification;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public abstract class Selection<T extends Requirement, U extends Specification> {
    private final T selection;
    private final UnmodifiableSet<U> specifications;

    Selection(T selection, Set<U> specifications) {
        Validate.notNull(selection);
        Validate.notNull(specifications);
        Validate.noNullElements(specifications);
        this.selection = selection;
        this.specifications = (UnmodifiableSet<U>) unmodifiableSet(new HashSet<>(specifications));
    }

    public T getSelection() {
        return selection;
    }

    public Set<U> getSpecifications() {
        return specifications;
    }
    
    public static void isDistinctSpecifications(Set<? extends Selection> selectedSet) {
        long distinctCount = selectedSet.stream().flatMap(x -> x.getSpecifications().stream()).distinct().count();
        long fullCount = selectedSet.stream().flatMap(x -> x.getSpecifications().stream()).count();
        Validate.isTrue(distinctCount == fullCount);
    }
    
    public static void isDistinctSelections(Set<? extends Selection> selectedSet) {
        long distinctCount = selectedSet.stream().map(x -> x.getSelection()).distinct().count();
        long fullCount = selectedSet.stream().map(x -> x.getSelection()).count();
        Validate.isTrue(distinctCount == fullCount);
    }
}

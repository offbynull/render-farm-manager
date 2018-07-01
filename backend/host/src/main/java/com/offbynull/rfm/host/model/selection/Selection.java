package com.offbynull.rfm.host.model.selection;

import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.requirement.RequirementType;
import com.offbynull.rfm.host.model.specification.Specification;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import static org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet;
import org.apache.commons.lang3.Validate;

public abstract class Selection<T extends Requirement, U extends Specification> {
    private final T requirement;
    private final UnmodifiableSet<U> specifications;

    Selection(T requirement, Set<U> specifications) {
        Validate.notNull(requirement);
        Validate.notNull(specifications);
        Validate.noNullElements(specifications);
        this.requirement = requirement;
        this.specifications = (UnmodifiableSet<U>) unmodifiableSet(new HashSet<>(specifications));
    }

    public T getRequirement() {
        return requirement;
    }

    public Set<U> getSpecifications() {
        return specifications;
    }
    
    /**
     * Checks to see if all specifications in a set are distinct.
     * @param selectionSet selection set
     * @return {@code true} if all selections in set are distinct, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element of {@code selectionSet} is {@code null}
     */
    public static boolean isDistinctSpecifications(Set<? extends Selection> selectionSet) {
        Validate.notNull(selectionSet);
        Validate.noNullElements(selectionSet);
        long distinctCount = selectionSet.stream().flatMap(x -> x.getSpecifications().stream()).distinct().count();
        long fullCount = selectionSet.stream().flatMap(x -> x.getSpecifications().stream()).count();
        return distinctCount == fullCount;
    }
    
    /**
     * Checks to see if all selections in a set are distinct.
     * @param selectionSet selection set
     * @return {@code true} if all selections in set are distinct, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element of {@code selectionSet} is {@code null}
     */
    public static boolean isDistinctSelections(Set<? extends Selection> selectionSet) {
        Validate.notNull(selectionSet);
        Validate.noNullElements(selectionSet);
        long distinctCount = selectionSet.stream().map(x -> x.getRequirement()).distinct().count();
        long fullCount = selectionSet.stream().map(x -> x.getRequirement()).count();
        return distinctCount == fullCount;
    }

    /**
     * Checks to see that the number of child specifications match that child requirement's expected range.
     * <p>
     * For child requirements of type {@link RequirementType#EACH}, each element in {@code childSpecificationList} must have a size that's
     * within {@code childRequirement}'s number range. For example, imagine the selection criteria...
     * <pre>
     * [1,5] parent {
     *   [5,10] child each
     * }
     * </pre>
     * For each parent specification, this method makes sure that it has between 5 to 10 child specifications EACH.
     * <p>
     * For child requirements of type {@link RequirementType#TOTAL}, the total number of specifications in {@code childSpecificationList}
     * must be within {@code childRequirement}'s number range.  For example, imagine the selection criteria...
     * <pre>
     * [1,5] parent {
     *   [2,4] child total
     * }
     * </pre>
     * This method makes sure that there are between 2 to 4 child specifications TOTAL across all parent specifications.
     * @param <PR> parent requirement type
     * @param <CR> child requirement type
     * @param <PS> parent specification type
     * @param <CS> child specification type
     * @param parentRequirement parent requirement
     * @param childRequirement child requirement
     * @param specifications map linking parent specification to child specifications
     * @return {@code true} if number of child specifications match requirement child requirement, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any element contains {@code null}
     */
    public static <PR extends Requirement, CR extends Requirement, PS extends Specification, CS extends Specification> boolean
            isCorrectCount(PR parentRequirement, CR childRequirement, MultiValuedMap<PS, CS> specifications) {
        Validate.notNull(parentRequirement);
        Validate.notNull(childRequirement);
        Validate.notNull(specifications);
        Validate.noNullElements(specifications.keySet());
        Validate.noNullElements(specifications.values());
        
        int parentSpecCount = specifications.size();
        Validate.isTrue(parentRequirement.getNumberRange().isInRange(parentSpecCount));

        RequirementType type = childRequirement.getRequirementType();
        switch (type) {
            case EACH: {
                NumberRange childNumberRange = childRequirement.getNumberRange();
                for (PS parentSpecification : specifications.keySet()) {
                    int count = specifications.get(parentSpecification).size();
                    if (!childNumberRange.isInRange(count)) {
                        return false;
                    }
                }
                return true;
            }
            case TOTAL: {
                NumberRange childNumberRange = childRequirement.getNumberRange();
                BigDecimal totalChildSpecCount = specifications.values().stream()
                        .map(x -> ONE)
                        .reduce((a,b) -> a.add(b))
                        .orElse(ZERO);
                return childNumberRange.isInRange(totalChildSpecCount);
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    public interface ChildSpecificationExtractor {
        Set<? extends Specification> get();
    }
}

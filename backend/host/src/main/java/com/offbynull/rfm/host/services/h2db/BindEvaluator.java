package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.parser.Parser;
import com.offbynull.rfm.host.model.requirement.Expression;
import com.offbynull.rfm.host.model.requirement.HostRequirement;
import com.offbynull.rfm.host.model.requirement.NumberRange;
import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.requirement.RequirementType;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.HostSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.model.work.Work;
import com.offbynull.rfm.host.model.worker.Worker;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.stream;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.toMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import com.offbynull.rfm.host.model.requirement.CapacityEnabledRequirement;

final class BindEvaluator {
    public static Set<Worker> evaluate(Work work, WorkerIterator workerIt) throws IOException {
        Validate.notNull(workerIt);
        Validate.notNull(work);
        
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(EMPTY_MAP);
        Parser parser = new Parser(EMPTY_LIST, EMPTY_LIST);
        
        String script = work.getRequirementsScript();
        UnmodifiableMap<String, Object> tags = work.getTags();
        
        HostRequirement hostSelection = parser.parseScriptReqs(tags, script);
        
        Set<Worker> foundWorkers = new HashSet<>();
        while (workerIt.hasNext()) {
            Worker worker = workerIt.next().getWorker();
            HostSpecification hostSpecification = worker.getHostSpecification();

            Expression whereExpr = hostSelection.getWhereCondition();
            boolean hostMatches = (Boolean) expressionEvaluator.evaluate(whereExpr, tags);
            if (!hostMatches) {
                continue;
            }

            Set<HostSpecification> foundSpec = evaluateSelection(
                    expressionEvaluator,
                    tags,
                    NumberRange.SINGLE,
                    hostSelection,
                    Set.of(hostSpecification));
            
            Validate.validState(foundSpec.size() <= 1); // sanity check
            if (foundSpec.size() == 1) {
                foundWorkers.add(worker);
            }
            
            int len = foundWorkers.size();
            if (hostSelection.getNumberRange().compareEnd(len) >= 0) {
                break;
            }
        }
        
        int len = foundWorkers.size();
        if (hostSelection.getNumberRange().compareStart(len) >= 0) {
            return foundWorkers;
        }
        
        return null;
    }
    
    private static MultiValuedMap<String, Specification> drill(
            ExpressionEvaluator expressionEvaluator,
            Map<String, Object> scriptTags,
            Requirement selection,
            Specification specification) {
        Map<String, Method> selectionMethods = getChildSelectMethods(selection);
        Map<String, Method> specificationMethods = getChildSpecificationMethods(specification);
        
        selectionMethods.remove("capacity");
        specificationMethods.remove("capacity");
        
        MultiValuedMap<String, Specification> ret = new HashSetValuedHashMap<>();
        for (String name : selectionMethods.keySet()) {
            Method selectionMethod = selectionMethods.get(name);
            Method specificationMethod = specificationMethods.get(name);
            
            UnmodifiableList<Requirement> childSelects;
            UnmodifiableList<Specification> childSpecs;
            
            try {
                childSelects = (UnmodifiableList<Requirement>) selectionMethod.invoke(selection);
                childSpecs = (UnmodifiableList<Specification>) specificationMethod.invoke(specification);
            } catch (ReflectiveOperationException roe) {
                throw new IllegalStateException(roe); // should never happen
            }
            
            Set<Specification> remainingSpecs = new HashSet<>(childSpecs);
            for (Requirement childSelect : childSelects) {
                RequirementType selectionType = childSelect.getRequirementType();
                NumberRange numberRange = childSelect.getNumberRange();
                top:
                while (!remainingSpecs.isEmpty()) {
                    Set<Specification> foundSpecs = evaluateSelection(expressionEvaluator, scriptTags, numberRange, childSelect, remainingSpecs);
                    if (foundSpecs == null) {
                        return null;
                    }

                    ret.putAll(name, foundSpecs);
                    switch (selectionType) {
                        case EACH: // Each child needs to be within the number range specified
                            break;
                        case TOTAL: // All children combined need to be within the number range
                            numberRange = subtract(numberRange, foundSpecs.size());
                            if (numberRange == null) { // we've got the max we can take, break out of loop
                                break top;
                            }
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
            }
        }
        
        return ret;
    }
    
    private static Map<String, Method> getChildSelectMethods(Requirement selection) {
        return stream(selection.getClass().getDeclaredMethods())
                .filter(m -> m.getName().startsWith("get") && m.getName().endsWith("Selections"))
                .filter(m -> (m.getModifiers() & Modifier.PUBLIC) != 0)
                .filter(m -> (m.getModifiers() & Modifier.STATIC) == 0)
                .filter(m -> m.getParameterCount() == 0)
                .collect(toMap(m -> {
                    String ret = m.getName();
                    ret = StringUtils.removeStart(ret, "get");
                    ret = StringUtils.removeEnd(ret, "Selections");
                    ret = StringUtils.uncapitalize(ret);
                    return ret;
                }, m -> m));
    }
    
    private static Map<String, Method> getChildSpecificationMethods(Specification specification) {
        return stream(specification.getClass().getDeclaredMethods())
                .filter(m -> m.getName().startsWith("get") && m.getName().endsWith("Specification"))
                .filter(m -> (m.getModifiers() & Modifier.PUBLIC) != 0)
                .filter(m -> (m.getModifiers() & Modifier.STATIC) == 0)
                .filter(m -> m.getParameterCount() == 0)
                .collect(toMap(m -> {
                    String ret = m.getName();
                    ret = StringUtils.removeStart(ret, "get");
                    ret = StringUtils.removeEnd(ret, "Selections");
                    ret = StringUtils.uncapitalize(ret);
                    return ret;
                }, m -> m));
    }
    
    private static NumberRange subtract(NumberRange numberRange, int size) {
        BigDecimal sizeBd = BigDecimal.valueOf(size);
        BigDecimal newStart = numberRange.getStart().subtract(sizeBd);
        BigDecimal newEnd = numberRange.getEnd().subtract(sizeBd);
        
        if (newEnd.compareTo(ZERO) <= 0) {
            return null; // we got it all, null means stop
        }
        
        if (newStart.compareTo(ZERO) <= 0) {
            newStart = ONE;
        }
        
        return new NumberRange(newStart, newEnd);
    }
    
    private static <T extends Requirement, U extends Specification> Set<U> evaluateSelection(
            ExpressionEvaluator expressionEvaluator,
            Map<String, Object> scriptTags,
            NumberRange selectionRange,
            T selection,
            Set<U> remainingSpecifications) {
        Set<U> foundSpecifications = new HashSet<>();

        // DO NOT USE selection.getNumberRange() -- use selectionRange instead because we want to support TOTAL and EACH.
        int min = selectionRange.getStart().intValueExact();
        int max = selectionRange.getEnd().intValueExact();

        for (U remainingSpecification : remainingSpecifications) {
            Expression whereExpr = selection.getWhereCondition();
            Map<String, Object> whereVars = new HashMap<>();
            whereVars.putAll(scriptTags);
            whereVars.putAll(remainingSpecification.getProperties());

            // Does the where expression evaluate to true?
            boolean matches = (Boolean) expressionEvaluator.evaluate(whereExpr, whereVars);
            if (!matches) {
                continue;
            }

            // Does the spec have enough capacity?
            boolean selectCapEnabled = selection instanceof CapacityEnabledRequirement;
            boolean specCapEnabled = remainingSpecification instanceof CapacityEnabledSpecification;
              // sanity check -- selection and spec must both have capacity OR both not have capacity
            Validate.isTrue(!(selectCapEnabled ^ specCapEnabled));
              // if cap enabled, skip if not enough available
            if (selectCapEnabled && specCapEnabled) {
                NumberRange capacitySelectionRange = ((CapacityEnabledRequirement) selection).getCapacityRequirement().getNumberRange();
                BigDecimal capacity = ((CapacityEnabledSpecification) remainingSpecification).getCapacity();
                if (capacitySelectionRange.getStart().compareTo(capacity) < 0) {
                    continue;
                }
            }

            // Do all the children match?
            MultiValuedMap<String, Specification> children = drill(expressionEvaluator, scriptTags, selection, remainingSpecification);
            if (children == null) {
                continue;
            }

            // Add the spec. If reached selection max, don't continue grabbing anymore
            foundSpecifications.add(remainingSpecification);
            if (foundSpecifications.size() >= max) {
                break;
            }
        }

        // Ensure we reached min
        if (foundSpecifications.size() < min) {
            return null;
        }

        remainingSpecifications.removeAll(foundSpecifications);
        return foundSpecifications;
    }
}

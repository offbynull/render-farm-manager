package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.union;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ClassUtils;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }
    
    static String getRequirementName(Requirement requirement) {
        return getRequirementName(requirement.getClass());
    }
    
    static String getRequirementName(Class<?> requirementCls) {
        String className = requirementCls.getSimpleName();
        Validate.isTrue(className.endsWith("Requirement"));
        String name = className;
        name = removeEnd(name, "Requirement");
        name = uncapitalize(name);
        return name;
    }
    
    static String getRequirementName(Method method) {
        String methodName = method.getName();
        Validate.isTrue(methodName.startsWith("get"));
        Validate.isTrue(methodName.endsWith("Requirements"));
        String name = methodName;
        name = removeStart(name, "get");
        name = removeEnd(name, "Requirements");
        name = uncapitalize(name);
        return name;
    }
    
    static Set<String> getRequirementKey(Requirement requirement) {
        return getRequirementKey(requirement.getClass());
    }
    
    static Set<String> getRequirementKey(Class<?> requirementCls) {
        String name = getRequirementName(requirementCls);

        String specClassStr = "com.offbynull.rfm.host.model.specification." + capitalize(name) + "Specification";
        Class<?> specClass;
        try {
            specClass = requirementCls.getClassLoader().loadClass(specClassStr);
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException(roe);
        }
        
        return getSpecificationKey(specClass);
    }
    
    static Set<String> getRequirementFullKey(List<Requirement> requirementChain) {
        return getRequirementFullKeyFromClasses(requirementChain.stream().map(s -> s.getClass()).collect(toList()));
    }
    
    static Set<String> getRequirementFullKeyFromClasses(List<Class<? extends Requirement>> requirementChain) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Class<? extends Requirement> req : requirementChain) {
            Set<String> key = getRequirementKey(req);
            ret.addAll(key);
        }
        
        return ret;
    }
    
    static List<Requirement> localizeChain(List<Requirement> requirementChain, Requirement requirement) {
        int idxInRequirementChain = requirementChain.indexOf(requirement);
        List<Requirement> localRequirementChain = requirementChain.subList(0, idxInRequirementChain + 1);
        return new ArrayList<>(localRequirementChain);
    }
    
    static List<Class<? extends Requirement>> toClasses(List<Requirement> requirementChain) {
        return requirementChain.stream()
                .map(x -> x.getClass())
                .collect(toList());
    }
    
    static MultiValuedMap<String, Requirement> getRequirementChildren(Requirement requirement) {
        ArrayListValuedHashMap<String, Requirement> childRequirements = stream(requirement.getClass().getMethods())
                .filter(m -> m.getName().startsWith("get") && m.getName().endsWith("Requirements"))
                .filter(m -> ClassUtils.isAssignable(m.getReturnType(), UnmodifiableList.class))
                .filter(m -> m.getParameterCount() == 0)
                .collect(ArrayListValuedHashMap::new,
                        (c, m) -> c.putAll(getRequirementName(m), invokeRequirementGetter(m, requirement)),
                        (c, m) -> c.putAll(m));
        
        return childRequirements;
    }
    
    static UnmodifiableList<Requirement> invokeRequirementGetter(Method method, Requirement object) {
        try {
            UnmodifiableList<Requirement> childRequirements = (UnmodifiableList<Requirement>) method.invoke(object);
            
            long typesReturnedByMethod = childRequirements.stream().map(cr -> cr.getClass()).distinct().count();
            Validate.validState(typesReturnedByMethod == 1L);
            
            return childRequirements;
        } catch (IllegalArgumentException | ReflectiveOperationException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
    }
    
    static List<List<Requirement>> flattenRequirementHierarchy(Requirement requirement) {
        List<List<Requirement>> collection = new ArrayList<>();
        List<Requirement> parentRequirementChain = List.of();
        
        flattenRequirementHierarchy(collection, parentRequirementChain, requirement);
        
        return collection;
    }
            
    private static void flattenRequirementHierarchy(
            List<List<Requirement>> collection,
            List<Requirement> parentRequirementChain,
            Requirement requirement) {
        List<Requirement> requirementChain = union(parentRequirementChain, List.of(requirement));
        collection.add(requirementChain);
        
        for (Requirement childRequirement : getRequirementChildren(requirement).values()) {
            flattenRequirementHierarchy(collection, requirementChain, childRequirement);
        }
    }
    
    
    
    
    
    
    
    
    
    static String getSpecificationName(Specification specification) {
        return getSpecificationName(specification.getClass());
    }
    
    static String getSpecificationName(Class<?> specificationCls) {
        String className = specificationCls.getSimpleName();
        Validate.isTrue(className.endsWith("Specification"));
        String name = className;
        name = removeEnd(name, "Specification");
        name = uncapitalize(name);
        return name;
    }
    
    static String getSpecificationName(Method method) {
        String methodName = method.getName();
        Validate.isTrue(methodName.startsWith("get"));
        Validate.isTrue(methodName.endsWith("Specifications"));
        String name = methodName;
        name = removeStart(name, "get");
        name = removeEnd(name, "Specifications");
        name = uncapitalize(name);
        return name;
    }
    
    static Set<String> getSpecificationKey(Specification specification) {
        return getSpecificationKey(specification.getClass());
    }
    
    static Set<String> getSpecificationKey(Class<?> specificationCls) {
        try {
            return (Set<String>) MethodUtils.invokeStaticMethod(specificationCls, "getKeyPropertyNames");
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException(roe);
        }
    }
    
    static Set<String> getSpecificationFullKey(List<Specification> specificationChain) {
        return getSpecificationFullKeyFromClasses(specificationChain.stream().map(s -> s.getClass()).collect(toList()));
    }
    
    static Set<String> getSpecificationFullKeyFromClasses(List<Class<? extends Specification>> specificationChain) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Class<? extends Specification> req : specificationChain) {
            Set<String> key = getSpecificationKey(req);
            ret.addAll(key);
        }
        
        return ret;
    }
    
    static MultiValuedMap<String, Specification> getSpecificationChildren(Specification specification) {
        ArrayListValuedHashMap<String, Specification> childSpecifications = stream(specification.getClass().getMethods())
                .filter(m -> m.getName().startsWith("get") && m.getName().endsWith("Specifications"))
                .filter(m -> ClassUtils.isAssignable(m.getReturnType(), UnmodifiableList.class))
                .filter(m -> m.getParameterCount() == 0)
                .collect(ArrayListValuedHashMap::new,
                        (c, m) -> c.putAll(getSpecificationName(m), invokeSpecificationGetter(m, specification)),
                        (c, m) -> c.putAll(m));
        
        return childSpecifications;
    }
    
    static UnmodifiableList<Specification> invokeSpecificationGetter(Method method, Specification object) {
        try {
            UnmodifiableList<Specification> childSpecifications = (UnmodifiableList<Specification>) method.invoke(object);
            
            long typesReturnedByMethod = childSpecifications.stream().map(cr -> cr.getClass()).distinct().count();
            Validate.validState(typesReturnedByMethod == 1L);
            
            return childSpecifications;
        } catch (IllegalArgumentException | ReflectiveOperationException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
    }
    
    static BigDecimal getSpecificationCapacity(Specification specification) {
        Class<? extends Specification> cls = specification.getClass();
        
        boolean correctInterface = stream(cls.getInterfaces()).filter(x -> x == CapacityEnabledSpecification.class).count() == 1L;
        if (!correctInterface) {
            return null;
        }
        
        Method method = MethodUtils.getAccessibleMethod(cls, "getCapacity");
        Validate.validState(method != null); // sanity check -- if of correct interface, it should contain this method
        
        BigDecimal capacity;
        try {
            capacity = (BigDecimal) method.invoke(specification);
        } catch (ClassCastException | ReflectiveOperationException roe) {
            throw new IllegalStateException(roe); // should never happen
        }
        
        Validate.validState(capacity != null); // sanity check -- if the method exists, it should never return null
        return capacity;
    }
}

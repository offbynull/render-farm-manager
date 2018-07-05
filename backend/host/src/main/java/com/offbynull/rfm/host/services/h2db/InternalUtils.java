package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.requirement.Requirement;
import java.lang.reflect.Method;
import static java.util.Arrays.stream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ClassUtils;
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
    
    static String getRequirementName(Class<?> requirement) {
        String className = requirement.getClass().getSimpleName();
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
        return InternalUtils.getRequirementKey(requirement.getClass());
    }
    
    static Set<String> getRequirementKey(Class<?> requirementCls) {
        String name = getRequirementName(requirementCls);

        String specClassStr = "com.offbynull.rfm.host.model.specification." + name + "Specification";
        try {
            Class<?> specClass = requirementCls.getClassLoader().loadClass(specClassStr);
            return (Set<String>) MethodUtils.invokeStaticMethod(specClass, "getKeyPropertyNames");
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException(roe);
        }
    }
    
    static Set<String> getRequirementFullKey(List<Requirement> requirementChain) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Requirement req : requirementChain) {
            Set<String> key = getRequirementKey(req);
            ret.addAll(key);
        }
        
        return ret;
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
    
    static UnmodifiableList<? extends Requirement> invokeRequirementGetter(Method method, Requirement object) {
        try {
            UnmodifiableList<? extends Requirement> childRequirements = (UnmodifiableList<? extends Requirement>) method.invoke(object);
            
            long typesReturnedByMethod = childRequirements.stream().map(cr -> cr.getClass()).distinct().count();
            Validate.validState(typesReturnedByMethod == 1L);
            
            return childRequirements;
        } catch (IllegalArgumentException | ReflectiveOperationException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
    }
}

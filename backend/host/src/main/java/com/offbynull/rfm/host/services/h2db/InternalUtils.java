package com.offbynull.rfm.host.services.h2db;

import com.offbynull.rfm.host.model.requirement.Requirement;
import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.union;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ClassUtils;
import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import org.apache.commons.lang3.Validate;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;
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
        Validate.validState(className.endsWith("Requirement"));
        String name = className;
        name = removeEnd(name, "Requirement");
        name = uncapitalize(name);
        return name;
    }
    
    static String getRequirementName(Method method) {
        String methodName = method.getName();
        Validate.validState(methodName.startsWith("get"));
        Validate.validState(methodName.endsWith("Requirements"));
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
    
    static Set<String> getRequirementFullKeyFromClasses(List<Class<? extends Requirement>> requirementChainClses) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Class<? extends Requirement> reqCls : requirementChainClses) {
            Set<String> key = getRequirementKey(reqCls);
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
        Validate.validState(className.endsWith("Specification"));
        String name = className;
        name = removeEnd(name, "Specification");
        name = uncapitalize(name);
        return name;
    }
    
    static String getSpecificationName(Method method) {
        String methodName = method.getName();
        Validate.validState(methodName.startsWith("get"));
        Validate.validState(methodName.endsWith("Specifications"));
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
    
    static Map<String, Object> getSpecificationKeyValues(Specification specification) {
        LinkedHashMap<String, Object> ret = new LinkedHashMap<>();

        Set<String> key = getSpecificationKey(specification);
        for (String keyItem : key) {
            Object value = specification.getProperties().get(keyItem);
            ret.put(keyItem, value);
        }
        
        return ret;
    }
    
    static Map<String, Object> reduceToSpecificationKey(Class<?> specificationCls, Map<String, Object> map) {
        Set<String> key = getSpecificationKey(specificationCls);
        
        Map<String, Object> keyValues = new HashMap<>(map);
        keyValues.keySet().retainAll(key);
        Validate.isTrue(keyValues.keySet().containsAll(key));
        
        return keyValues;
    }
    
    static Set<String> getSpecificationFullKey(List<Specification> specificationChain) {
        return getSpecificationFullKeyFromClasses(specificationChain.stream().map(s -> s.getClass()).collect(toList()));
    }
    
    static Set<String> getSpecificationFullKeyFromClasses(List<Class<? extends Specification>> specificationChainClses) {
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        
        for (Class<? extends Specification> specCls : specificationChainClses) {
            Set<String> key = getSpecificationKey(specCls);
            ret.addAll(key);
        }
        
        return ret;
    }

    static Map<String, Object> getSpecificationFullKeyValues(List<Specification> specificationChain) {
       LinkedHashMap<String, Object> ret = new LinkedHashMap<>();
        
        for (Specification spec : specificationChain) {
            Set<String> key = getSpecificationKey(spec);
            for (String keyItem : key) {
                Object value = spec.getProperties().get(keyItem);
                ret.put(keyItem, value);
            }
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
    
    static Set<Class<? extends Specification>> getSpecificationChildClasses(Class<? extends Specification> specificationCls) {
        // derived from constructor params
        
        // single constructor
        Constructor<?>[] constructors = specificationCls.getConstructors();
        Validate.validState(constructors.length == 1);
        Constructor<?> constructor = constructors[0];
        Class<?>[] constructorParams = constructor.getParameterTypes();
        
        // split out child specification parameters
        Class<?>[] specParams = stream(constructorParams)
                .filter(p -> isAssignable(p, Specification[].class))
                .map(p -> p.getComponentType())
                .toArray(len -> new Class<?>[len]);
        if (specParams.length > 0) { // children must be first params in constructor -- this block checks for this
            int lastIdx = specParams.length - 1;
            Validate.validState(specParams[0] == constructorParams[0].getComponentType());
            Validate.validState(specParams[lastIdx] == constructorParams[lastIdx].getComponentType());
        }
        
        // add to set and return
        Set<Class<? extends Specification>> specChildTypes = new LinkedHashSet<>();
        stream(specParams).forEach(s -> specChildTypes.add((Class<? extends Specification>) s));
        Validate.validState(specChildTypes.size() == specParams.length); // child types must be unique
        
        return specChildTypes;
    }
    
    static Specification constructSpecification(Class<? extends Specification> specCls, Set<Specification> children,
            Map<String, Object> properties, BigDecimal capacity) {
        Set<Class<? extends Specification>> childClses = getSpecificationChildClasses(specCls);
        if (isAssignable(specCls, CapacityEnabledSpecification.class)) {
            Validate.validState(capacity != null);
        }

        // create args array
        int argsLen = childClses.size() + 1; // add 1 for the properties map
        if (capacity != null) {              // add 1 for capacity
            argsLen++;
        }
        Object[] args = new Object[argsLen];

        // add child specs to args
        int idx = 0;
        for (Class<?> childCls : childClses) {
            Object param = children.stream()
                .filter(child -> isAssignable(child.getClass(), childCls))
                .map(child -> childCls.cast(child))
                .toArray(len -> (Object[]) Array.newInstance(childCls, len));
            args[idx] = param;
            idx++;
        }
        
        // add capacity map to args
        if (capacity != null) {
            args[idx] = capacity;
            idx++;
        }
        
        // add properties map to args
        args[idx] = properties;
        
        // construct
        try {
            return invokeConstructor(specCls, args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
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
    
    static boolean isSpecificationCapacityEnabled(Class<? extends Specification> specificationCls) {
        return stream(specificationCls.getInterfaces())
                .filter(x -> x == CapacityEnabledSpecification.class)
                .count() == 1L;
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
    
    
    
    
    
    
    
    
    
    public PreparedStatement prepareQuery(Connection conn, Query query) throws SQLException {
        PreparedStatement ret = conn.prepareStatement(query.toJdbcQuery());
        try {
            int counter = 1;
            List<Object> params = query.toJdbcParameters();
            for (Object param : params) {
                ret.setObject(counter, param);
                counter++;
            }
        } catch (SQLException sqle) {
            ret.close();
            throw sqle;
        }
        return ret;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    static Class<?> toArrayClass(Class<?> componentType, int... dimensions) {
        return Array.newInstance(componentType, dimensions).getClass();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    static String toWorkerCursor(String host, int port) {
        return host + ":" + port;
    }
    
    static DecomposedWorkerCursor fromWorkerCursor(String cursor) throws IOException {
        int splitIdx = cursor.lastIndexOf(':');
        if (splitIdx == -1) {
            throw new IOException("Bad cursor");
        }
        
        String host = cursor.substring(0, splitIdx);
        int port;
        try {
            port = Integer.valueOf(cursor.substring(splitIdx + 1));
        } catch (NumberFormatException nfe) {
            throw new IOException("Bad cursor", nfe);
        }
        try {
            Validate.notEmpty(host);
            Validate.isTrue(port >= 1 && port <= 65535);
        } catch (IllegalArgumentException iae) {
            throw new IOException("Bad cursor", iae);
        }
        
        return new DecomposedWorkerCursor(host, port);
    }
    
    static final class DecomposedWorkerCursor {
        private final String host;
        private final int port;

        private DecomposedWorkerCursor(String host, int port) {
            Validate.notNull(host);
            Validate.isTrue(port >= 1 && port <= 65535);
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
        
    }
    
    
    
    
    
    static String toWorkCursor(BigDecimal priority, String id) {
        return priority.stripTrailingZeros().toPlainString() + ":" + id;
    }
    
    static DecomposedWorkCursor fromWorkCursor(String cursor) throws IOException {
        int splitIdx = cursor.lastIndexOf(':');
        if (splitIdx == -1) {
            throw new IOException("Bad cursor");
        }
        
        BigDecimal priority;
        try {
            priority = new BigDecimal(cursor.substring(0, splitIdx));
        } catch (NumberFormatException nfe) {
            throw new IOException("Bad cursor", nfe);
        }
        String id = cursor.substring(splitIdx + 1);
        try {
            Validate.notEmpty(id);
        } catch (IllegalArgumentException iae) {
            throw new IOException("Bad cursor", iae);
        }
        
        return new DecomposedWorkCursor(priority, id);
    }
    
    static final class DecomposedWorkCursor {
        private final BigDecimal priority;
        private final String id;

        private DecomposedWorkCursor(BigDecimal priority, String id) {
            Validate.notNull(priority);
            Validate.notNull(id);
            this.priority = priority.stripTrailingZeros();
            this.id = id;
        }

        public BigDecimal getPriority() {
            return priority;
        }

        public String getId() {
            return id;
        }
    }
}

package com.offbynull.rfm.host.testutils;

import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.testutils.WorkerLexer.CloseBracketLexerToken;
import com.offbynull.rfm.host.testutils.WorkerLexer.ColonLexerToken;
import com.offbynull.rfm.host.testutils.WorkerLexer.IdLexerToken;
import com.offbynull.rfm.host.testutils.WorkerLexer.NumberLexerToken;
import com.offbynull.rfm.host.testutils.WorkerLexer.OpenBracketLexerToken;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;

public final class WorkerParser {
    private WorkerParser() {
        // do nothing
    }
    
    
    public static Specification specification(LexerTokenReader r) throws IOException {
        r.mark();
        
        Header header = header(r);
        if (header == null) {
            r.resetMark();
            return null;
        }
        
        if (!r.matches(OpenBracketLexerToken.class)) {
            r.resetMark();
            return null;
        }
        
        r.read(); // read and discard opening bracket

        List<Specification> specs = new ArrayList<>();
        Map<String, Object> props = new HashMap<>();
        while (!r.matches(CloseBracketLexerToken.class)) {
            Specification spec = specification(r);
            if (spec != null) {
                specs.add(spec);
                continue;
            }
            
            Property prop = property(r);
            if (prop != null) {
                props.put(prop.name, prop.value);
                continue;
            }
            
            r.resetMark();
            return null;
        }
        
        r.read(); // read and discard closing bracket

        r.discardMark();
        try {
            Class<?> specClass = WorkerParser.class.getClassLoader().loadClass("com.offbynull.rfm.host.model.specification." + capitalize(header.name) + "Specification");
            Constructor<?> specConstructor = specClass.getConstructors()[0];
            Object[] args = stream(specConstructor.getParameterTypes())
                    .filter(c -> c.isArray())
                    .map(c -> c.getComponentType())
                    .filter(c -> !c.isArray())
                    .map(c -> specs.stream()
                            .filter(cs -> c.isInstance(cs))
                            .toArray(len -> (Specification[]) Array.newInstance(c, len))
                    )
                    .toArray();
            
            // add capacity arg
            if (header.capacity != null) {
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = header.capacity;
            }
            
            // add props arg
            args = Arrays.copyOf(args, args.length + 1);
            args[args.length - 1] = props;
            
            return (Specification) invokeConstructor(specClass, args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public static Header header(LexerTokenReader r) {
        r.mark();
        
        String name;
        BigDecimal capacity;
        if (!r.matches(IdLexerToken.class)) {
            r.resetMark();
            return null;
        }

        name = r.read().getText();
        if (r.matches(ColonLexerToken.class, NumberLexerToken.class)) {
            r.read(); // discard colon
            capacity = new BigDecimal(r.read().getText());
        } else {
            capacity = null;
        }

        r.discardMark();
        return new Header(name, capacity);
    }
    
    public static final class Header {
        private final String name;
        private final BigDecimal capacity;

        public Header(String name, BigDecimal capacity) {
            this.name = name;
            this.capacity = capacity;
        }
    }
    
    public static Property property(LexerTokenReader r) throws IOException {
        r.mark();
        
        if (!r.matches(IdLexerToken.class, ColonLexerToken.class, NumberLexerToken.class)
                && !r.matches(IdLexerToken.class, ColonLexerToken.class, IdLexerToken.class)) {
            r.resetMark();
            return null;
        }
        
        String propNameStr = r.read().getText();
        r.read(); // discard colon
        String propValueStr = r.read().getText();
        if (propNameStr.length() < 2) {
            r.resetMark();
            return null;
        }
        switch (propNameStr.substring(0, 2)) {
            case "b_":
                switch (propValueStr) {
                    case "true":
                        r.discardMark();
                        return new Property(propNameStr, true);
                    case "false":
                        r.discardMark();
                        return new Property(propNameStr, false);
                    default:
                        throw new IllegalStateException();
                }
            case "n_":
                r.discardMark();
                return new Property(propNameStr, new BigDecimal(propValueStr));
            case "s_":
                r.discardMark();
                return new Property(propNameStr, propValueStr);
            default:
                r.resetMark();
                return null;
        }
    }
    
    public static final class Property {
        private final String name;
        private final Object value;

        public Property(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}

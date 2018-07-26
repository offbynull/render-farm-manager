package com.offbynull.rfm.host.testutils;

import com.offbynull.rfm.host.model.specification.CapacityEnabledSpecification;
import com.offbynull.rfm.host.model.specification.Specification;
import com.offbynull.rfm.host.testutils.WorkerLexer.LexerToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.List;
import org.apache.commons.io.IOUtils;

public final class TestUtils {
    private TestUtils() {
        // do nothing
    }
    
    public static Specification loadSpecResource(String name) throws ClassNotFoundException, IOException {
        try (InputStream is = TestUtils.class.getResourceAsStream(name)) {
            String str = IOUtils.toString(is, StandardCharsets.UTF_8);
            
            List<LexerToken> lexerTokens = WorkerLexer.lex(new StringReader(str));
            Specification spec = WorkerParser.specification(new LexerTokenReader(lexerTokens));
            
            return spec;
        }
    }
    
    public static Specification loadSpec(String str) throws ClassNotFoundException, IOException {
        List<LexerToken> lexerTokens = WorkerLexer.lex(new StringReader(str));
        Specification spec = WorkerParser.specification(new LexerTokenReader(lexerTokens));
        
        return spec;
    }
    
    public static void pullCapacities(Specification rootSpec, IdentityHashMap<CapacityEnabledSpecification, BigDecimal> capacities) throws Exception {
        if (rootSpec instanceof CapacityEnabledSpecification) {
            CapacityEnabledSpecification capacitySpec = (CapacityEnabledSpecification) rootSpec;
            BigDecimal capacity = capacitySpec.getCapacity();
            capacities.put(capacitySpec, capacity);
        }
        
        for (Method method : rootSpec.getClass().getMethods()) {
            if (!method.getName().endsWith("Specifications")) {
                continue;
            }
            
            method.setAccessible(true);
            List<Specification> childSpecs = (List<Specification>) method.invoke(rootSpec);
            
            for (Specification childSpec : childSpecs) {
                pullCapacities(childSpec, capacities);
            }
        }
    }
}

package com.offbynull.rfm.host.services.h2db;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.containsOnly;
import org.apache.commons.lang3.Validate;

final class QueryTracker {
    static final char PREFIX_PARAM = 'p';
    static final char PREFIX_ALIAS = 'q';
    static final String TOKEN_BORDER = ":::"; 
    
    private final LinkedList<Integer> depthStack = new LinkedList<>();
    private int depthCounter = 0;
    
    private final Map<String, Object> paramMapping = new HashMap<>();
    private int paramCounter = 0;
    private int aliasCounter = 0;

    public void enter() {
        depthStack.addLast(depthCounter);
        paramCounter = 0;
        aliasCounter = 0;
        depthCounter = 0;
    }
    
    public Map<String, Object> params() {
        return new HashMap<>(paramMapping);
    }
    
    public String param(Object data) {
        return param("", data);
    }
    
    public String param(String hint, Object data) {
        Validate.notNull(hint);
        Validate.isTrue(containsOnly(hint, "0123456789abcdefghijklmnopqrstuvwxyz"));
        
        String name = TOKEN_BORDER
                + PREFIX_PARAM + "_"
                + (hint == null ? "" : hint) + "_"
                + depthStack.stream().map(i -> "" + i).collect(joining("_")) + (depthStack.isEmpty() ? "" : "_") + paramCounter
                + TOKEN_BORDER;
        paramCounter++;
        paramMapping.put(name, data);
        return name;
    }
    
    public String alias() {
        return alias("");
    }
    
    public String alias(String hint) {
        Validate.notNull(hint);
        Validate.isTrue(containsOnly(hint, "0123456789abcdefghijklmnopqrstuvwxyz"));
        
        String name = TOKEN_BORDER
                + PREFIX_ALIAS + "_"
                + (hint == null ? "" : hint) + "_"
                + depthStack.stream().map(i -> "" + i).collect(joining("_")) + (depthStack.isEmpty() ? "" : "_") + aliasCounter
                + TOKEN_BORDER;
        aliasCounter++;
        return name;
    }

    public void exit() {
        Validate.isTrue(!depthStack.isEmpty());
        
        depthCounter = depthStack.removeLast();
        depthCounter++;
    }
    
    public int depth() {
        return depthStack.size();
    }
}

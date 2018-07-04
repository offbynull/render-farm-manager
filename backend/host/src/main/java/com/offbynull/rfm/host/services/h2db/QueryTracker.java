package com.offbynull.rfm.host.services.h2db;

import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.repeat;
import org.apache.commons.lang3.Validate;

final class QueryTracker {
    static final String PARAM_BORDER = ":::"; 
    
    private final LinkedList<Integer> tracker = new LinkedList<>();
    private int idCounter = 0;
    
    private final Map<String, Object> paramMapping = new HashMap<>();
    private int paramCounter = 0;

    public void enter() {
        tracker.addLast(idCounter);
        paramCounter = 0;
        idCounter = 0;
    }

    public String next() {
        Validate.validState(tracker != null);
        String id = "q" + tracker.stream().map(i -> String.valueOf(i)).collect(joining("_")) + "_" + idCounter;
        idCounter++;
        return id;
    }
    
    public Map<String, Object> params() {
        return new HashMap<>(paramMapping);
    }
    
    public String param(Object data) {
        return param(null, data);
    }
    
    public String param(String helperName, Object data) {
        Validate.validState(paramMapping != null);
        String name = PARAM_BORDER
                + "p" + tracker.stream().map(i -> String.valueOf(i)).collect(joining("_")) + "_" + idCounter + "_" + paramCounter
                + (helperName == null ? "" : "_" + helperName)
                + PARAM_BORDER;
        paramCounter++;
        paramMapping.put(name, data);
        return name;
    }

    public void exit() {
        idCounter = tracker.removeLast();
        idCounter++;
    }

    public String indentLines(String str) {
        return indentLines(0, str);
    }

    public String indentLines(int extra, String str) {
        Validate.validState(tracker != null);
        String[] lines = str.split("\r?\n");
        return stream(lines).map(x -> repeat(' ', (tracker.size() + extra) * 2) + x).collect(joining("\n"));
    }
}

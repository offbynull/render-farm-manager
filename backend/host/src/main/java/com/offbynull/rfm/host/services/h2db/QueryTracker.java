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
    
    private LinkedList<Integer> tracker;
    private int idCounter = -1;
    
    private Map<String, Object> paramMapping;
    private int paramCounter = -1;

    public void enter() {
        if (tracker == null) {
            tracker = new LinkedList<>();
            paramMapping = new HashMap<>();
        } else {
            tracker.addLast(idCounter);
        }
        paramCounter = 0;
        idCounter = 0;
    }

    public String next() {
        Validate.validState(tracker != null);
        String id = "q" + tracker.stream().map(i -> String.valueOf(i)).collect(joining("_")) + "_" + idCounter;
        idCounter++;
        return id;
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
        if (tracker.isEmpty()) {
            tracker = null;
            paramMapping = null;
        } else {
            idCounter = tracker.removeLast();
        }
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

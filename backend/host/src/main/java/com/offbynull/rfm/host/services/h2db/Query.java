package com.offbynull.rfm.host.services.h2db;

import static com.offbynull.rfm.host.services.h2db.QueryTracker.PARAM_BORDER;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

final class Query {
    private final String query;
    private final UnmodifiableList<Object> queryParams;

    public Query(String query, Map<String, Object> params) {
        StringBuilder psQueryBuilder = new StringBuilder();
        List<Object> psQueryParams = new ArrayList<>();
        
        int idx = 0;
        int borderLen = PARAM_BORDER.length();
        while (true) {
            int startIdx = query.indexOf(PARAM_BORDER, idx);
            if (startIdx == -1) {
                String section = query.substring(idx);
                psQueryBuilder.append(section).append("?");
                break;
            }
            
            int endIdx = query.indexOf(PARAM_BORDER, startIdx + borderLen);
            if (endIdx == -1) {
                String section = query.substring(startIdx);
                psQueryBuilder.append(section).append("?");
                break;
            }
            
            String section = query.substring(idx, startIdx);
            psQueryBuilder.append(section).append("?");
            
            String paramName = query.substring(startIdx, endIdx);
            Validate.isTrue(params.containsKey(paramName));
            Object paramValue = params.get(paramName);
            psQueryParams.add(paramValue);
            
            idx = endIdx + borderLen;
        }
        
        this.query = psQueryBuilder.toString();
        this.queryParams = (UnmodifiableList<Object>) unmodifiableList(psQueryParams);
    }

    public String getQuery() {
        return query;
    }

    public UnmodifiableList<Object> getQueryParams() {
        return queryParams;
    }
    
}

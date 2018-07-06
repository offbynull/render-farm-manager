package com.offbynull.rfm.host.services.h2db;

import static com.offbynull.rfm.host.services.h2db.QueryTracker.PREFIX_ALIAS;
import static com.offbynull.rfm.host.services.h2db.QueryTracker.PREFIX_PARAM;
import java.util.Map;
import static org.apache.commons.lang3.StringUtils.containsOnly;
import org.apache.commons.lang3.Validate;
import static com.offbynull.rfm.host.services.h2db.QueryTracker.TOKEN_BORDER;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.joining;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.repeat;

final class Query {
    private final String query;
    private final Map<String, Object> params;

    public Query(String query, Map<String, Object> params) {
        Validate.notNull(query);
        Validate.notNull(params);
        Validate.noNullElements(params.keySet());
        // paramValues can be null
        
        this.query = query;
        this.params = new HashMap<>(params);
        
        int idx = 0;
        while (true) {
            Token token = nextToken(query, idx);
            if (token == null) {
                break;
            }
            
            if (token.type == TokenType.PARAM) {
                String tokenRaw = token.raw();
                Validate.isTrue(params.containsKey(tokenRaw));
            }
            
            idx = token.endIdx;
        }
    }
    
    private static Token nextToken(String query, int idx) {
        int startIdx = query.indexOf(TOKEN_BORDER, idx);
        if (startIdx == -1) {
            return null;
        }

        int stopIdx = query.indexOf(TOKEN_BORDER, startIdx + TOKEN_BORDER.length());
        if (stopIdx == -1) {
            return null;
        }
        stopIdx += TOKEN_BORDER.length();
        
        String raw = query.substring(startIdx, stopIdx);
        return new Token(raw, startIdx, stopIdx);
    }
    
    public String compose(QueryTracker qt) {
        return compose(0, qt);
    }
    
    public String compose(int indent, QueryTracker qt) {
        Map<String, String> tokenMapping = new HashMap<>();
        
        String output;
        qt.enter();
        try {
            StringBuilder queryBuilder = new StringBuilder();

            int idx = 0;
            while (true) {
                Token token = nextToken(query, idx);
                if (token == null) {
                    String section = query.substring(idx);
                    queryBuilder.append(section);
                    break;
                }

                String beforeToken = query.substring(idx, token.beginIdx);
                String oldRaw = token.raw();
                String newRaw = tokenMapping.get(oldRaw);
                if (newRaw == null) {
                    switch (token.type) {
                        case PARAM: {
                            Object paramValue = params.get(oldRaw);
                            newRaw = qt.param(token.hint, paramValue);
                            break;
                        }
                        case ALIAS: {
                            newRaw = qt.alias(token.hint);
                            break;
                        }
                        default:
                            throw new IllegalStateException();
                    }
                    tokenMapping.put(oldRaw, newRaw);
                }
                queryBuilder.append(beforeToken).append(newRaw);

                idx = token.endIdx;
            }

            output = queryBuilder.toString();
        } finally {
            qt.exit();
        }
        
        String[] lines = output.split("\r?\n");
        return stream(lines).map(x -> repeat(' ', indent) + x).collect(joining("\n"));
    }

    public String toJdbcQuery() {
        Map<String, String> aliasMapping = new HashMap<>();
        
        StringBuilder psQueryBuilder = new StringBuilder();
        
        int aliasCounter = 0;
        int idx = 0;
        while (true) {
            Token token = nextToken(query, idx);
            if (token == null) {
                String remainingRegion = query.substring(idx);
                psQueryBuilder.append(remainingRegion);
                break;
            }
            
            String beforeParamRegion = query.substring(idx, token.beginIdx);
            psQueryBuilder.append(beforeParamRegion);
            switch (token.type) {
                case PARAM: {
                    psQueryBuilder.append("?");
                    break;
                }
                case ALIAS: {
                    String raw = token.raw();
                    String alias = aliasMapping.get(raw);
                    if (alias == null) {
                        alias = "q" + aliasCounter;
                        aliasMapping.put(raw, alias);
                        aliasCounter++;
                    }
                    psQueryBuilder.append(alias);
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
            
            idx = token.endIdx;
        }
        
        return psQueryBuilder.toString();
    }

    public List<Object> toJdbcParameters() {
        List<Object> psQueryParams = new LinkedList<>();
        
        int idx = 0;
        while (true) {
            Token token = nextToken(query, idx);
            if (token == null) {
                break;
            }
            
            if (token.type == TokenType.PARAM) {
                String tokenRaw = token.raw();
                Object paramValue = params.get(tokenRaw); // construct makes sure this key exists in map
                psQueryParams.add(paramValue);
            }
            
            idx = token.endIdx;
        }
        
        return psQueryParams;
    }
    
    private static final class Token {
        private final TokenType type;
        private final String id;
        private final String hint;
        private final int beginIdx;
        private final int endIdx;

        public Token(String raw, int beginIdx, int endIdx) {
            Validate.notNull(raw);
            Validate.isTrue(beginIdx >= 0);
            Validate.isTrue(endIdx >= 0);
            Validate.notEmpty(raw);
            Validate.isTrue(raw.startsWith(TOKEN_BORDER));
            Validate.isTrue(raw.endsWith(TOKEN_BORDER));
            
            raw = StringUtils.removeStart(raw, TOKEN_BORDER);
            raw = StringUtils.removeEnd(raw, TOKEN_BORDER);
            
            switch (raw.charAt(0)) {
                case PREFIX_PARAM:
                    type = TokenType.PARAM;
                    break;
                case PREFIX_ALIAS:
                    type = TokenType.ALIAS;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            int underscore1Idx = raw.indexOf('_');
            Validate.isTrue(underscore1Idx != -1);
            int underscore2Idx = raw.indexOf('_', underscore1Idx + 1);
            Validate.isTrue(underscore2Idx != -1);
            
            hint = raw.substring(underscore1Idx + 1, underscore2Idx);
            id = raw.substring(underscore2Idx + 1);
            
            Validate.isTrue(containsOnly(id, "0123456789_"));
            Validate.isTrue(containsOnly(hint, "0123456789abcdefghijklmnopqrstuvwxyz"));
            
            this.beginIdx = beginIdx;
            this.endIdx = endIdx;
        }
        
        public String raw() {
            StringBuilder sb = new StringBuilder();
            
            sb.append(TOKEN_BORDER);
            switch (type) {
                case PARAM:
                    sb.append(PREFIX_PARAM);
                    break;
                case ALIAS:
                    sb.append(PREFIX_ALIAS);
                    break;
                default:
                    throw new IllegalStateException();
            }            
            sb.append('_').append(hint).append('_').append(id).append(TOKEN_BORDER);

            return sb.toString();
        }
    }

    private enum TokenType {
        PARAM,
        ALIAS
    }
}

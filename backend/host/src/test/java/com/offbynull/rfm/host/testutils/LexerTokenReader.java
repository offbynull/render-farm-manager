package com.offbynull.rfm.host.testutils;

import com.offbynull.rfm.host.testutils.WorkerLexer.LexerToken;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.toList;

public final class LexerTokenReader {
    private final List<LexerToken> lexerTokens;
    private final LinkedList<Integer> markIdxes = new LinkedList<>();
    private int idx = 0;

    public LexerTokenReader(List<LexerToken> lexerTokens) {
        this.lexerTokens = new ArrayList<>(lexerTokens);
    }
    
    public void resetMark() {
        idx = markIdxes.removeLast();
    }
    
    public void discardMark() {
        markIdxes.removeLast();
    }
    
    public void mark() {
        markIdxes.addLast(idx);
    }
    
    public LexerToken read() {
        if (lexerTokens.isEmpty()) {
            return null;
        }
        
        LexerToken ret = lexerTokens.get(idx);
        idx++;
        return ret;
    }

    public boolean matches(Class<? extends LexerToken>... tokens) {
        if (lexerTokens.size() - idx < tokens.length) {
            return false;
        }
        
        return lexerTokens.subList(idx, idx + tokens.length).stream().map(t -> t.getClass()).collect(toList()).equals(asList(tokens));
    }
}

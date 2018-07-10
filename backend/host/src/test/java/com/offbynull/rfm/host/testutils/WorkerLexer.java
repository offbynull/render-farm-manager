package com.offbynull.rfm.host.testutils;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class WorkerLexer {
    private static final Predicate<Integer> OPEN_BRACKET_CHAR_PREDICATE = ch -> (ch == '{');
    private static final Predicate<Integer> CLOSE_BRACKET_CHAR_PREDICATE = ch -> (ch == '}');
    private static final Predicate<Integer> COLON_CHAR_PREDICATE = ch -> (ch == ':');
    private static final Predicate<Integer> NUM_CHAR_PREDICATE = ch -> (ch >= '0' && ch <= '9');
    private static final Predicate<Integer> ID_CHAR_PREDICATE = ch -> ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '/');
    private static final Predicate<Integer> WHITESPACE_CHAR_PREDICATE = ch -> (ch == ' ' || ch == '\r' || ch == '\n');
    
    private WorkerLexer() {
        // do nothing
    }
    
    public static List<LexerToken> lex(Reader r) throws IOException {
        List<LexerToken> ret = new ArrayList<>();
        
        while (true) {
            r.mark(1);
            
            int ch = r.read();
            if (ch == -1) {
                break;
            }
            
            LexerToken token;
            if (OPEN_BRACKET_CHAR_PREDICATE.test(ch)) {
                r.reset();
                token = new OpenBracketLexerToken(r);
            } else if (CLOSE_BRACKET_CHAR_PREDICATE.test(ch)) {
                r.reset();
                token = new CloseBracketLexerToken(r);
            } else if (COLON_CHAR_PREDICATE.test(ch)) {
                r.reset();
                token = new ColonLexerToken(r);
            } else if (NUM_CHAR_PREDICATE.test(ch)) {
                r.reset();
                token = new NumberLexerToken(r);
            } else if (ID_CHAR_PREDICATE.test(ch)) {
                r.reset();
                token = new IdLexerToken(r);
            } else if (WHITESPACE_CHAR_PREDICATE.test(ch)) {
                continue;
            } else {
                throw new IllegalArgumentException("Unrecognized char " + (char) ch);
            }
            
            ret.add(token);
        }
        
        return ret;
    }
    
    public interface LexerToken {
        String getText();
    }
    
    public static abstract class SingleCharLexerToken implements LexerToken {
        private final String text;
        
        public SingleCharLexerToken(Reader r, Predicate<Integer> charPredicate) throws IOException {
            r.mark(1);
            int ch = r.read();
            if (!charPredicate.test(ch)) {
                throw new IllegalStateException("" + ch);
            }
            text = "" + (char) ch;
        }
        
        @Override
        public final String getText() {
            return text;
        }
    }
    
    public static abstract class MultiCharLexerToken implements LexerToken {
        private final String text;
        
        public MultiCharLexerToken(Reader r, Predicate<Integer> charPredicate) throws IOException {
            StringBuilder textBuilder = new StringBuilder();
            r.mark(1);
            int ch;
            while ((ch = r.read()) != -1) {
                if (!charPredicate.test(ch)) {
                    r.reset();
                    break;
                }
                textBuilder.append((char) ch);
                r.mark(1);
            }
            text = textBuilder.toString();
        }
        
        @Override
        public final String getText() {
            return text;
        }
    }
    
    public static final class OpenBracketLexerToken extends SingleCharLexerToken {
        public OpenBracketLexerToken(Reader r) throws IOException {
            super(r, OPEN_BRACKET_CHAR_PREDICATE);
        }
    }
    
    public static final class CloseBracketLexerToken extends SingleCharLexerToken {
        public CloseBracketLexerToken(Reader r) throws IOException {
            super(r, CLOSE_BRACKET_CHAR_PREDICATE);
        }
    }
    
    public static final class ColonLexerToken extends SingleCharLexerToken {
        public ColonLexerToken(Reader r) throws IOException {
            super(r, COLON_CHAR_PREDICATE);
        }
    }
    
    public static final class IdLexerToken extends MultiCharLexerToken {
        public IdLexerToken(Reader r) throws IOException {
            super(r, ID_CHAR_PREDICATE);
        }
    }
    
    public static final class NumberLexerToken extends MultiCharLexerToken {
        public NumberLexerToken(Reader r) throws IOException {
            super(r, NUM_CHAR_PREDICATE);
        }
    }

}

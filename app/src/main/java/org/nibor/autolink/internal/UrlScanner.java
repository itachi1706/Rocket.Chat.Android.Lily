package org.nibor.autolink.internal;

import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

/**
 * Scan for URLs starting from the trigger character ":", requires "://".
 * <p>
 * Based on RFC 3986.
 */
public class UrlScanner implements Scanner {

    @Override
    public LinkSpan scan(CharSequence input, int triggerIndex, int rewindIndex) {
        int length = input.length();
        int afterSlashSlash = triggerIndex + 3;
        if (afterSlashSlash >= length || input.charAt(triggerIndex + 1) != '/' || input.charAt(triggerIndex + 2) != '/') {
            return null;
        }

        int first = findFirst(input, triggerIndex - 1, rewindIndex);
        if (first == -1) {
            return null;
        }

        int last = findLast(input, afterSlashSlash);

        return new LinkSpanImpl(LinkType.URL, first, last + 1);
    }

    // See "scheme" in RFC 3986
    private int findFirst(CharSequence input, int beginIndex, int rewindIndex) {
        int first = -1;
        for (int i = beginIndex; i >= rewindIndex; i--) {
            char c = input.charAt(i);
            if (Scanners.isAlpha(c)) {
                first = i;
            } else if (!schemeNonAlpha(c)) {
                break;
            }
        }
        return first;
    }

    private int findLast(CharSequence input, int beginIndex) {
        int round = 0;
        int square = 0;
        int curly = 0;
        int angle = 0;
        boolean doubleQuote = false;
        boolean singleQuote = false;
        int last = beginIndex;
        loop:
        for (int i = beginIndex; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Scanners.isNonAscii(c)){
                break;
            }
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\f':
                case '\r':
                    break loop;
                case '?':
                case '!':
                case '.':
                case ',':
                case ':':
                    continue loop;
                case '(':
                    round++;
                    break;
                case ')':
                    round--;
                    break;
                case '[':
                    square++;
                    break;
                case ']':
                    square--;
                    break;
                case '{':
                    curly++;
                    break;
                case '}':
                    curly--;
                    break;
                case '<':
                    angle++;
                    break;
                case '>':
                    angle--;
                    break;
                case '"':
                    doubleQuote = !doubleQuote;
                    break;
                case '\'':
                    singleQuote = !singleQuote;
                    break;
                default:
                    last = i;
                    continue loop;
            }
            if (round >= 0 && square >= 0 && curly >= 0 && angle >=0 && !doubleQuote && !singleQuote) {
                last = i;
            }
        }
        return last;
    }

    private boolean schemeNonAlpha(char c) {
        switch (c) {
            case '+':
            case '-':
            case '.':
                return true;
        }
        return Scanners.isDigit(c);
    }

}

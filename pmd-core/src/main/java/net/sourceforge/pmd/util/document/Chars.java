/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.document;


import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.internal.util.IteratorUtil.AbstractIterator;

/**
 * View on a string which doesn't copy the array for subsequence operations.
 * This view is immutable. Since it uses a string internally it benefits from
 * Java 9's compacting feature, it can also be efficiently created from
 * a StringBuilder. When confronted with an instance of this interface, please
 * don't create substrings unnecessarily. Both {@link #subSequence(int, int)}
 * and {@link #slice(int, int)} can cut out a subsequence without copying
 * the underlying byte array. The {@link Pattern} API also works perfectly
 * on arbitrary {@link CharSequence}s, not just on strings. Lastly some
 * methods here provided provide mediated access to the underlying string,
 * which for many use cases is much more optimal than using this CharSequence
 * directly, eg {@link #appendChars(StringBuilder)}, {@link #writeFully(Writer)}.
 *
 * @see Chars#wrap(CharSequence) Chars::wrap, the factory method
 */
public final class Chars implements CharSequence {

    public static final Chars EMPTY = wrap("");

    private final String str;
    private final int start;
    private final int len;

    private Chars(String str, int start, int len) {
        validateRangeWithAssert(start, len, str.length());
        this.str = str;
        this.start = start;
        this.len = len;
    }

    private int idx(int off) {
        return this.start + off;
    }


    /**
     * Wraps the given char sequence into a {@link Chars}. This may
     * call {@link CharSequence#toString()}. If the sequence is already
     * a {@link Chars}, returns it. This is the main factory method for
     * this class. You can eg pass a StringBuilder if you want.
     */
    public static Chars wrap(CharSequence chars) {
        if (chars instanceof Chars) {
            return (Chars) chars;
        }
        return new Chars(chars.toString(), 0, chars.length());
    }

    /**
     * Write all characters of this buffer into the given writer.
     *
     * @param writer A writer
     *
     * @throws NullPointerException If the writer is null
     */
    public void writeFully(@NonNull Writer writer) throws IOException {
        writer.write(str, start, length());
    }

    /**
     * Write a range of characters to the given writer.
     *
     * @param writer A writer
     * @param start  Start offset in this CharSequence
     * @param count  Number of characters
     *
     * @throws IOException               If the writer throws
     * @throws IndexOutOfBoundsException See {@link Writer#write(int)}
     */
    public void write(@NonNull Writer writer, int start, int count) throws IOException {
        writer.write(str, idx(start), count);
    }

    /**
     * Copies 'len' characters from index 'from' into the given array,
     * starting at 'off'.
     *
     * @param srcBegin Start offset in this CharSequence
     * @param cbuf     Character array
     * @param count    Number of characters to copy
     * @param dstBegin Start index in the array
     *
     * @throws NullPointerException      If the array is null (may)
     * @throws IndexOutOfBoundsException See {@link String#getChars(int, int, char[], int)}
     */
    public void getChars(int srcBegin, char @NonNull [] cbuf, int dstBegin, int count) {
        if (count == 0) {
            return;
        }
        int start = idx(srcBegin);
        str.getChars(start, start + count, cbuf, dstBegin);
    }

    /**
     * Appends the character range identified by offset and length into
     * the string builder. This is much more efficient than calling
     * {@link StringBuilder#append(CharSequence)} with this as the
     * parameter, especially on Java 9+.
     *
     * <p>Be aware that {@link StringBuilder#append(CharSequence, int, int)}
     * takes a start and <i>end</i> offset, whereas this method (like all
     * the others in this class) take a start offset and a length.
     *
     * @param off Start (inclusive)
     * @param len Number of characters
     *
     * @throws IndexOutOfBoundsException See {@link StringBuilder#append(CharSequence, int, int)}
     */
    public void appendChars(StringBuilder sb, int off, int len) {
        if (len == 0) {
            return;
        }
        int idx = idx(off);
        sb.append(str, idx, idx + len);
    }

    /**
     * Append this character sequence on the given stringbuilder.
     * This is much more efficient than calling {@link StringBuilder#append(CharSequence)}
     * with this as the parameter, especially on Java 9+.
     *
     * @param sb String builder
     */
    public void appendChars(StringBuilder sb) {
        sb.append(str, start, start + len);
    }


    /**
     * Returns the characters of this charsequence encoded with the
     * given charset.
     */
    public ByteBuffer getBytes(Charset charset) {
        return charset.encode(CharBuffer.wrap(str, start, start + len));
    }

    /**
     * See {@link String#indexOf(String, int)}.
     */
    public int indexOf(String s, int fromIndex) {
        int res = str.indexOf(s, idx(fromIndex)) - start;
        return res >= len ? -1 : res;
    }

    /**
     * See {@link String#indexOf(int, int)}.
     */
    public int indexOf(int ch, int fromIndex) {
        int res = str.indexOf(ch, idx(fromIndex)) - start;
        return res >= len ? -1 : res;
    }

    /**
     * See {@link String#startsWith(String, int)}.
     */
    public boolean startsWith(String prefix, int fromIndex) {
        if (fromIndex < 0 || fromIndex >= len || prefix.length() > len) {
            return false;
        }
        return str.startsWith(prefix, idx(fromIndex));
    }

    /**
     * See {@link String#startsWith(String)}.
     */
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * Returns a subsequence which does not start with control characters ({@code <= 32}).
     * This is consistent with {@link String#trim()}.
     */
    public Chars trimStart() {
        int i = start;
        int maxIdx = start + len;
        while (i < maxIdx && str.charAt(i) <= 32) {
            i++;
        }
        i -= start;
        return slice(i, len - i);
    }

    /**
     * Returns a subsequence which does not end with control characters ({@code <= 32}).
     * This is consistent with {@link String#trim()}.
     */
    public Chars trimEnd() {
        int i = start + len;
        while (i > start && str.charAt(i - 1) <= 32) {
            i--;
        }
        return slice(0, i - start);
    }

    /**
     * Like {@link String#trim()}.
     */
    public Chars trim() {
        return trimStart().trimEnd();
    }


    /**
     * Returns true if this char sequence is logically equal to the
     * parameter. This means they're equal character-by-character. This
     * is more general than {@link #equals(Object)}, which will only answer
     * true if the parameter is a {@link Chars}.
     *
     * @param cs         Another char sequence
     * @param ignoreCase Whether to ignore case
     *
     * @return True if both sequences are logically equal
     */
    public boolean contentEquals(CharSequence cs, boolean ignoreCase) {
        if (cs instanceof Chars) {
            Chars chars2 = (Chars) cs;
            if (len != chars2.len) {
                return false;
            }
            return str.regionMatches(ignoreCase, start, chars2.str, chars2.start, len);
        } else {
            if (length() != cs.length()) {
                return false;
            }
            return str.regionMatches(ignoreCase, start, cs.toString(), 0, len);
        }
    }

    /**
     * Like {@link #contentEquals(CharSequence, boolean)}, considering
     * case distinctions.
     *
     * @param cs A char sequence
     *
     * @return True if both sequences are logically equal, considering case
     */
    public boolean contentEquals(CharSequence cs) {
        return contentEquals(cs, false);
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= len) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return str.charAt(idx(index));
    }

    @Override
    public Chars subSequence(int start, int end) {
        return slice(start, end - start);
    }

    /**
     * Slice a region of text.
     *
     * @param region A region
     *
     * @return A Chars instance
     *
     * @throws IndexOutOfBoundsException If the region is not a valid range
     */
    public Chars slice(TextRegion region) {
        return slice(region.getStartOffset(), region.getLength());
    }

    /**
     * Like {@link #subSequence(int, int)} but with offset + length instead
     * of start + end.
     *
     * @param off Start of the slice ({@code 0 <= off < this.length()})
     * @param len Length of the slice ({@code 0 <= len <= this.length() - off})
     *
     * @return A Chars instance
     *
     * @throws IndexOutOfBoundsException If the parameters are not a valid range
     */
    public Chars slice(int off, int len) {
        validateRange(off, len, this.len);
        if (len == 0) {
            return EMPTY;
        } else if (off == 0 && len == this.len) {
            return this;
        }
        return new Chars(str, idx(off), len);
    }

    /**
     * Returns the substring starting at the given offset and with the
     * given length. This differs from {@link String#substring(int, int)}
     * in that it uses offset + length instead of start + end.
     *
     * @param off Start offset ({@code 0 <= off < this.length()})
     * @param len Length of the substring ({@code 0 <= len <= this.length() - off})
     *
     * @return A substring
     *
     * @throws IndexOutOfBoundsException If the parameters are not a valid range
     */
    public String substring(int off, int len) {
        validateRange(off, len, this.len);
        int start = idx(off);
        return str.substring(start, start + len);
    }

    private static void validateRangeWithAssert(int off, int len, int bound) {
        assert len >= 0 && off >= 0 && (off + len) <= bound : invalidRange(off, len, bound);
    }

    private static void validateRange(int off, int len, int bound) {
        if (len < 0 || off < 0 || (off + len) > bound) {
            throw new IndexOutOfBoundsException(invalidRange(off, len, bound));
        }
    }

    private static String invalidRange(int off, int len, int bound) {
        return "Invalid range [" + off + ", " + (off + len) + "[ (length " + len + ") in string of length " + bound;
    }

    @Override
    public String toString() {
        // this already avoids the copy if start == 0 && len == str.length()
        return str.substring(start, start + len);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Chars)) {
            return false;
        }
        return contentEquals((Chars) o);
    }

    @Override
    public int hashCode() {
        if (isFullString()) {
            return str.hashCode(); // hashcode is cached on strings
        }
        int h = 0;
        for (int i = start, end = start + len; i < end; i++) {
            h = h * 31 + str.charAt(i);
        }
        return h;
    }

    private boolean isFullString() {
        return start == 0 && len == str.length();
    }

    /**
     * Returns an iterable over the lines of this char sequence. The lines
     * are yielded without line separators.
     */
    public Iterable<Chars> lines() {
        return () -> new AbstractIterator<Chars>() {
            final int max = len;
            int pos = 0;

            @Override
            protected void computeNext() {
                if (pos >= max) {
                    done();
                    return;
                }
                int nl = indexOf('\n', pos);
                if (nl < 0) {
                    setNext(subSequence(pos, max));
                    pos = max;
                    return;
                } else if (startsWith("\r", nl - 1)) {
                    setNext(subSequence(pos, nl - 1));
                } else {
                    setNext(subSequence(pos, nl));
                }
                pos = nl + 1;
            }
        };
    }

}

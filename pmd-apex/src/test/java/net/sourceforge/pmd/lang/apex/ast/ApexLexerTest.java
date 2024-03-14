/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.lang.apex.internal.AntlrVersionCheckSuppression;

import com.nawforce.apexparser.ApexLexer;
import com.nawforce.apexparser.ApexParser;


/**
 * This is an exploration test for {@link ApexLexer}.
 */
class ApexLexerTest {
    static {
        AntlrVersionCheckSuppression.initApexLexer();
    }

    private static final String CODE = "public class Foo {\n"
            + "   public List<SObject> test1() {\n"
            + "       return Database.query('Select Id from Account LIMIT 100');\n"
            + "   }\n"
            + "}\n";

    @Test
    void testLexer() {
        CharStream in = CharStreams.fromString(CODE);
        ApexLexer lexer = new ApexLexer(in);

        Token token = lexer.nextToken();
        int tokenCount = 0;
        while (token.getType() != Token.EOF) {
            tokenCount++;
            token = lexer.nextToken();
        }
        assertEquals(35, tokenCount);
    }

    @Test
    void testParser() {
        CharStream in = CharStreams.fromString(CODE);
        ApexLexer lexer = new ApexLexer(in);
        ApexParser parser = new com.nawforce.apexparser.ApexParser(new CommonTokenStream(lexer));
        ApexParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
        assertNotNull(compilationUnit);
    }
}

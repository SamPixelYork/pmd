/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql.ast;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.ast.impl.javacc.JavaccTokenDocument;
import net.sourceforge.pmd.lang.ast.impl.javacc.JavaccTokenDocument.TokenDocumentBehavior;
import net.sourceforge.pmd.lang.ast.impl.javacc.JjtreeParserAdapter;
import net.sourceforge.pmd.lang.ast.impl.javacc.CharStream;

public class PLSQLParser extends JjtreeParserAdapter<ASTInput> {

    private static final TokenDocumentBehavior TOKEN_BEHAVIOR = new TokenDocumentBehavior(PLSQLTokenKinds.TOKEN_NAMES);

    @Override
    protected TokenDocumentBehavior tokenBehavior() {
        return TOKEN_BEHAVIOR;
    }

    @Override
    protected ASTInput parseImpl(CharStream cs, ParserTask task) throws ParseException {
        return new PLSQLParserImpl(cs).Input().addTaskInfo(task);
    }

}

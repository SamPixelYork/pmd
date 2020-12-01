/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.design;

import static net.sourceforge.pmd.properties.constraints.NumericConstraints.positive;

import java.math.BigInteger;
import java.util.logging.Logger;

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.internal.PrettyPrintingUtil;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaMetricsRule;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;


/**
 * Simple n-path complexity rule.
 *
 * @author Clément Fournier
 * @author Jason Bennett
 */
public class NPathComplexityRule extends AbstractJavaMetricsRule {

    private static final Logger LOG = Logger.getLogger(NPathComplexityRule.class.getName());

    @Deprecated
    private static final PropertyDescriptor<Double> MINIMUM_DESCRIPTOR
        = PropertyFactory.doubleProperty("minimum").desc("Deprecated! Minimum reporting threshold")
                         .require(positive()).defaultValue(200d).build();


    private static final PropertyDescriptor<Integer> REPORT_LEVEL_DESCRIPTOR
        = PropertyFactory.intProperty("reportLevel").desc("N-Path Complexity reporting threshold")
                         .require(positive()).defaultValue(200).build();


    private int reportLevel = 200;


    public NPathComplexityRule() {
        definePropertyDescriptor(REPORT_LEVEL_DESCRIPTOR);
        definePropertyDescriptor(MINIMUM_DESCRIPTOR);
    }


    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
        reportLevel = getReportLevel();

        super.visit(node, data);
        return data;
    }


    private int getReportLevel() {
        double oldProp = getProperty(MINIMUM_DESCRIPTOR);
        if (oldProp != MINIMUM_DESCRIPTOR.defaultValue()) {
            LOG.warning("Rule NPathComplexity uses deprecated property 'minimum'. Future versions of PMD will remove support for this property. Please use 'reportLevel' instead!");
            return (int) oldProp;
        }

        return getProperty(REPORT_LEVEL_DESCRIPTOR);
    }


    @Override
    public final Object visit(ASTMethodOrConstructorDeclaration node, Object data) {
        if (!JavaMetrics.NPATH.supports(node)) {
            return data;
        }

        BigInteger npath = MetricsUtil.computeMetric(JavaMetrics.NPATH, node);
        if (npath.compareTo(BigInteger.valueOf(reportLevel)) >= 0) {
            addViolation(data, node, new String[] {node instanceof ASTMethodDeclaration ? "method" : "constructor",
                                                   PrettyPrintingUtil.displaySignature(node),
                                                   String.valueOf(npath),
                                                   String.valueOf(reportLevel)});
        }

        return data;
    }
}


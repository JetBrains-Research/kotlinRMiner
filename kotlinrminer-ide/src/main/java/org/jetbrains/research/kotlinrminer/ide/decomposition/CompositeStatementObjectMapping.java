package org.jetbrains.research.kotlinrminer.ide.decomposition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlinrminer.ide.uml.UMLOperation;
import org.jetbrains.research.kotlinrminer.common.util.StringDistance;

public class CompositeStatementObjectMapping extends AbstractCodeMapping implements Comparable<CompositeStatementObjectMapping> {

    private final double compositeChildMatchingScore;

    public CompositeStatementObjectMapping(CompositeStatementObject statement1, CompositeStatementObject statement2,
                                           UMLOperation operation1, UMLOperation operation2, double score) {
        super(statement1, statement2, operation1, operation2);
        this.compositeChildMatchingScore = score;
    }

    @Override
    public int compareTo(@NotNull CompositeStatementObjectMapping o) {

        double distance1;
        double distance2;
        if (this.getFragment1().getString().equals(this.getFragment2().getString())) {
            distance1 = 0;
        } else {
            String s1 = this.getFragment1().getString().toLowerCase();
            String s2 = this.getFragment2().getString().toLowerCase();
            int distance = StringDistance.editDistance(s1, s2);
            distance1 = (double) distance / (double) Math.max(s1.length(), s2.length());
        }

        if (o.getFragment1().getString().equals(o.getFragment2().getString())) {
            distance2 = 0;
        } else {
            String s1 = o.getFragment1().getString().toLowerCase();
            String s2 = o.getFragment2().getString().toLowerCase();
            int distance = StringDistance.editDistance(s1, s2);
            distance2 = (double) distance / (double) Math.max(s1.length(), s2.length());
        }

        if (distance1 != distance2) {
            return Double.compare(distance1, distance2);
        } else {
            if (this.compositeChildMatchingScore != o.compositeChildMatchingScore) {
                return -Double.compare(this.compositeChildMatchingScore, o.compositeChildMatchingScore);
            } else {
                int depthDiff1 = Math.abs(this.getFragment1().getDepth() - this.getFragment2().getDepth());
                int depthDiff2 = Math.abs(o.getFragment1().getDepth() - o.getFragment2().getDepth());

                if (depthDiff1 != depthDiff2) {
                    return Integer.compare(depthDiff1, depthDiff2);
                } else {
                    int indexDiff1 = Math.abs(this.getFragment1().getIndex() - this.getFragment2().getIndex());
                    int indexDiff2 = Math.abs(o.getFragment1().getIndex() - o.getFragment2().getIndex());
                    return Integer.compare(indexDiff1, indexDiff2);
                }
            }
        }
    }

}
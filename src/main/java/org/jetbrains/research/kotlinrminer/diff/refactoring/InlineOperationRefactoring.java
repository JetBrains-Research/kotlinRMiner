package org.jetbrains.research.kotlinrminer.diff.refactoring;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.research.kotlinrminer.api.Refactoring;
import org.jetbrains.research.kotlinrminer.api.RefactoringType;
import org.jetbrains.research.kotlinrminer.decomposition.*;
import org.jetbrains.research.kotlinrminer.decomposition.replacement.Replacement;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.uml.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class InlineOperationRefactoring implements Refactoring {
    private final UMLOperation inlinedOperation;
    private final UMLOperation targetOperationAfterInline;
    private final UMLOperation targetOperationBeforeInline;
    private final List<OperationInvocation> inlinedOperationInvocations;
    private final Set<Replacement> replacements;
    private final Set<AbstractCodeFragment> inlinedCodeFragmentsFromInlinedOperation;
    private final Set<AbstractCodeFragment> inlinedCodeFragmentsInTargetOperation;
    private final UMLOperationBodyMapper bodyMapper;

    public InlineOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation targetOperationBeforeInline,
                                      List<OperationInvocation> operationInvocations) {
        this.bodyMapper = bodyMapper;
        this.inlinedOperation = bodyMapper.getOperation1();
        this.targetOperationAfterInline = bodyMapper.getOperation2();
        this.targetOperationBeforeInline = targetOperationBeforeInline;
        this.inlinedOperationInvocations = operationInvocations;
        this.replacements = bodyMapper.getReplacements();
        this.inlinedCodeFragmentsFromInlinedOperation = new LinkedHashSet<>();
        this.inlinedCodeFragmentsInTargetOperation = new LinkedHashSet<>();
        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            this.inlinedCodeFragmentsFromInlinedOperation.add(mapping.getFragment1());
            this.inlinedCodeFragmentsInTargetOperation.add(mapping.getFragment2());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(inlinedOperation);
        if (getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            sb.append(" inlined to ");
            sb.append(targetOperationAfterInline);
            sb.append(" in class ");
            sb.append(getClassName());
        } else if (getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
            sb.append(" moved from class ");
            sb.append(inlinedOperation.getClassName());
            sb.append(" to class ");
            sb.append(getTargetOperationAfterInline().getClassName());
            sb.append(" & inlined to ");
            sb.append(getTargetOperationAfterInline());
        }
        return sb.toString();
    }

    private String getClassName() {
        return targetOperationAfterInline.getClassName();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        if (!getTargetOperationBeforeInline().getClassName().equals(getInlinedOperation().getClassName()))
            return RefactoringType.MOVE_AND_INLINE_OPERATION;
        return RefactoringType.INLINE_OPERATION;
    }

    public UMLOperationBodyMapper getBodyMapper() {
        return bodyMapper;
    }

    public UMLOperation getInlinedOperation() {
        return inlinedOperation;
    }

    public UMLOperation getTargetOperationAfterInline() {
        return targetOperationAfterInline;
    }

    public UMLOperation getTargetOperationBeforeInline() {
        return targetOperationBeforeInline;
    }

    public List<OperationInvocation> getInlinedOperationInvocations() {
        return inlinedOperationInvocations;
    }

    public Set<Replacement> getReplacements() {
        return replacements;
    }

    public Set<AbstractCodeFragment> getInlinedCodeFragments() {
        return inlinedCodeFragmentsInTargetOperation;
    }

    /**
     * @return the code range of the target method in the <b>parent</b> commit
     */
    public CodeRange getTargetOperationCodeRangeBeforeInline() {
        return targetOperationBeforeInline.codeRange();
    }

    /**
     * @return the code range of the target method in the <b>child</b> commit
     */
    public CodeRange getTargetOperationCodeRangeAfterInline() {
        return targetOperationAfterInline.codeRange();
    }

    /**
     * @return the code range of the inlined method in the <b>parent</b> commit
     */
    public CodeRange getInlinedOperationCodeRange() {
        return inlinedOperation.codeRange();
    }

    /**
     * @return the code range of the inlined code fragment from the inlined method in the <b>parent</b> commit
     */
    public CodeRange getInlinedCodeRangeFromInlinedOperation() {
        return CodeRange.computeRange(inlinedCodeFragmentsFromInlinedOperation);
    }

    /**
     * @return the code range of the inlined code fragment in the target method in the <b>child</b> commit
     */
    public CodeRange getInlinedCodeRangeInTargetOperation() {
        return CodeRange.computeRange(inlinedCodeFragmentsInTargetOperation);
    }

    /**
     * @return the code range(s) of the invocation(s) to the inlined method inside the target method in the <b>parent</b> commit
     */
    public Set<CodeRange> getInlinedOperationInvocationCodeRanges() {
        Set<CodeRange> codeRanges = new LinkedHashSet<>();
        for (OperationInvocation invocation : inlinedOperationInvocations) {
            codeRanges.add(invocation.codeRange());
        }
        return codeRanges;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getInlinedOperation().getLocationInfo().getFilePath(),
                                      getInlinedOperation().getClassName()));
        pairs.add(new ImmutablePair<>(getTargetOperationBeforeInline().getLocationInfo().getFilePath(),
                                      getTargetOperationBeforeInline().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getTargetOperationAfterInline().getLocationInfo().getFilePath(),
                                      getTargetOperationAfterInline().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(getInlinedOperationCodeRange()
                       .setDescription("inlined method declaration")
                       .setCodeElement(inlinedOperation.toString()));
        //ranges.add(getInlinedCodeRangeFromInlinedOperation().setDescription("inlined code from inlined method declaration"));
        for (AbstractCodeFragment inlinedCodeFragment : inlinedCodeFragmentsFromInlinedOperation) {
            ranges.add(inlinedCodeFragment.codeRange().setDescription("inlined code from inlined method declaration"));
        }
        ranges.add(getTargetOperationCodeRangeBeforeInline()
                       .setDescription("target method declaration before inline")
                       .setCodeElement(targetOperationBeforeInline.toString()));
        for (OperationInvocation invocation : inlinedOperationInvocations) {
            ranges.add(invocation.codeRange()
                           .setDescription("inlined method invocation")
                           .setCodeElement(invocation.actualString()));
        }
        for (StatementObject statement : bodyMapper.getNonMappedLeavesT1()) {
            ranges.add(statement.codeRange().
                setDescription("deleted statement in inlined method declaration"));
        }
        for (CompositeStatementObject statement : bodyMapper.getNonMappedInnerNodesT1()) {
            ranges.add(statement.codeRange().
                setDescription("deleted statement in inlined method declaration"));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(getTargetOperationCodeRangeAfterInline()
                       .setDescription("target method declaration after inline")
                       .setCodeElement(targetOperationAfterInline.toString()));
        for (AbstractCodeFragment inlinedCodeFragment : inlinedCodeFragmentsInTargetOperation) {
            ranges.add(inlinedCodeFragment.codeRange().setDescription("inlined code in target method declaration"));
        }
        return ranges;
    }
}

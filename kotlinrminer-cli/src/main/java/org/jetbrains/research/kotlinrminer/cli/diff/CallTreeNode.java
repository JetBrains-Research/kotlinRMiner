package org.jetbrains.research.kotlinrminer.cli.diff;

import org.jetbrains.research.kotlinrminer.cli.decomposition.OperationInvocation;
import org.jetbrains.research.kotlinrminer.cli.uml.UMLOperation;

import java.util.ArrayList;
import java.util.List;

public class CallTreeNode {
    private final UMLOperation originalOperation;
    private final UMLOperation invokedOperation;
    private final OperationInvocation invocation;
    private final List<CallTreeNode> children = new ArrayList<>();

    public CallTreeNode(UMLOperation originalOperation, UMLOperation invokedOperation,
                        OperationInvocation invocation) {
        this.originalOperation = originalOperation;
        this.invokedOperation = invokedOperation;
        this.invocation = invocation;
    }

    public UMLOperation getOriginalOperation() {
        return originalOperation;
    }

    public UMLOperation getInvokedOperation() {
        return invokedOperation;
    }

    public OperationInvocation getInvocation() {
        return invocation;
    }

    public void addChild(CallTreeNode node) {
        children.add(node);
    }

    public List<CallTreeNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((invocation == null) ? 0 : invocation.hashCode());
        result = prime * result + ((invokedOperation == null) ? 0 : invokedOperation.hashCode());
        result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CallTreeNode other = (CallTreeNode) obj;
        if (invocation == null) {
            if (other.invocation != null)
                return false;
        } else if (!invocation.equals(other.invocation))
            return false;
        if (invokedOperation == null) {
            if (other.invokedOperation != null)
                return false;
        } else if (!invokedOperation.equals(other.invokedOperation))
            return false;
        if (originalOperation == null) {
            return other.originalOperation == null;
        } else return originalOperation.equals(other.originalOperation);
    }

    public String toString() {
        return invokedOperation +
            " called from " +
            originalOperation;
    }
}

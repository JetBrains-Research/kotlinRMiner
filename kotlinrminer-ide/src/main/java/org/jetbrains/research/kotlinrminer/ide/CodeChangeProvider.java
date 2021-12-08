package org.jetbrains.research.kotlinrminer.ide;

import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;

import java.util.List;

interface CodeRangeProvider {
    List<CodeRange> leftSide();

    List<CodeRange> rightSide();
}
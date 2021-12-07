package org.jetbrains.research.kotlinrminer.core;

import org.jetbrains.research.kotlinrminer.core.diff.CodeRange;

import java.util.List;

interface CodeRangeProvider {
    List<CodeRange> leftSide();

    List<CodeRange> rightSide();
}
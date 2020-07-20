package org.jetbrains.research.kotlinrminer.api;

import org.jetbrains.research.kotlinrminer.diff.CodeRange;

import java.util.List;

interface CodeRangeProvider {
    List<CodeRange> leftSide();

    List<CodeRange> rightSide();
}
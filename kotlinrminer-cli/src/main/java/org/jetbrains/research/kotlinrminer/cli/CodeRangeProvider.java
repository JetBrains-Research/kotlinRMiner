package org.jetbrains.research.kotlinrminer.cli;

import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;

import java.util.List;

interface CodeRangeProvider {
    List<CodeRange> leftSide();

    List<CodeRange> rightSide();
}
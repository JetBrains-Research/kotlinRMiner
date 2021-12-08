package org.jetbrains.research.kotlinrminer.cli.decomposition;


import org.jetbrains.research.kotlinrminer.cli.diff.CodeRange;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}

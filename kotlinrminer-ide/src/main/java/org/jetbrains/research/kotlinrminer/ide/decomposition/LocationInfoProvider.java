package org.jetbrains.research.kotlinrminer.ide.decomposition;


import org.jetbrains.research.kotlinrminer.ide.diff.CodeRange;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}

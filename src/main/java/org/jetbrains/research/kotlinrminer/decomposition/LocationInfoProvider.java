package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.diff.CodeRange;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}

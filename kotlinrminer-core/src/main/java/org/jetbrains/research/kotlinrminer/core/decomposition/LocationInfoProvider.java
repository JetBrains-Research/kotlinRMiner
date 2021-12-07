package org.jetbrains.research.kotlinrminer.core.decomposition;

import org.jetbrains.research.kotlinrminer.core.diff.CodeRange;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}

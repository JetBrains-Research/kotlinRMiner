package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

public interface LocationInfoProvider {
    LocationInfo getLocationInfo();

    CodeRange codeRange();
}

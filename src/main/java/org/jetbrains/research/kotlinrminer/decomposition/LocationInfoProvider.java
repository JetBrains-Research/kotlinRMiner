package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.research.kotlinrminer.diff.CodeRange;
import org.jetbrains.research.kotlinrminer.LocationInfo;

public interface LocationInfoProvider {
    public LocationInfo getLocationInfo();

    public CodeRange codeRange();
}

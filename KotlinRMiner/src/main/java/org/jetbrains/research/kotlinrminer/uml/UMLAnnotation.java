package org.jetbrains.research.kotlinrminer.uml;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.kotlin.psi.KtAnnotation;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlinrminer.decomposition.CodeElementType;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfo;
import org.jetbrains.research.kotlinrminer.decomposition.AbstractExpression;
import org.jetbrains.research.kotlinrminer.decomposition.LocationInfoProvider;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

public class UMLAnnotation implements Serializable, LocationInfoProvider {
    private final LocationInfo locationInfo;
    private final String typeName;
    private final Map<String, AbstractExpression> memberValuePairs = new LinkedHashMap<>();
    private AbstractExpression value;

    public UMLAnnotation(KtFile cu, String filePath, KtAnnotation annotation) {
        this.typeName = annotation.getName();
        this.locationInfo = new LocationInfo(cu, filePath, annotation, CodeElementType.ANNOTATION);
        List<KtAnnotationEntry> ktAnnotationEntries = annotation.getEntries();
        //TODO: process annotation entries
    }

    public String getTypeName() {
        return typeName;
    }

    public AbstractExpression getValue() {
        return value;
    }

    public Map<String, AbstractExpression> getMemberValuePairs() {
        return memberValuePairs;
    }

    public boolean isMarkerAnnotation() {
        return value == null && memberValuePairs.isEmpty();
    }

    public boolean isSingleMemberAnnotation() {
        return value != null;
    }

    public boolean isNormalAnnotation() {
        return memberValuePairs.size() > 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(typeName);
        if (value != null) {
            sb.append("(");
            sb.append(value.getExpression());
            sb.append(")");
        }
        if (!memberValuePairs.isEmpty()) {
            sb.append("(");
            int i = 0;
            for (String key : memberValuePairs.keySet()) {
                sb.append(key).append(" = ").append(memberValuePairs.get(key).getExpression());
                if (i < memberValuePairs.size() - 1) {
                    sb.append(", ");
                }
                i++;
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    @Override
    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + memberValuePairsHashCode();
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.getExpression().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UMLAnnotation other = (UMLAnnotation) obj;
        if (!this.memberValuePairsEquals(other)) {
            return false;
        }
        if (typeName == null) {
            if (other.typeName != null) {
                return false;
            }
        } else if (!typeName.equals(other.typeName)) {
            return false;
        }
        if (value == null) {
            return other.value == null;
        } else {
            return value.getExpression().equals(other.value.getExpression());
        }
    }

    private boolean memberValuePairsEquals(UMLAnnotation other) {
        Map<String, AbstractExpression> m = other.memberValuePairs;
        int thisSize = this.memberValuePairs.size();
        int otherSize = other.memberValuePairs.size();
        if (thisSize != otherSize) {
            return false;
        }
        for (Map.Entry<String, AbstractExpression> entry : memberValuePairs.entrySet()) {
            String thisKey = entry.getKey();
            AbstractExpression thisValue = entry.getValue();
            if (thisValue == null) {
                if (!(m.get(thisKey) == null && m.containsKey(thisKey))) {
                    return false;
                }
            } else {
                if (!thisValue.getExpression().equals(m.get(thisKey).getExpression())) {
                    return false;
                }
            }
        }
        return true;
    }

    private int memberValuePairsHashCode() {
        int h = 0;
        for (Map.Entry<String, AbstractExpression> entry : memberValuePairs.entrySet()) {
            h += (entry.getKey() == null ? 0 : entry.getKey().hashCode()) ^ (entry.getValue() == null ? 0 :
                entry.getValue().getExpression().hashCode());
        }
        return h;
    }
}

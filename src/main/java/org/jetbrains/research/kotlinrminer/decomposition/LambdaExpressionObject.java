package org.jetbrains.research.kotlinrminer.decomposition;

import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.research.kotlinrminer.LocationInfo;
import org.jetbrains.research.kotlinrminer.LocationInfo.CodeElementType;
import org.jetbrains.research.kotlinrminer.diff.CodeRange;

public class LambdaExpressionObject implements LocationInfoProvider {
    private final LocationInfo locationInfo;
    private final OperationBody body;
    private AbstractExpression expression;

    public LambdaExpressionObject(KtFile cu, String filePath, KtLambdaExpression lambdaExpression) {
        this.locationInfo = new LocationInfo(cu, filePath, lambdaExpression, CodeElementType.LAMBDA_EXPRESSION);
        this.body = new OperationBody(cu, filePath, lambdaExpression.getBodyExpression());
    }

    public OperationBody getBody() {
        return body;
    }

    public AbstractExpression getExpression() {
        return expression;
    }

    @Override
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

}

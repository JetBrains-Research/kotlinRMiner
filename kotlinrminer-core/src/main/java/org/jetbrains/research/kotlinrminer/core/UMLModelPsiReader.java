package org.jetbrains.research.kotlinrminer.core;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.core.decomposition.CodeElementType;
import org.jetbrains.research.kotlinrminer.core.decomposition.LocationInfo;
import org.jetbrains.research.kotlinrminer.core.decomposition.OperationBody;
import org.jetbrains.research.kotlinrminer.core.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.core.uml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

/**
 * Parses and processes the files written in Kotlin.
 */
public class UMLModelPsiReader {
    private final UMLModel umlModel;

    public UMLModelPsiReader(Set<String> repositoryDirectories) {
        this.umlModel = new UMLModel(repositoryDirectories);
    }

    public void parseFiles(@Nullable Project project, Map<String, String> kotlinFileContents) {
        for (String filePath : kotlinFileContents.keySet()) {
            KtFile ktFile = null;
            if (project != null) {
                ktFile = (KtFile) PsiFileFactory.getInstance(project)
                    .createFileFromText(filePath, KotlinLanguage.INSTANCE, kotlinFileContents.get(filePath));
            }
            List<String> importedTypes = processImports(ktFile);
            PsiElement[] elementsInFile = ktFile.getChildren();
            List<KtNamedFunction> packageLevelFunctions = new ArrayList<>();
            for (PsiElement psiElement : elementsInFile) {
                if (psiElement instanceof KtObjectDeclaration) {
                    KtObjectDeclaration objectDeclaration = (KtObjectDeclaration) psiElement;
                    processObject(objectDeclaration,
                                  filePath);
                } else if (psiElement instanceof KtClass) {
                    KtClass ktClass = (KtClass) psiElement;
                    if (ktClass.isEnum()) {
                        processKtEnum(ktClass, ktFile.getPackageFqName().asString(), filePath,
                                      importedTypes);
                    } else {
                        processKtClass(ktClass, ktFile.getPackageFqName().asString(), filePath,
                                       importedTypes);
                    }
                } else if (psiElement instanceof KtNamedFunction) {
                    packageLevelFunctions.add((KtNamedFunction) psiElement);
                }
            }
            if (packageLevelFunctions.size() > 0) {
                processPackageLevelFunctions(ktFile, packageLevelFunctions, filePath);
            }
        }
    }

    private void processPackageLevelFunctions(KtFile ktFile, List<KtNamedFunction> packageLevelFunctions,
                                              String filePath) {
        UMLFile umlFile = new UMLFile(filePath);
        LocationInfo locationInfo = generateLocationInfo(ktFile, ktFile.getVirtualFilePath(), ktFile,
                                                         CodeElementType.TYPE_DECLARATION);
        umlFile.setLocationInfo(locationInfo);
        for (KtNamedFunction function : packageLevelFunctions) {
            UMLOperation umlOperation = processMethodDeclaration(ktFile, function,
                                                                 false,
                                                                 filePath);
            umlOperation.setClassName(ktFile.getName());
            umlFile.addMethod(umlOperation);
        }
        this.getUmlModel().addFile(umlFile);
    }

    public List<String> processImports(KtFile ktFile) {
        List<String> importedTypes = new ArrayList<>();
        KtImportList importList = ktFile.getImportList();
        if (importList != null) {
            for (KtImportDirective importDeclaration : importList.getImports()) {
                FqName fqName = importDeclaration.getImportedFqName();
                String importName = fqName == null ? null : fqName.asString();
                if (importName != null) {
                    importedTypes.add(importName);
                }
            }
        }
        return importedTypes;
    }

    public void processKtEnum(KtClass ktEnum, String packageName, String sourceFile, List<String> importedTypes) {
        UMLJavadoc javadoc = generateDocComment(ktEnum);
        String className = ktEnum.getName();
        LocationInfo locationInfo = generateLocationInfo(ktEnum.getContainingKtFile(), sourceFile, ktEnum,
                                                         CodeElementType.TYPE_DECLARATION);
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, ktEnum.isTopLevel(), importedTypes);
        umlClass.setJavadoc(javadoc);

        umlClass.setEnum(true);
        umlClass.setVisibility(extractVisibilityModifier(ktEnum));
        //TODO: process body declarations

        this.getUmlModel().addClass(umlClass);
    }

    public void processKtClass(KtClass ktClass, String packageName, String sourceFile, List<String> importedTypes) {
        String className = ktClass.getName();
        LocationInfo locationInfo = generateLocationInfo(ktClass.getContainingKtFile(), sourceFile, ktClass,
                                                         CodeElementType.TYPE_DECLARATION);
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, ktClass.isTopLevel(), importedTypes);

        if (ktClass.isInterface()) {
            umlClass.setInterface(true);
        }
        umlClass.setVisibility(extractVisibilityModifier(ktClass));

        if (ktClass.isData()) {
            umlClass.setData(true);
        } else if (ktClass.isSealed()) {
            umlClass.setSealed(true);
        } else if (ktClass.isInner()) {
            umlClass.setInner(true);
        }

        KtModifierList ktClassExtendedModifiers = ktClass.getModifierList();
        if (ktClassExtendedModifiers != null) {
            for (KtAnnotation annotation : ktClassExtendedModifiers.getAnnotations()) {
                umlClass.addAnnotation(new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));
            }
        }

        List<KtTypeParameter> parameters = ktClass.getTypeParameters();

        for (KtTypeParameter parameter : parameters) {
            UMLTypeParameter umlTypeParameter = new UMLTypeParameter(parameter.getName());
            //TODO: umlTypeParameter.addTypeBound(UMLType.extractTypeObject(ktFile, sourceFile, type, 0));
            KtModifierList parameterModifierList = parameter.getModifierList();
            if (parameterModifierList != null) {
                for (KtAnnotation annotation : parameterModifierList.getAnnotations()) {
                    umlTypeParameter.addAnnotation(
                        new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));

                }
            }
            umlClass.addTypeParameter(umlTypeParameter);
        }

        List<KtSuperTypeListEntry> superTypeListEntries = ktClass.getSuperTypeListEntries();
        for (KtSuperTypeListEntry superTypeListEntry : superTypeListEntries) {
            UMLType umlType = UMLType.extractTypeObject(ktClass.getContainingKtFile(), sourceFile,
                                                        superTypeListEntry.getTypeReference(), 0);
            UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass, umlType.getClassType());
            umlClass.setSuperclass(umlType);
            getUmlModel().addGeneralization(umlGeneralization);
        }

        List<KtProperty> ktClassProperties = ktClass.getProperties();
        for (KtProperty ktProperty : ktClassProperties) {
            UMLAttribute attribute =
                processFieldDeclaration(ktClass.getContainingKtFile(), ktProperty, sourceFile);
            attribute.setClassName(umlClass.getQualifiedName());
            umlClass.addAttribute(attribute);
        }

        List<KtDeclaration> declarations = ktClass.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtNamedFunction) {
                KtNamedFunction function = (KtNamedFunction) declaration;
                UMLOperation operation =
                    processMethodDeclaration(ktClass.getContainingKtFile(), function,
                                             umlClass.isInterface(), sourceFile);
                operation.setClassName(umlClass.getQualifiedName());
                umlClass.addOperation(operation);
            }
        }

        List<KtObjectDeclaration> companionObjects = ktClass.getCompanionObjects();
        for (KtObjectDeclaration companionObject : companionObjects) {
            UMLCompanionObject umlCompanionObject = processCompanionObject(companionObject, sourceFile);
            umlCompanionObject.setClassName(umlClass.getQualifiedName());
            umlClass.addCompanionObject(umlCompanionObject);
        }

        this.getUmlModel().addClass(umlClass);
    }

    private UMLAttribute processFieldDeclaration(KtFile ktFile,
                                                 KtProperty fieldDeclaration,
                                                 String sourceFile) {
        UMLJavadoc javadoc = generateDocComment(fieldDeclaration);
        //TODO: figure out how to get dimensions
        UMLType type = UMLType.extractTypeObject(ktFile, sourceFile, fieldDeclaration.getTypeReference(), 0);
        String fieldName = fieldDeclaration.getName();
        LocationInfo locationInfo =
            generateLocationInfo(ktFile, sourceFile, fieldDeclaration, CodeElementType.FIELD_DECLARATION);
        UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo);
        VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFile, fieldDeclaration);
        variableDeclaration.setAttribute(true);
        umlAttribute.setVariableDeclaration(variableDeclaration);
        umlAttribute.setJavadoc(javadoc);
        umlAttribute.setVisibility(extractVisibilityModifier(fieldDeclaration));
        return umlAttribute;
    }

    private UMLJavadoc generateDocComment(KtNamedDeclaration bodyDeclaration) {
        UMLJavadoc doc = null;
        KDoc javaDoc = bodyDeclaration.getDocComment();
        if (javaDoc != null) {
            doc = new UMLJavadoc();
            KDocSection tag = javaDoc.getDefaultSection();
            UMLTagElement tagElement = new UMLTagElement(tag.getName());
            String fragments = tag.getContent();
            tagElement.addFragment(fragments);
            doc.addTag(tagElement);
        }
        return doc;
    }

    private UMLOperation processMethodDeclaration(KtFile ktFile,
                                                  KtNamedFunction methodDeclaration,
                                                  boolean isInterfaceMethod,
                                                  String filePath) {
        UMLJavadoc javadoc = generateDocComment(methodDeclaration);
        String methodName = methodDeclaration.getName();
        LocationInfo locationInfo = generateLocationInfo(ktFile.getContainingKtFile(), filePath, methodDeclaration,
                                                         CodeElementType.METHOD_DECLARATION);
        UMLOperation umlOperation = new UMLOperation(methodName, locationInfo);
        umlOperation.setJavadoc(javadoc);

        umlOperation.setConstructor(false);

        if (methodDeclaration.hasDeclaredReturnType()) {
            KtTypeReference returnTypeReference = methodDeclaration.getTypeReference();
            if (returnTypeReference != null) {
                //TODO: get extra dimensions
                //TODO: get fully qualified name
                UMLType type =
                    UMLType.extractTypeObject(ktFile.getContainingKtFile(), filePath, returnTypeReference, 0);
                UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
                umlOperation.addParameter(returnParameter);
            }
        }

        KtModifierList methodModifiers = methodDeclaration.getModifierList();
        if (isInterfaceMethod) {
            umlOperation.setVisibility("public");
        } else {
            umlOperation.setVisibility(extractVisibilityModifier(methodDeclaration));
        }
        if (methodModifiers != null) {
            List<KtAnnotation> ktAnnotations = methodModifiers.getAnnotations();
            for (KtAnnotation annotation : ktAnnotations) {
                umlOperation.addAnnotation(new UMLAnnotation(ktFile.getContainingKtFile(), filePath, annotation));
            }
        }

        List<KtTypeParameter> typeParameters = methodDeclaration.getTypeParameters();
        for (KtTypeParameter typeParameter : typeParameters) {
            UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getName());
            KtTypeReference typeBounds = typeParameter.getExtendsBound();
            if (typeBounds != null) {
                umlTypeParameter.addTypeBound(
                    UMLType.extractTypeObject(ktFile.getContainingKtFile(), filePath, typeBounds, 0));
            }

            KtModifierList typeParameterExtendedModifiers = typeParameter.getModifierList();
            if (typeParameterExtendedModifiers != null) {
                for (KtAnnotation annotation : typeParameterExtendedModifiers.getAnnotations()) {
                    umlTypeParameter.addAnnotation(
                        new UMLAnnotation(ktFile.getContainingKtFile(), filePath, annotation));
                }
            }
            umlOperation.addTypeParameter(umlTypeParameter);
        }

        KtBlockExpression methodBody = methodDeclaration.getBodyBlockExpression();
        if (methodBody != null) {
            OperationBody body = new OperationBody(ktFile.getContainingKtFile(), filePath, methodBody);
            umlOperation.setBody(body);
            if (methodBody.getStatements().size() == 0) {
                umlOperation.setEmptyBody(true);
            }
        } else {
            umlOperation.setBody(null);
        }

        List<KtParameter> parameters = methodDeclaration.getValueParameters();
        for (KtParameter parameter : parameters) {
            KtTypeReference typeReference = parameter.getTypeReference();
            String paramName = parameter.getName();

            UMLType type = UMLType.extractTypeObject(parameter.getContainingKtFile(), filePath, typeReference, 0);
            UMLParameter umlParameter = new UMLParameter(paramName, type, "in", parameter.isVarArg());
            VariableDeclaration variableDeclaration =
                new VariableDeclaration(parameter.getContainingKtFile(), filePath, parameter, parameter.isVarArg());

            variableDeclaration.setParameter(true);
            umlParameter.setVariableDeclaration(variableDeclaration);
            umlOperation.addParameter(umlParameter);
        }

        return umlOperation;
    }

    public UMLCompanionObject processCompanionObject(KtObjectDeclaration object, String sourceFile) {
        UMLCompanionObject umlCompanionObject = new UMLCompanionObject();
        umlCompanionObject.setName(object.getName());
        LocationInfo objectLocationInfo = generateLocationInfo(object.getContainingKtFile(), sourceFile, object,
                                                               CodeElementType.COMPANION_OBJECT);
        umlCompanionObject.setLocationInfo(objectLocationInfo);
        List<KtDeclaration> declarations = object.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            LocationInfo locationInfo = generateLocationInfo(declaration.getContainingKtFile(), sourceFile, declaration,
                                                             CodeElementType.METHOD_DECLARATION);
            UMLOperation method = new UMLOperation(declaration.getName(), locationInfo);
            umlCompanionObject.addMethod(method);
        }
        return umlCompanionObject;
    }

    public void processObject(KtObjectDeclaration objectDeclaration, String filePath) {
        String objectName = objectDeclaration.getName();
        LocationInfo locationInfo =
            generateLocationInfo(objectDeclaration.getContainingKtFile(), filePath, objectDeclaration,
                                 CodeElementType.TYPE_DECLARATION);
        UMLClass object =
            new UMLClass(objectDeclaration.getContainingKtFile().getPackageFqName().asString(), objectName,
                         locationInfo,
                         objectDeclaration.isTopLevel(), processImports(objectDeclaration.getContainingKtFile()));
        object.setObject(true);
        object.setVisibility(extractVisibilityModifier(objectDeclaration));

        KtClassBody body = objectDeclaration.getBody();
        if (body != null) {
            List<KtNamedFunction> functions = body.getFunctions();
            for (KtNamedFunction function : functions) {
                UMLOperation operation =
                    processMethodDeclaration(objectDeclaration.getContainingKtFile(), function,
                                             object.isInterface(), filePath);
                operation.setClassName(object.getQualifiedName());
                object.addOperation(operation);
            }
            List<KtProperty> properties = body.getProperties();
            for (KtProperty property : properties) {
                UMLAttribute umlAttribute =
                    processFieldDeclaration(property.getContainingKtFile(), property, filePath);
                umlAttribute.setClassName(object.getQualifiedName());
                object.addAttribute(umlAttribute);
            }
        }
        this.getUmlModel().addClass(object);
    }

    private String extractVisibilityModifier(KtNamedDeclaration ktNamedDeclaration) {
        KtModifierList modifiers = ktNamedDeclaration.getModifierList();
        String visibility;
        if (modifiers != null) {
            if (modifiers.hasModifier(PUBLIC_KEYWORD)) {
                visibility = "public";
            } else if (modifiers.hasModifier(PROTECTED_KEYWORD)) {
                visibility = "protected";
            } else if (modifiers.hasModifier(PRIVATE_KEYWORD)) {
                visibility = "private";
            } else if (modifiers.hasModifier(INTERNAL_KEYWORD)) {
                visibility = "internal";
            } else {
                visibility = "public";
            }
        } else {
            visibility = "public";
        }
        return visibility;
    }

    public UMLModel getUmlModel() {
        return this.umlModel;
    }

    private LocationInfo generateLocationInfo(KtFile ktFile,
                                              String sourceFile,
                                              KtElement node,
                                              CodeElementType codeElementType) {
        return new LocationInfo(ktFile, sourceFile, node, codeElementType);
    }

}

package org.jetbrains.research.kotlinrminer;

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.research.kotlinrminer.decomposition.OperationBody;
import org.jetbrains.research.kotlinrminer.decomposition.VariableDeclaration;
import org.jetbrains.research.kotlinrminer.uml.*;
import org.jetbrains.research.kotlinrminer.util.KotlinLightVirtualFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.research.kotlinrminer.util.EnvironmentManager.createKotlinCoreEnvironment;

/**
 * Parses and processes the files written in Kotlin
 */
public class UMLModelPsiReader {
    private final UMLModel umlModel;

    public UMLModelPsiReader(Map<String, String> kotlinFileContents, Set<String> repositoryDirectories) throws
            IOException {
        this.umlModel = new UMLModel(repositoryDirectories);
        for (String filePath : kotlinFileContents.keySet()) {
            KtFile ktFile = (KtFile) buildPsiFile(filePath, createKotlinCoreEnvironment(new HashSet<>()),
                                                  kotlinFileContents.get(filePath));
            List<String> importedTypes = processImports(ktFile);

            PsiElement[] elementsInFile = ktFile.getChildren();
            for (PsiElement psiElement : elementsInFile) {
                if (psiElement instanceof KtObjectDeclaration) {
                    KtObjectDeclaration objectDeclaration = (KtObjectDeclaration) psiElement;
                    processObject(objectDeclaration,
                                  objectDeclaration.getContainingKtFile().getPackageFqName().asString());
                } else if (psiElement instanceof KtClass) {
                    KtClass ktClass = (KtClass) psiElement;
                    if (ktClass.isEnum()) {
                        processKtEnum(ktClass, ktFile.getPackageFqName().asString(), ktFile.getVirtualFilePath(),
                                      importedTypes);
                    } else {
                        processKtClass(ktClass, ktFile.getPackageFqName().asString(), ktFile.getVirtualFilePath(),
                                       importedTypes);
                    }
                }
            }
        }
    }

    public List<String> processImports(KtFile ktFile) {
        List<String> importedTypes = new ArrayList<>();
        KtImportList importList = ktFile.getImportList();
        if (importList != null) {
            for (KtImportDirective importDeclaration : importList.getImports()) {
                FqName fqName = importDeclaration.getImportedFqName();
                String importName = fqName == null ? null : fqName.asString();
                if (importName != null)
                    importedTypes.add(importName);
            }
        }
        return importedTypes;
    }

    public void processKtEnum(KtClass ktEnum, String packageName, String sourceFile, List<String> importedTypes) {
        UMLJavadoc javadoc = generateDocComment(ktEnum);
        String className = ktEnum.getName();
        LocationInfo locationInfo = generateLocationInfo(ktEnum.getContainingKtFile(), sourceFile, ktEnum,
                                                         LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, ktEnum.isTopLevel(), importedTypes);
        umlClass.setJavadoc(javadoc);

        umlClass.setEnum(true);
        processModifiers(sourceFile, ktEnum, umlClass);

        //TODO: process body declarations

        this.getUmlModel().addClass(umlClass);
    }

    public void processKtClass(KtClass ktClass, String packageName, String sourceFile, List<String> importedTypes) {
        String className = ktClass.getName();
        LocationInfo locationInfo = generateLocationInfo(ktClass.getContainingKtFile(), sourceFile, ktClass,
                                                         LocationInfo.CodeElementType.TYPE_DECLARATION);
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, ktClass.isTopLevel(), importedTypes);

        if (ktClass.isInterface()) {
            umlClass.setInterface(true);
        }

        processModifiers(sourceFile, ktClass, umlClass);

        if (ktClass.isData()) {
            umlClass.setData(true);
        } else if (ktClass.isSealed()) {
            umlClass.setSealed(true);
        } else if (ktClass.isInner()) {
            umlClass.setInner(true);
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
                    processFieldDeclaration(ktClass.getContainingKtFile(), ktProperty, umlClass.isInterface(),
                                            sourceFile);
            attribute.setClassName(umlClass.getName());
            umlClass.addAttribute(attribute);
        }

        List<KtDeclaration> declarations = ktClass.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtNamedFunction) {
                KtNamedFunction function = (KtNamedFunction) declaration;
                UMLOperation operation =
                        processMethodDeclaration(ktClass, function, packageName, umlClass.isInterface(), sourceFile);
                operation.setClassName(umlClass.getName());
                umlClass.addOperation(operation);
            }
        }

        List<KtObjectDeclaration> companionObjects = ktClass.getCompanionObjects();
        for (KtObjectDeclaration companionObject : companionObjects) {
            UMLCompanionObject umlCompanionObject = processCompanionObject(companionObject, sourceFile);
            umlCompanionObject.setClassName(umlClass.getName());
            umlClass.addCompanionObject(umlCompanionObject);
        }

        this.getUmlModel().addClass(umlClass);
    }

    private UMLAttribute processFieldDeclaration(KtFile ktFile,
                                                 KtProperty fieldDeclaration,
                                                 boolean isInterfaceField,
                                                 String sourceFile) {
        UMLJavadoc javadoc = generateDocComment(fieldDeclaration);
        KtExpression initializer = fieldDeclaration.getInitializer();

        //TODO: figure out how to get dimensions
        UMLType type = UMLType.extractTypeObject(ktFile, sourceFile, fieldDeclaration.getTypeReference(), 0);
        String fieldName = fieldDeclaration.getName();
        LocationInfo locationInfo =
                generateLocationInfo(ktFile, sourceFile, initializer, LocationInfo.CodeElementType.FIELD_DECLARATION);
        UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo);
        VariableDeclaration variableDeclaration = new VariableDeclaration(ktFile, sourceFile, fieldDeclaration);
        variableDeclaration.setAttribute(true);
        umlAttribute.setVariableDeclaration(variableDeclaration);
        umlAttribute.setJavadoc(javadoc);

        KtModifierList propertyModifierList = fieldDeclaration.getModifierList();
        if (propertyModifierList != null) {
            if (propertyModifierList.hasModifier(KtModifierKeywordToken.keywordModifier("public")))
                umlAttribute.setVisibility("public");
            else if (propertyModifierList.hasModifier(KtModifierKeywordToken.keywordModifier("protected")))
                umlAttribute.setVisibility("protected");
            else if (propertyModifierList.hasModifier(KtModifierKeywordToken.keywordModifier("private")))
                umlAttribute.setVisibility("private");
            else if (isInterfaceField)
                umlAttribute.setVisibility("public");
            else
                umlAttribute.setVisibility("package");
        }

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

    private UMLOperation processMethodDeclaration(KtClass ktClass,
                                                  KtNamedFunction methodDeclaration,
                                                  String packageName,
                                                  boolean isInterfaceMethod,
                                                  String sourceFile) {
        UMLJavadoc javadoc = generateDocComment(methodDeclaration);
        String methodName = methodDeclaration.getName();
        LocationInfo locationInfo = generateLocationInfo(ktClass.getContainingKtFile(), sourceFile, methodDeclaration,
                                                         LocationInfo.CodeElementType.METHOD_DECLARATION);
        UMLOperation umlOperation = new UMLOperation(methodName, locationInfo);
        umlOperation.setJavadoc(javadoc);

        umlOperation.setConstructor(false);

        if (methodDeclaration.hasDeclaredReturnType()) {
            KtTypeReference returnTypeReference = methodDeclaration.getTypeReference();
            if (returnTypeReference != null) {
                //TODO: get extra dimensions
                //TODO: get fully qualified name
                String returnType = returnTypeReference.getTypeElement().getChildren()[0].getFirstChild().getText();
                UMLType type =
                        UMLType.extractTypeObject(ktClass.getContainingKtFile(), sourceFile, returnTypeReference, 0);
                UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
                umlOperation.addParameter(returnParameter);
            }
        }

        KtModifierList methodModifiers = methodDeclaration.getModifierList();
        if (methodModifiers != null) {
            if (methodModifiers.hasModifier(KtModifierKeywordToken.keywordModifier("public")))
                umlOperation.setVisibility("public");
            else if (methodModifiers.hasModifier(KtModifierKeywordToken.keywordModifier("protected")))
                umlOperation.setVisibility("protected");
            else if (methodModifiers.hasModifier(KtModifierKeywordToken.keywordModifier("private")))
                umlOperation.setVisibility("private");
            else if (isInterfaceMethod)
                umlOperation.setVisibility("public");
            else
                umlOperation.setVisibility("package");

            List<KtAnnotation> ktAnnotations = methodModifiers.getAnnotations();
            for (KtAnnotation annotation : ktAnnotations) {
                umlOperation.addAnnotation(new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));
            }
        }

        List<KtTypeParameter> typeParameters = methodDeclaration.getTypeParameters();
        for (KtTypeParameter typeParameter : typeParameters) {
            UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getName());
            KtTypeReference typeBounds = typeParameter.getExtendsBound();
            if (typeBounds != null) {
                umlTypeParameter.addTypeBound(
                        UMLType.extractTypeObject(ktClass.getContainingKtFile(), sourceFile, typeBounds, 0));
            }

            KtModifierList typeParameterExtendedModifiers = typeParameter.getModifierList();
            if (typeParameterExtendedModifiers != null) {
                for (KtAnnotation annotation : typeParameterExtendedModifiers.getAnnotations()) {
                    umlTypeParameter.addAnnotation(
                            new UMLAnnotation(ktClass.getContainingKtFile(), sourceFile, annotation));
                }
            }
            umlOperation.addTypeParameter(umlTypeParameter);
        }

        KtBlockExpression methodBody = methodDeclaration.getBodyBlockExpression();
        if (methodBody != null) {
            OperationBody body = new OperationBody(ktClass.getContainingKtFile(), sourceFile, methodBody);
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

            UMLType type = UMLType.extractTypeObject(parameter.getContainingKtFile(), sourceFile, typeReference, 0);
            UMLParameter umlParameter = new UMLParameter(paramName, type, "in", parameter.isVarArg());
            VariableDeclaration variableDeclaration =
                    new VariableDeclaration(parameter.getContainingKtFile(), sourceFile, parameter,
                                            parameter.isVarArg());
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
                                                               LocationInfo.CodeElementType.COMPANION_OBJECT);
        umlCompanionObject.setLocationInfo(objectLocationInfo);
        List<KtDeclaration> declarations = object.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            LocationInfo locationInfo = generateLocationInfo(declaration.getContainingKtFile(), sourceFile, declaration,
                                                             LocationInfo.CodeElementType.METHOD_DECLARATION);
            UMLOperation method = new UMLOperation(declaration.getName(), locationInfo);
            umlCompanionObject.addMethod(method);
        }
        return umlCompanionObject;
    }

    public void processObject(KtObjectDeclaration objectDeclaration, String sourceFile) {
        UMLObject umlObject = new UMLObject();
        umlObject.setName(objectDeclaration.getName());
        LocationInfo objectLocationInfo =
                generateLocationInfo(objectDeclaration.getContainingKtFile(), sourceFile, objectDeclaration,
                                     LocationInfo.CodeElementType.OBJECT);
        umlObject.setLocationInfo(objectLocationInfo);
        KtClassBody body = objectDeclaration.getBody();
        if (body != null) {
            List<KtNamedFunction> functions = body.getFunctions();
            for (KtNamedFunction function : functions) {
                LocationInfo locationInfo = generateLocationInfo(function.getContainingKtFile(), sourceFile, function,
                                                                 LocationInfo.CodeElementType.METHOD_DECLARATION);
                UMLOperation umlOperation = new UMLOperation(function.getName(), locationInfo);
                umlObject.addMethod(umlOperation);
            }
            List<KtProperty> properties = body.getProperties();
            for (KtProperty property : properties) {
                UMLAttribute umlAttribute =
                        processFieldDeclaration(property.getContainingKtFile(), property, false, sourceFile);
                umlObject.addProperty(umlAttribute);
            }
        }
        this.getUmlModel().addObject(umlObject);
    }

    private void processModifiers(String sourceFile, KtClass typeDeclaration, UMLClass umlClass) {
        KtModifierList modifiers = typeDeclaration.getModifierList();
        if (modifiers != null) {
            if (modifiers.hasModifier(KtModifierKeywordToken.keywordModifier("public")))
                umlClass.setVisibility("public");
            else if (modifiers.hasModifier(KtModifierKeywordToken.keywordModifier("protected")))
                umlClass.setVisibility("protected");
            else if (modifiers.hasModifier(KtModifierKeywordToken.keywordModifier("private")))
                umlClass.setVisibility("private");
            else if (modifiers.hasModifier(KtModifierKeywordToken.keywordModifier("internal")))
                umlClass.setVisibility("internal");
            else
                umlClass.setVisibility("package");

            for (KtAnnotation annotation : modifiers.getAnnotations()) {
                umlClass.addAnnotation(
                        new UMLAnnotation(typeDeclaration.getContainingKtFile(), sourceFile, annotation));
            }
        }
    }

    public PsiFile buildPsiFile(String file, KotlinCoreEnvironment environment, String content) throws IOException {
        File newFile = new File(file);
        FileUtilRt.createDirectory(newFile);
        PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject());
        KotlinLightVirtualFile virtualFile = new KotlinLightVirtualFile(newFile, content);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
    }

    public UMLModel getUmlModel() {
        return this.umlModel;
    }

    private LocationInfo generateLocationInfo(KtFile ktFile,
                                              String sourceFile,
                                              KtElement node,
                                              LocationInfo.CodeElementType codeElementType) {
        return new LocationInfo(ktFile, sourceFile, node, codeElementType);
    }

}

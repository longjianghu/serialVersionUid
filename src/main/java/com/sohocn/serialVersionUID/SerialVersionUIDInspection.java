package com.sohocn.serialVersionUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

/**
 * 检查实现了Serializable接口的类是否有serialVersionUID字段
 */
public class SerialVersionUIDInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public String getShortName() {
        return "SerialVersionUID";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Serializable class should have serialVersionUID";
    }

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Serialization";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(aClass)) {
            return null;
        }

        // 检查类是否已经有serialVersionUID字段
        PsiField serialVersionUIDField = SerialVersionUIDGenerator.findSerialVersionUIDField(aClass);
        if (serialVersionUIDField != null) {
            // 如果已经有字段，检查是否有@Serial注解
            boolean hasSerialAnnotation = false;
            PsiAnnotation[] annotations = serialVersionUIDField.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if ("java.io.Serial".equals(annotation.getQualifiedName())) {
                    hasSerialAnnotation = true;
                    break;
                }
            }

            if (!hasSerialAnnotation) {
                // 如果没有@Serial注解，提示添加
                return new ProblemDescriptor[]{
                        manager.createProblemDescriptor(
                                serialVersionUIDField,
                                "serialVersionUID field should have @Serial annotation",
                                new AddSerialAnnotationQuickFix(),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                isOnTheFly
                        )
                };
            }
            return null;
        }

        // 如果没有serialVersionUID字段，提示添加
        return new ProblemDescriptor[]{
                manager.createProblemDescriptor(
                        aClass.getNameIdentifier() != null ? aClass.getNameIdentifier() : aClass,
                        "Serializable class should have serialVersionUID field",
                        new AddSerialVersionUIDQuickFix(),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly
                )
        };
    }

    /**
     * 添加@Serial注解的快速修复
     */
    private static class AddSerialAnnotationQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add @Serial annotation";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (!(element instanceof PsiField)) {
                return;
            }

            PsiField field = (PsiField) element;
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiAnnotation annotation = factory.createAnnotationFromText("@Serial", field);
            field.getModifierList().addBefore(annotation, field.getModifierList().getFirstChild());

            // 添加导入
            PsiFile file = field.getContainingFile();
            if (file instanceof PsiJavaFile) {
                PsiImportList importList = ((PsiJavaFile) file).getImportList();
                if (importList != null) {
                    boolean hasSerialImport = false;
                    for (PsiImportStatement importStatement : importList.getImportStatements()) {
                        if ("java.io.Serial".equals(importStatement.getQualifiedName())) {
                            hasSerialImport = true;
                            break;
                        }
                    }

                    if (!hasSerialImport) {
                        PsiClass serialClass = JavaPsiFacade.getInstance(project)
                                .findClass("java.io.Serial", field.getResolveScope());
                        if (serialClass != null) {
                            PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                            importList.add(importStatement);
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加serialVersionUID字段的快速修复
     */
    private static class AddSerialVersionUIDQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add serialVersionUID field";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiClass psiClass = element instanceof PsiClass ? (PsiClass) element : 
                               (element instanceof PsiIdentifier ? (PsiClass) element.getParent() : null);
            
            if (psiClass == null) {
                return;
            }

            // 生成serialVersionUID值
            long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);

            // 创建serialVersionUID字段
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            String fieldText = "@Serial\nprivate static final long serialVersionUID = " + serialVersionUID + "L;";
            PsiField field = factory.createFieldFromText(fieldText, psiClass);

            // 添加字段到类中
            PsiElement anchor = null;
            PsiField[] fields = psiClass.getFields();
            if (fields.length > 0) {
                anchor = fields[0];
            } else {
                PsiMethod[] methods = psiClass.getMethods();
                if (methods.length > 0) {
                    anchor = methods[0];
                } else {
                    PsiClass[] innerClasses = psiClass.getInnerClasses();
                    if (innerClasses.length > 0) {
                        anchor = innerClasses[0];
                    }
                }
            }

            if (anchor != null) {
                psiClass.addBefore(field, anchor);
            } else {
                psiClass.add(field);
            }

            // 添加导入
            PsiFile file = psiClass.getContainingFile();
            if (file instanceof PsiJavaFile) {
                PsiImportList importList = ((PsiJavaFile) file).getImportList();
                if (importList != null) {
                    boolean hasSerialImport = false;
                    for (PsiImportStatement importStatement : importList.getImportStatements()) {
                        if ("java.io.Serial".equals(importStatement.getQualifiedName())) {
                            hasSerialImport = true;
                            break;
                        }
                    }

                    if (!hasSerialImport) {
                        PsiClass serialClass = JavaPsiFacade.getInstance(project)
                                .findClass("java.io.Serial", psiClass.getResolveScope());
                        if (serialClass != null) {
                            PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                            importList.add(importStatement);
                        }
                    }
                }
            }
        }
    }
} 
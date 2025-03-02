package com.sohocn.serialVersionUID;

import java.util.Objects;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

/**
 * 工具类，提供共用的方法
 */
public class SerialVersionUIDUtils {

    /**
     * 查找添加字段的位置
     *
     * @param psiClass 要添加字段的类
     * @return 添加字段的锚点元素
     */
    public static PsiElement findAnchorForField(PsiClass psiClass) {
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            return fields[0];
        }
        
        PsiMethod[] methods = psiClass.getMethods();
        if (methods.length > 0) {
            return methods[0];
        }
        
        PsiClass[] innerClasses = psiClass.getInnerClasses();
        if (innerClasses.length > 0) {
            return innerClasses[0];
        }
        
        return null;
    }

    /**
     * 如果需要，添加Serial注解的导入
     *
     * @param psiClass 要添加导入的类
     * @param project 当前项目
     */
    public static void addSerialAnnotationIfNeeded(PsiClass psiClass, Project project) {
        PsiFile file = psiClass.getContainingFile();
        if (file instanceof PsiJavaFile javaFile) {
            PsiImportList importList = javaFile.getImportList();
            
            if (importList != null) {
                boolean hasSerialImport = false;
                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                    if ("java.io.Serial".equals(importStatement.getQualifiedName())) {
                        hasSerialImport = true;
                        break;
                    }
                }
                
                if (!hasSerialImport) {
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    PsiClass serialClass = JavaPsiFacade.getInstance(project).findClass("java.io.Serial", psiClass.getResolveScope());
                    if (serialClass != null) {
                        PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                        importList.add(importStatement);
                    }
                }
            }
        }
    }

  public   static void serialVersionUID(Project project, PsiClass psiClass) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField existingField = SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass);

        long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);

        if (existingField != null) {
            // 更新现有字段的值
            PsiExpression initializer = factory.createExpressionFromText(serialVersionUID + "L", psiClass);
            Objects.requireNonNull(existingField.getInitializer()).replace(initializer);
        } else {
            // 创建新的serialVersionUID字段
            String fieldText = SerialVersionUIDGenerator.createSerialVersionUIDFieldText(serialVersionUID, project);
            PsiField field = factory.createFieldFromText(fieldText, psiClass);

            // 添加字段到类中
            PsiElement anchor = SerialVersionUIDUtils.findAnchorForField(psiClass);
            if (anchor != null) {
                psiClass.addBefore(field, anchor);
            } else {
                psiClass.add(field);
            }

            // 检查是否需要导入Serial注解
            if (SerialVersionUIDGenerator.shouldUseSerialAnnotation(project)) {
                SerialVersionUIDUtils.addSerialAnnotationIfNeeded(psiClass, project);
            }

            // 优化导入
            SerialVersionUIDUtils.optimizeImports(psiClass, project);
        }
    }
    
    /**
     * 优化导入
     *
     * @param psiClass 要优化导入的类
     * @param project 当前项目
     */
    public static void optimizeImports(PsiClass psiClass, Project project) {
        JavaCodeStyleManager.getInstance(project).optimizeImports(psiClass.getContainingFile());
    }
} 
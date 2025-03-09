package com.sohocn.serialVersionUid.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * 为实现了Serializable接口但没有serialVersionUID字段的类提供生成serialVersionUID的意图动作
 */
public class GenerateSerialVersionUIDIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return "Generate/Update serialVersionUID";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Serialization";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 检查当前元素是否在一个类中
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return false;
        }

        // 检查类是否实现了Serializable接口（无论是否已有serialVersionUID字段）
        return SerialVersionUIDGenerator.isSerializable(psiClass);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        // 获取当前类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        // 生成serialVersionUID字段
        String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);

        // 检查是否已存在serialVersionUID字段
        if (SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
            // 查找现有的serialVersionUID字段并替换
            PsiField[] fields = psiClass.getFields();
            for (PsiField existingField : fields) {
                if ("serialVersionUID".equals(existingField.getName())) {
                    existingField.replace(field);
                    break;
                }
            }
        } else {
            // 添加新字段到类中
            PsiElement anchor = findAnchor(psiClass);
            if (anchor != null) {
                psiClass.addBefore(field, anchor);
            } else {
                psiClass.add(field);
            }
        }

        // 添加必要的导入语句
        boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(project);
        if (useSerialAnnotation) {
            try {
                PsiFile file = element.getContainingFile();
                // 检查是否在预览环境中（DummyHolder）
                if (file != null && file.isPhysical() && file instanceof PsiJavaFile) {
                    PsiJavaFile javaFile = (PsiJavaFile) file;
                    PsiImportList importList = javaFile.getImportList();
                    if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                        PsiClass serialClass = JavaPsiFacade.getInstance(project).findClass(
                                "java.io.Serial",
                                psiClass.getResolveScope()
                        );
                        if (serialClass != null) {
                            importList.add(factory.createImportStatement(serialClass));
                        }
                    }
                }
            } catch (PsiInvalidElementAccessException e) {
                // 在预览模式下忽略导入语句的添加
            }
        }
    }

    /**
     * 查找添加字段的位置，确保serialVersionUID字段位于类的所有字段之前
     */
    private PsiElement findAnchor(PsiClass psiClass) {
        // 获取类的所有字段
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            // 返回第一个字段，确保serialVersionUID字段位于类的所有字段之前
            return fields[0];
        }
        
        // 如果没有字段，则获取类的所有方法
        PsiMethod[] methods = psiClass.getMethods();
        if (methods.length > 0) {
            // 返回第一个方法，确保serialVersionUID字段位于类的所有方法之前
            return methods[0];
        }
        
        // 如果没有字段和方法，则获取类的所有子元素
        PsiElement[] children = psiClass.getChildren();
        for (PsiElement child : children) {
            // 跳过注释、空白等非代码元素
            if (child instanceof PsiField || child instanceof PsiMethod || child instanceof PsiClass) {
                return child;
            }
        }
        
        return null;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
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
    
        // 移除Serializable接口检查，对所有类都可用
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        // 获取当前类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }
    
        // 使用新的完整生成方法，会自动添加Serializable接口（如果需要）
        String serialVersionUIDCode = SerialVersionUIDGenerator.generateCompleteSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    
        // 生成serialVersionUID字段
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
        // 获取类的第一个代码元素作为锚点
        PsiElement firstCodeElement = null;
        
        // 遍历类的所有子元素，找到第一个有效的代码元素
        for (PsiElement child : psiClass.getChildren()) {
            // 检查元素是否是有效的代码元素，并且确保它是类的直接子元素
            if ((child instanceof PsiField || child instanceof PsiMethod || child instanceof PsiClass) && 
                child.getParent() == psiClass) {
                firstCodeElement = child;
                break;
            }
        }
        
        // 如果找到了有效的锚点，返回它
        if (firstCodeElement != null) {
            return firstCodeElement;
        }
        
        return null;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
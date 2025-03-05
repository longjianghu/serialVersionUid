package com.sohoch.serialVersionUid.intention;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.sohoch.serialVersionUid.util.SerialVersionUIDGenerator;

public class GenerateSerialVersionUIDIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return "生成 serialVersionUID";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Serialization";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (project == null || editor == null) {
            return false;
        }

        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return false;
        }

        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return false;
        }

        // 检查是否已经存在serialVersionUID字段
        PsiField existingField = psiClass.findFieldByName("serialVersionUID", false);
        return existingField == null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

            // 检查是否已存在serialVersionUID字段
            PsiField existingField = psiClass.findFieldByName("serialVersionUID", false);
            String fieldText = String.format("private static final long serialVersionUID = %dL;", serialVersionUID);

            if (SerialVersionUIDGenerator.isJavaVersionAtLeast14(ModuleUtil.findModuleForPsiElement(psiClass))) {
                // 添加@Serial注解
                fieldText = "@Serial\n" + fieldText;

                // 确保导入java.io.Serial类
                PsiClass serialClass =
                    JavaPsiFacade.getInstance(project).findClass("java.io.Serial", GlobalSearchScope.allScope(project));

                if (serialClass != null) {
                    PsiJavaFile javaFile = (PsiJavaFile)psiClass.getContainingFile();
                    javaFile.importClass(serialClass);
                }
            }

            if (existingField != null) {
                // 更新现有字段
                existingField.replace(factory.createFieldFromText(fieldText, psiClass));
            } else {
                // 创建新字段
                psiClass.add(factory.createFieldFromText(fieldText, psiClass));
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
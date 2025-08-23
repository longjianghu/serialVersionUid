package com.sohocn.serialVersionUid.intention;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;

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
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        return psiClass != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }
    
        SerialVersionUIDGenerator.generateAndAddSerialVersionUID(psiClass, project);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
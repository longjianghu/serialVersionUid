package com.sohocn.serialVersionUID;

import static com.sohocn.serialVersionUID.SerialVersionUIDUtils.serialVersionUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * 为实现了Serializable接口的类添加serialVersionUID字段的意图动作
 */
public class AddSerialVersionUIDIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return "Generate serialVersionUID";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "SerialVersionUID generator";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 检查当前元素是否在一个类中
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return false;
        }

        // 检查类是否实现了Serializable接口
        return SerialVersionUIDGenerator.isSerializable(psiClass);

        // 如果已经有字段，我们仍然提供更新选项
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        serialVersionUID(project, psiClass);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
} 
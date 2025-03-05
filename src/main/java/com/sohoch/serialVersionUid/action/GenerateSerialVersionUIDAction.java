package com.sohoch.serialVersionUid.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.sohoch.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

public class GenerateSerialVersionUIDAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        if (psiClass == null || !SerialVersionUIDGenerator.isSerializable(psiClass)) {
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
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        e.getPresentation().setEnabledAndVisible(
            psiClass != null && SerialVersionUIDGenerator.isSerializable(psiClass)
        );
    }
}
package com.sohocn.serialVersionUid.action;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;

/**
 * 在生成菜单（Alt+Insert）中提供生成serialVersionUID的操作
 */
public class GenerateSerialVersionUIDAction extends BaseGenerateAction {
    public GenerateSerialVersionUIDAction() {
        super(null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiClass psiClass = this.getPsiClass(e);
    
        String commandName = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "Update serialVersionUID" : "Generate serialVersionUID";
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> {
            SerialVersionUIDGenerator.generateAndAddSerialVersionUID(psiClass, project);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiClass psiClass = this.getPsiClass(e);

        boolean hasSerialVersionUID = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass);
        e.getPresentation().setText(hasSerialVersionUID ? "Update SerialVersionUID" : "Generate SerialVersionUID");
        e.getPresentation().setEnabled(true);
    }

    private PsiClass getPsiClass(AnActionEvent e){
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || file == null || !(file instanceof PsiJavaFile)) {
            return null;
        }

        // 获取当前光标位置的类
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }
}
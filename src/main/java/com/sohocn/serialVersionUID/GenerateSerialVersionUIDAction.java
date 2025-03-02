package com.sohocn.serialVersionUID;

import static com.sohocn.serialVersionUID.SerialVersionUIDUtils.serialVersionUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * 在右键菜单中添加生成serialVersionUID的选项
 */
public class GenerateSerialVersionUIDAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // 获取当前光标位置的元素
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return;
        }

        // 查找包含当前元素的类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return;
        }

        // 在写入命令中执行添加serialVersionUID的操作
        WriteCommandAction.runWriteCommandAction(project, () -> serialVersionUID(project, psiClass));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 只有在编辑Java文件且光标在类内部时才启用此操作
        boolean enabled = false;
        if (project != null && editor != null && psiFile instanceof PsiJavaFile) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            if (element != null) {
                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                if (SerialVersionUIDGenerator.isSerializable(psiClass)) {
                    enabled = true;
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
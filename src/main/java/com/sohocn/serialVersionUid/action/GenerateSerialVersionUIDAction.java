package com.sohocn.serialVersionUid.action;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

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
        if (project == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || file == null) {
            return;
        }

        // 获取当前光标位置的类
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return;
        }

        // 生成serialVersionUID字段
        String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);

        // 使用WriteCommandAction包装PSI修改操作
        String commandName = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "Update serialVersionUID" : "Generate serialVersionUID";
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> {
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
                // 添加字段到类中
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
                if (file instanceof PsiJavaFile) {
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
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

        // 禁用菜单项，直到我们确认当前位置有一个可以添加或更新serialVersionUID的类
        e.getPresentation().setEnabled(false);

        if (project == null || editor == null || file == null || !(file instanceof PsiJavaFile)) {
            return;
        }

        // 获取当前光标位置的类
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        // 检查类是否实现了Serializable接口（无论是否已有serialVersionUID字段）
        if (SerialVersionUIDGenerator.isSerializable(psiClass)) {
            // 根据是否已有serialVersionUID字段设置不同的菜单文本
            boolean hasSerialVersionUID = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass);
            e.getPresentation().setText(hasSerialVersionUID ? "Update SerialVersionUID" : "Generate SerialVersionUID");
            e.getPresentation().setEnabled(true);
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
}
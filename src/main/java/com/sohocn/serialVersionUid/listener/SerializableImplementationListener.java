package com.sohocn.serialVersionUid.listener;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * 监听Serializable接口的实现，并提示生成serialVersionUID
 */
public class SerializableImplementationListener implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) element;
            // 检查类是否实现了Serializable接口
            if (SerialVersionUIDGenerator.isSerializable(psiClass) && !SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                // 显示提示，询问用户是否生成serialVersionUID
                ApplicationManager.getApplication().invokeLater(() -> {
                    Project project = element.getProject();
                    if (project.isDisposed() || !psiClass.isValid()) return;
                    
                    // 显示提示，询问用户是否生成serialVersionUID
                    showSerialVersionUIDNotification(project, psiClass);
                });
            }
        }
        return false;
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
    
    /**
     * 显示通知，询问用户是否生成serialVersionUID
     */
    private void showSerialVersionUIDNotification(Project project, PsiClass psiClass) {
        com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("SerialVersionUID Generator")
                .createNotification(
                        "SerialVersionUID Generator",
                        "是否为类 '" + psiClass.getName() + "' 生成serialVersionUID字段？",
                        com.intellij.notification.NotificationType.INFORMATION)
                .addAction(new com.intellij.notification.NotificationAction("生成") {
                    @Override
                    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, @NotNull com.intellij.notification.Notification notification) {
                        notification.expire();
                        generateSerialVersionUID(project, psiClass);
                    }
                })
                .addAction(new com.intellij.notification.NotificationAction("取消") {
                    @Override
                    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, @NotNull com.intellij.notification.Notification notification) {
                        notification.expire();
                    }
                })
                .notify(project);
    }
    
    /**
     * 为类生成或更新serialVersionUID字段
     */
    private void generateSerialVersionUID(Project project, PsiClass psiClass) {
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return;
        }
        
        // 生成serialVersionUID字段
        String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);
        
        // 使用WriteCommandAction包装PSI修改操作
        String commandName = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "Update serialVersionUID" : "Generate serialVersionUID";
        WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> {
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
                PsiFile file = psiClass.getContainingFile();
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
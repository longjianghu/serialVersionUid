package com.sohocn.serialVersionUid.listener;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.PsiTreeChangePreprocessor;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * 监听Java类的变化，特别是当类实现了Serializable接口时
 * 自动提示生成serialVersionUID
 */
public class SerializableClassListener implements PsiTreeChangePreprocessor {

    private final Project myProject;

    public SerializableClassListener(Project project) {
        myProject = project;
    }

    @Override
    public void treeChanged(@NotNull PsiTreeChangeEventImpl event) {
        // 只处理子树改变事件
        if (event.getCode() != PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED &&
            event.getCode() != PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED &&
            event.getCode() != PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED) {
            return;
        }

        // 检查是否是Java文件
        PsiFile file = event.getFile();
        if (!(file instanceof PsiJavaFile)) {
            return;
        }

        // 获取改变的元素
        PsiElement child = event.getChild();
        if (child == null) {
            return;
        }

        // 检查是否是实现列表的变化
        if (child instanceof PsiReferenceList) {
            PsiReferenceList referenceList = (PsiReferenceList) child;
            if (referenceList.getRole() == PsiReferenceList.Role.IMPLEMENTS_LIST) {
                // 获取包含的类
                PsiClass psiClass = PsiTreeUtil.getParentOfType(referenceList, PsiClass.class);
                if (psiClass != null) {
                    // 检查类是否实现了Serializable接口
                    if (SerialVersionUIDGenerator.isSerializable(psiClass) && !SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                        // 显示提示，询问用户是否生成serialVersionUID
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (psiClass.isValid() && !myProject.isDisposed()) {
                                showSerialVersionUIDNotification(psiClass);
                            }
                        });
                    }
                }
            }
        }

        // 检查是否是类的变化
        PsiClass psiClass = PsiTreeUtil.getParentOfType(child, PsiClass.class);
        if (psiClass != null) {
            // 检查类是否实现了Serializable接口
            if (SerialVersionUIDGenerator.isSerializable(psiClass) && !SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                // 显示提示，询问用户是否生成serialVersionUID
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (psiClass.isValid() && !myProject.isDisposed()) {
                        showSerialVersionUIDNotification(psiClass);
                    }
                });
            }
        }
    }

    /**
     * 显示通知，询问用户是否生成serialVersionUID
     */
    private void showSerialVersionUIDNotification(PsiClass psiClass) {
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
                        generateSerialVersionUID(psiClass);
                    }
                })
                .addAction(new com.intellij.notification.NotificationAction("取消") {
                    @Override
                    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, @NotNull com.intellij.notification.Notification notification) {
                        notification.expire();
                    }
                })
                .notify(myProject);
    }

    /**
     * 为类生成或更新serialVersionUID字段
     */
    private void generateSerialVersionUID(PsiClass psiClass) {
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return;
        }
        
        // 生成serialVersionUID字段
        String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(myProject);
        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);
        
        // 使用WriteCommandAction包装PSI修改操作
        String commandName = SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "Update serialVersionUID" : "Generate serialVersionUID";
        WriteCommandAction.runWriteCommandAction(myProject, commandName, null, () -> {
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
            boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(myProject);
            if (useSerialAnnotation) {
                PsiFile file = psiClass.getContainingFile();
                if (file instanceof PsiJavaFile) {
                    PsiJavaFile javaFile = (PsiJavaFile) file;
                    PsiImportList importList = javaFile.getImportList();
                    if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                        PsiClass serialClass = JavaPsiFacade.getInstance(myProject).findClass(
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
package com.sohocn.serialVersionUid.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * 提供serialVersionUID的代码完成功能
 */
public class SerialVersionUIDCompletionContributor extends CompletionContributor {

    public SerialVersionUIDCompletionContributor() {
        // 方法1：在类中输入serialVersionUID时提供自动完成
        extend(
                CompletionType.BASIC,
                PsiJavaPatterns.psiElement().inside(PsiJavaPatterns.psiClass().withQualifiedName(PsiJavaPatterns.string().contains("."))),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                 @NotNull ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiClass psiClass = findContainingClass(position);

                        if (psiClass != null && SerialVersionUIDGenerator.isSerializable(psiClass)) {
                            String text = position.getText();
                            if (text.contains("serialVersionUID")) {
                                result.addElement(LookupElementBuilder.create("serialVersionUID")
                                        .withPresentableText(SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "serialVersionUID (update field)" : "serialVersionUID (generate field)")
                                        .withInsertHandler((context1, item) -> {
                                            int offset = context1.getEditor().getCaretModel().getOffset();
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            
                                            String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                            
                                            // 如果已存在serialVersionUID字段，则替换它
                                            if (SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                                                PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                                PsiField[] fields = psiClass.getFields();
                                                for (PsiField existingField : fields) {
                                                    if ("serialVersionUID".equals(existingField.getName())) {
                                                        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                        PsiField newField = factory.createFieldFromText(serialVersionUIDCode, psiClass);
                                                        existingField.replace(newField);
                                                        return;
                                                    }
                                                }
                                            } else {
                                                // 如果不存在，则插入新字段
                                                context1.getDocument().insertString(offset, serialVersionUIDCode);
                                            }
                                            
                                            // 添加必要的导入语句
                                            boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                            if (useSerialAnnotation) {
                                                PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                                PsiFile file = context1.getFile();
                                                if (file instanceof PsiJavaFile) {
                                                    PsiJavaFile javaFile = (PsiJavaFile) file;
                                                    PsiImportList importList = javaFile.getImportList();
                                                    if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                                                        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                        importList.add(factory.createImportStatement(
                                                                JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                                        "java.io.Serial",
                                                                        psiClass.getResolveScope()
                                                                )
                                                        ));
                                                    }
                                                }
                                            }
                                        }));
                            }
                        }
                    }
                }
        );

        // 方法2：在接口列表中输入Serializable时提供自动完成
        extend(
                CompletionType.BASIC,
                PsiJavaPatterns.psiElement().inside(PsiJavaPatterns.psiClass().withQualifiedName(PsiJavaPatterns.string().contains("."))),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                 @NotNull ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiClass psiClass = findContainingClass(position);

                        if (psiClass != null && !SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                            String text = position.getText();
                            if (text.contains("Serializable") && isInImplementsList(position)) {
                                result.addElement(LookupElementBuilder.create("Serializable")
                                        .withPresentableText("Serializable")
                                        .withInsertHandler((context1, item) -> {
                                            // 添加Serializable接口
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            context1.getDocument().insertString(context1.getStartOffset(), "Serializable");
                                            
                                            // 添加必要的导入语句
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            PsiFile file = context1.getFile();
                                            if (file instanceof PsiJavaFile) {
                                                PsiJavaFile javaFile = (PsiJavaFile) file;
                                                PsiImportList importList = javaFile.getImportList();
                                                if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serializable")) {
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    importList.add(factory.createImportStatement(
                                                            JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                                    "java.io.Serializable",
                                                                    psiClass.getResolveScope()
                                                            )
                                                    ));
                                                }
                                            }
                                            
                                            // 提示用户是否生成serialVersionUID
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            
                                            if (psiClass != null) {
                                                // 显示通知，询问用户是否生成serialVersionUID
                                                ApplicationManager.getApplication().invokeLater(() -> {
                                                    if (psiClass.isValid() && !psiClass.getProject().isDisposed()) {
                                                        com.intellij.notification.NotificationGroupManager.getInstance()
                                                                .getNotificationGroup("SerialVersionUID Generator")
                                                                .createNotification(
                                                                        "SerialVersionUID Generator",
                                                                        "是否为类 '" + psiClass.getName() + "' 生成serialVersionUID字段？",
                                                                        com.intellij.notification.NotificationType.INFORMATION)
                                                                .addAction(new com.intellij.notification.NotificationAction("生成") {
                                                                    @Override
                                                                    public void actionPerformed(@NotNull com.intellij.notification.Notification notification, @NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                                                                        notification.expire();
                                                                        // 生成serialVersionUID字段
                                                                        String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                                                        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                                        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);
                                                                        
                                                                        // 使用WriteCommandAction包装PSI修改操作
                                                                        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(psiClass.getProject(), "Generate serialVersionUID", null, () -> {
                                                                            // 添加字段到类的最前面
                                                                            PsiElement[] children = psiClass.getChildren();
                                                                            if (children.length > 0) {
                                                                                psiClass.addBefore(field, children[0]);
                                                                            } else {
                                                                                psiClass.add(field);
                                                                            }
                                                                            
                                                                            // 添加@Serial注解的导入（如果需要）
                                                                            boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                                                            if (useSerialAnnotation) {
                                                                                PsiFile file = psiClass.getContainingFile();
                                                                                if (file instanceof PsiJavaFile) {
                                                                                    PsiJavaFile javaFile = (PsiJavaFile) file;
                                                                                    PsiImportList importList = javaFile.getImportList();
                                                                                    if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                                                                                        PsiClass serialClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
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
                                                                })
                                                                .addAction(new com.intellij.notification.NotificationAction("取消") {
                                                                    @Override
                                                                    public void actionPerformed(@NotNull com.intellij.notification.Notification notification, @NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                                                                        notification.expire();
                                                                    }
                                                                })
                                                                .notify(psiClass.getProject());
                                                    }
                                                });
                                            }
                                        }));
                            }
                        }
                    }
                }
        );
        
        // 方法3：在任何位置输入Serializable时提供自动完成，不需要用户先实现Serializable接口
        extend(
                CompletionType.BASIC,
                PsiJavaPatterns.psiElement().inside(PsiJavaPatterns.psiClass().withQualifiedName(PsiJavaPatterns.string().contains("."))),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                 @NotNull ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiClass psiClass = findContainingClass(position);

                        if (psiClass != null && !SerialVersionUIDGenerator.isSerializable(psiClass) && !SerialVersionUIDGenerator.hasSerialVersionUID(psiClass)) {
                            String text = position.getText();
                            if (text.contains("Serializable")) {
                                result.addElement(LookupElementBuilder.create("Serializable")
                                        .withPresentableText("Serializable (添加接口)")
                                        .withInsertHandler((context1, item) -> {
                                            // 删除当前输入
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            context1.getDocument().insertString(context1.getStartOffset(), "Serializable");
                                            
                                            // 添加Serializable接口到类的实现列表
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            
                                            // 检查类是否已经有implements语句
                                            PsiReferenceList implementsList = psiClass.getImplementsList();
                                            if (implementsList == null) {
                                                // 如果没有implements语句，添加一个
                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                String classText = "class Dummy implements Serializable {}"; 
                                                PsiClass dummyClass = factory.createClassFromText(classText, psiClass);
                                                PsiReferenceList newImplementsList = dummyClass.getImplementsList();
                                                if (newImplementsList != null) {
                                                    // 找到类的左大括号位置
                                                    PsiElement lBrace = psiClass.getLBrace();
                                                    if (lBrace != null) {
                                                        psiClass.addBefore(newImplementsList, lBrace);
                                                    }
                                                }
                                            } else {
                                                // 如果已经有implements语句，添加Serializable接口
                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                PsiJavaCodeReferenceElement referenceElement = factory.createReferenceElementByFQClassName(
                                                        "java.io.Serializable", psiClass.getResolveScope());
                                                implementsList.add(referenceElement);
                                            }
                                            
                                            // 添加必要的导入语句
                                            PsiFile file = context1.getFile();
                                            if (file instanceof PsiJavaFile) {
                                                PsiJavaFile javaFile = (PsiJavaFile) file;
                                                PsiImportList importList = javaFile.getImportList();
                                                if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serializable")) {
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    importList.add(factory.createImportStatement(
                                                            JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                                    "java.io.Serializable",
                                                                    psiClass.getResolveScope()
                                                            )
                                                    ));
                                                }
                                            }
                                            
                                            // 提示用户是否生成serialVersionUID
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            
                                            // 显示通知，询问用户是否生成serialVersionUID
                                            ApplicationManager.getApplication().invokeLater(() -> {
                                                if (psiClass.isValid() && !psiClass.getProject().isDisposed()) {
                                                    com.intellij.notification.NotificationGroupManager.getInstance()
                                                            .getNotificationGroup("SerialVersionUID Generator")
                                                            .createNotification(
                                                                    "SerialVersionUID Generator",
                                                                    "是否为类 '" + psiClass.getName() + "' 生成serialVersionUID字段？",
                                                                    com.intellij.notification.NotificationType.INFORMATION)
                                                            .addAction(new com.intellij.notification.NotificationAction("生成") {
                                                                @Override
                                                                public void actionPerformed(@NotNull com.intellij.notification.Notification notification, @NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                                                                    notification.expire();
                                                                    // 生成serialVersionUID字段
                                                                    String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                                    PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);
                                                                    
                                                                    // 使用WriteCommandAction包装PSI修改操作
                                                                    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(psiClass.getProject(), "Generate serialVersionUID", null, () -> {
                                                                        // 添加字段到类的最前面
                                                                        PsiElement[] children = psiClass.getChildren();
                                                                        if (children.length > 0) {
                                                                            psiClass.addBefore(field, children[0]);
                                                                        } else {
                                                                            psiClass.add(field);
                                                                        }
                                                                        
                                                                        // 添加@Serial注解的导入（如果需要）
                                                                        boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                                                        if (useSerialAnnotation) {
                                                                            PsiJavaFile javaFile = (PsiJavaFile) file;
                                                                            PsiImportList importList = javaFile.getImportList();
                                                                            if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                                                                                importList.add(factory.createImportStatement(
                                                                                        JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                                                                "java.io.Serial",
                                                                                                psiClass.getResolveScope()
                                                                                        )
                                                                                ));
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            })
                                                            .addAction(new com.intellij.notification.NotificationAction("取消") {
                                                                @Override
                                                                public void actionPerformed(@NotNull com.intellij.notification.Notification notification, @NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                                                                    notification.expire();
                                                                }
                                                            })
                                                            .notify(psiClass.getProject());
                                                }
                                            });
                                        }));
                            }
                        }
                    }
                }
        );
    }

    /**
     * 查找包含当前元素的类
     */
    private PsiClass findContainingClass(PsiElement element) {
        while (element != null && !(element instanceof PsiClass)) {
            element = element.getParent();
        }
        return (PsiClass) element;
    }

    /**
     * 判断当前元素是否在实现接口列表中
     */
    private boolean isInImplementsList(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiReferenceList) {
                PsiReferenceList referenceList = (PsiReferenceList) parent;
                return referenceList.getRole() == PsiReferenceList.Role.IMPLEMENTS_LIST;
            }
            parent = parent.getParent();
        }
        return false;
    }
}
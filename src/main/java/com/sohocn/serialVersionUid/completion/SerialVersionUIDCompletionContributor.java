package com.sohocn.serialVersionUid.completion;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.sohocn.serialVersionUid.util.SerialVersionUIDGenerator;

/**
 * The type Serial version uid completion contributor.
 *
 * @author longjianghu
 */
public class SerialVersionUIDCompletionContributor extends CompletionContributor {
    /**
     * Instantiates a new Serial version uid completion contributor.
     */
    public SerialVersionUIDCompletionContributor() {
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

                    if (SerialVersionUIDGenerator.isSerializable(psiClass)) {
                            String text = position.getText();
                            if (text.contains("serialVersionUID")) {
                                result.addElement(LookupElementBuilder.create("serialVersionUID")
                                        .withPresentableText(SerialVersionUIDGenerator.hasSerialVersionUID(psiClass) ? "serialVersionUID (update field)" : "serialVersionUID (generate field)")
                                        .withInsertHandler((context1, item) -> {
                                            int offset = context1.getEditor().getCaretModel().getOffset();
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            
                                            String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                            
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
                                                context1.getDocument().insertString(offset, serialVersionUIDCode);
                                            }
                                            
                                            boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                            if (useSerialAnnotation) {
                                                PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                                PsiFile file = context1.getFile();
                                        if (file instanceof PsiJavaFile javaFile) {
                                                    PsiImportList importList = javaFile.getImportList();
                                                    if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serial")) {
                                                        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
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
                                        }));
                            }
                        }
                    }
                }
        );

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
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            context1.getDocument().insertString(context1.getStartOffset(), "Serializable");

                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            PsiFile file = context1.getFile();
                                    if (file instanceof PsiJavaFile javaFile) {
                                                PsiImportList importList = javaFile.getImportList();
                                                if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serializable")) {
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    PsiClass serializableClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                            "java.io.Serializable",
                                                            psiClass.getResolveScope()
                                                    );
                                                    if (serializableClass != null) {
                                                        importList.add(factory.createImportStatement(serializableClass));
                                                    }
                                                }
                                            }

                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());

                                            ApplicationManager.getApplication().invokeLater(() -> {
                                                if (psiClass.isValid() && !psiClass.getProject().isDisposed()) {
                                                    String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);

                                            com.intellij.openapi.command.WriteCommandAction
                                                .runWriteCommandAction(psiClass.getProject(),
                                                    "Generate SerialVersionUID", null, () -> {
                                                        PsiElement[] children = psiClass.getChildren();
                                                        if (children.length > 0) {
                                                            psiClass.addBefore(field, children[0]);
                                                        } else {
                                                            psiClass.add(field);
                                                        }

                                                        boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                                        if (useSerialAnnotation) {
                                                            PsiFile psiFile = psiClass.getContainingFile();
                                                            if (psiFile instanceof PsiJavaFile) {
                                                        assert file instanceof PsiJavaFile;
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
                                            });
                                        }));
                            }
                        }
                    }
                }
        );
        
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
                                            context1.getDocument().deleteString(context1.getStartOffset(), context1.getTailOffset());
                                            context1.getDocument().insertString(context1.getStartOffset(), "Serializable");
                                            
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            
                                            PsiReferenceList implementsList = psiClass.getImplementsList();
                                            if (implementsList == null) {
                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                String classText = "class Dummy implements Serializable {}"; 
                                                PsiClass dummyClass = factory.createClassFromText(classText, psiClass);
                                                PsiReferenceList newImplementsList = dummyClass.getImplementsList();
                                                if (newImplementsList != null) {
                                                    PsiElement lBrace = psiClass.getLBrace();
                                                    if (lBrace != null) {
                                                        psiClass.addBefore(newImplementsList, lBrace);
                                                    }
                                                }
                                            } else {
                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                PsiJavaCodeReferenceElement referenceElement = factory.createReferenceElementByFQClassName(
                                                        "java.io.Serializable", psiClass.getResolveScope());
                                                implementsList.add(referenceElement);
                                            }
                                            
                                            PsiFile file = context1.getFile();
                                    if (file instanceof PsiJavaFile javaFile) {
                                                PsiImportList importList = javaFile.getImportList();
                                                if (importList != null && !SerialVersionUIDGenerator.hasImport(importList, "java.io.Serializable")) {
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    PsiClass serializableClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                                                            "java.io.Serializable",
                                                            psiClass.getResolveScope()
                                                    );
                                                    if (serializableClass != null) {
                                                        importList.add(factory.createImportStatement(serializableClass));
                                                    }
                                                }
                                            }
                                            
                                            PsiDocumentManager.getInstance(psiClass.getProject()).commitDocument(context1.getDocument());
                                            
                                            ApplicationManager.getApplication().invokeLater(() -> {
                                                if (psiClass.isValid() && !psiClass.getProject().isDisposed()) {
                                                    String serialVersionUIDCode = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                                                    PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);
                                                    
                                            com.intellij.openapi.command.WriteCommandAction
                                                .runWriteCommandAction(psiClass.getProject(),
                                                    "Generate SerialVersionUID", null, () -> {
                                                        PsiElement[] children = psiClass.getChildren();
                                                        if (children.length > 0) {
                                                            psiClass.addBefore(field, children[0]);
                                                        } else {
                                                            psiClass.add(field);
                                                        }
                                                        
                                                        boolean useSerialAnnotation = SerialVersionUIDGenerator.shouldUseSerialAnnotation(psiClass.getProject());
                                                        if (useSerialAnnotation) {
                                                            PsiFile psiFile = psiClass.getContainingFile();
                                                    if (psiFile instanceof PsiJavaFile javaFile) {
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
                                                        
                                                        com.intellij.notification.NotificationGroupManager.getInstance()
                                                            .getNotificationGroup("SerialVersionUID Generator")
                                                            .createNotification(
                                                        "SerialVersionUID generator",
                                                                    "已为类 '" + psiClass.getName() + "' 自动生成serialVersionUID字段",
                                                                    com.intellij.notification.NotificationType.INFORMATION)
                                                            .notify(psiClass.getProject());
                                                    });
                                                }
                                            });
                                        }));
                            }
                        }
                    }
                }
        );
    }

    private PsiClass findContainingClass(PsiElement element) {
        while (element != null && !(element instanceof PsiClass)) {
            element = element.getParent();
        }
        return (PsiClass) element;
    }

    private boolean isInImplementsList(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiReferenceList referenceList) {
                return referenceList.getRole() == PsiReferenceList.Role.IMPLEMENTS_LIST;
            }
            parent = parent.getParent();
        }
        return false;
    }
}
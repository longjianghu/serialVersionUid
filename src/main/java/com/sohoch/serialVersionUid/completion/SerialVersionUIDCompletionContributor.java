package com.sohoch.serialVersionUid.completion;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.sohoch.serialVersionUid.util.SerialVersionUIDGenerator;

public class SerialVersionUIDCompletionContributor extends CompletionContributor {
    public SerialVersionUIDCompletionContributor() {
        // 为实现Serializable接口的类提供serialVersionUID字段的自动完成
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withParent(PsiClass.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                @NotNull ProcessingContext context,
                                                @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiClass psiClass = (PsiClass) position.getParent();

                        if (SerialVersionUIDGenerator.isSerializable(psiClass)) {
                            long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                            final String fieldText;
                            final String tempFieldText = String.format("private static final long serialVersionUID = %dL;", serialVersionUID);
                            
                            Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile().getVirtualFile(), parameters.getOriginalFile().getProject());
                            if (SerialVersionUIDGenerator.isJavaVersionAtLeast14(module)) {
                                fieldText = "@Serial\n" + tempFieldText;
                                
                                // 确保导入java.io.Serial类
                                PsiClass serialClass = JavaPsiFacade.getInstance(psiClass.getProject())
                                    .findClass("java.io.Serial", GlobalSearchScope.allScope(psiClass.getProject()));
                                
                                if (serialClass != null && psiClass.getContainingFile() instanceof PsiJavaFile) {
                                    PsiJavaFile javaFile = (PsiJavaFile) psiClass.getContainingFile();
                                    WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> {
                                        javaFile.importClass(serialClass);
                                    });
                                }
                            } else {
                                fieldText = tempFieldText;
                            }

                            result.addElement(LookupElementBuilder.create("serialVersionUID")
                                    .withPresentableText("serialVersionUID (generate field)")
                                    .withInsertHandler((context1, item) -> {
                                        int offset = context1.getEditor().getCaretModel().getOffset();
                                        context1.getDocument().insertString(offset, fieldText);
                                    }));
                        }
                    }
                });

        // 为接口列表中的Serializable提供自动完成
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().inside(PsiReferenceList.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                @NotNull ProcessingContext context,
                                                @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        // 修复类型转换错误，正确获取PsiClass对象
                        PsiElement parent = position.getParent();
                        if (parent instanceof PsiReferenceList) {
                            PsiReferenceList referenceList = (PsiReferenceList) parent;
                            PsiElement parentOfReferenceList = referenceList.getParent();
                            if (parentOfReferenceList instanceof PsiClass) {
                                PsiClass psiClass = (PsiClass) parentOfReferenceList;

                                result.addElement(LookupElementBuilder.create("Serializable (with serialVersionUID)")
                                        .withPresentableText("Serializable (with serialVersionUID)")
                                        .withInsertHandler((context1, item) -> {
                                            long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                            final String fieldText;
                                            final String tempFieldText = String.format("\n    private static final long serialVersionUID = %dL;", serialVersionUID);
                                            
                                            Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile().getVirtualFile(), parameters.getOriginalFile().getProject());
                                            if (SerialVersionUIDGenerator.isJavaVersionAtLeast14(module)) {
                                                fieldText = "\n    @Serial" + tempFieldText;
                                                
                                                // 确保导入java.io.Serial类
                                                PsiClass serialClass = JavaPsiFacade.getInstance(psiClass.getProject())
                                                    .findClass("java.io.Serial", GlobalSearchScope.allScope(psiClass.getProject()));
                                                
                                                if (serialClass != null && psiClass.getContainingFile() instanceof PsiJavaFile) {
                                                    PsiJavaFile javaFile = (PsiJavaFile) psiClass.getContainingFile();
                                                    context1.commitDocument();
                                                    WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> {
                                                        javaFile.importClass(serialClass);
                                                    });
                                                }
                                            } else {
                                                fieldText = tempFieldText;
                                            }

                                            int offset = context1.getEditor().getCaretModel().getOffset();
                                            context1.getDocument().insertString(offset, "Serializable" + fieldText);
                                        }));
                            }
                        }
                    }
                });
    }
}
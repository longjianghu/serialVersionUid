package com.sohoch.serialVersionUid.completion;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceList;
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
                            
                            if (SerialVersionUIDGenerator.isJavaVersionAtLeast14(ModuleUtil.findModuleForFile(parameters.getOriginalFile().getVirtualFile(), parameters.getOriginalFile().getProject()))) {
                                fieldText = "@Serial\n" + tempFieldText;
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
                        PsiClass psiClass = (PsiClass) position.getParent().getParent();

                        if (psiClass != null && position.getParent() instanceof PsiReferenceList) {
                            result.addElement(LookupElementBuilder.create("Serializable (with serialVersionUID)")
                                    .withPresentableText("Serializable (with serialVersionUID)")
                                    .withInsertHandler((context1, item) -> {
                                        long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                        final String fieldText;
                                        final String tempFieldText = String.format("\n    private static final long serialVersionUID = %dL;", serialVersionUID);
                                        
                                        if (SerialVersionUIDGenerator.isJavaVersionAtLeast14(ModuleUtil.findModuleForFile(parameters.getOriginalFile().getVirtualFile(), parameters.getOriginalFile().getProject()))) {
                                            fieldText = "\n    @Serial" + tempFieldText;
                                        } else {
                                            fieldText = tempFieldText;
                                        }

                                        int offset = context1.getEditor().getCaretModel().getOffset();
                                        context1.getDocument().insertString(offset, "Serializable" + fieldText);
                                    }));
                        }
                    }
                });
    }
}
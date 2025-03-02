package com.sohocn.serialVersionUID;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 当用户实现Serializable接口时提供serialVersionUID的代码完成
 */
public class SerialVersionUIDCompletionContributor extends CompletionContributor {

    public SerialVersionUIDCompletionContributor() {
        // 在类体内部提供代码完成
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withSuperParent(2, PsiClass.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                 @NotNull ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiClass psiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
                        
                        if (psiClass == null) {
                            return;
                        }
                        
                        // 检查类是否实现了Serializable接口
                        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
                            return;
                        }
                        
                        // 检查类是否已经有serialVersionUID字段
                        if (SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass) != null) {
                            return;
                        }
                        
                        // 生成serialVersionUID值
                        long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                        
                        // 添加代码完成项
                        result.addElement(LookupElementBuilder.create("serialVersionUID")
                                .withPresentableText("serialVersionUID")
                                .withTypeText("long")
                                .withTailText(" = " + serialVersionUID + "L", true)
                                .withInsertHandler((context1, item) -> {
                                    // 插入完整的serialVersionUID字段
                                    context1.getDocument().insertString(
                                            context1.getStartOffset(),
                                            "@Serial\nprivate static final long serialVersionUID = " + serialVersionUID + "L;"
                                    );
                                    
                                    // 删除自动插入的部分
                                    context1.getDocument().deleteString(
                                            context1.getStartOffset() + "@Serial\nprivate static final long ".length(),
                                            context1.getTailOffset()
                                    );
                                    
                                    // 添加Serial注解的导入
                                    PsiDocumentManager.getInstance(context1.getProject()).commitDocument(context1.getDocument());
                                    PsiFile file = context1.getFile();
                                    if (file instanceof PsiJavaFile) {
                                        PsiImportList importList = ((PsiJavaFile) file).getImportList();
                                        if (importList != null) {
                                            boolean hasSerialImport = false;
                                            for (PsiImportStatement importStatement : importList.getImportStatements()) {
                                                if ("java.io.Serial".equals(importStatement.getQualifiedName())) {
                                                    hasSerialImport = true;
                                                    break;
                                                }
                                            }
                                            
                                            if (!hasSerialImport) {
                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(context1.getProject());
                                                PsiClass serialClass = JavaPsiFacade.getInstance(context1.getProject())
                                                        .findClass("java.io.Serial", psiClass.getResolveScope());
                                                if (serialClass != null) {
                                                    PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                                                    importList.add(importStatement);
                                                }
                                            }
                                        }
                                    }
                                }));
                    }
                });
    }
} 
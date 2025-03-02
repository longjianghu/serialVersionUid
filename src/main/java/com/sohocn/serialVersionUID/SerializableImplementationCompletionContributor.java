package com.sohocn.serialVersionUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * 在用户实现Serializable接口时提示生成serialVersionUID
 */
public class SerializableImplementationCompletionContributor extends CompletionContributor {

    public SerializableImplementationCompletionContributor() {
        // 在实现接口列表中提供代码完成
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withParent(PsiJavaCodeReferenceElement.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                 @NotNull ProcessingContext context,
                                                 @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiElement parent = position.getParent();
                        
                    if (!(parent instanceof PsiJavaCodeReferenceElement ref)) {
                            return;
                        }

                        PsiElement refParent = ref.getParent();
                        
                        // 检查是否在实现接口列表中
                    if (!(refParent instanceof PsiReferenceList refList)) {
                            return;
                        }

                        if (refList.getRole() != PsiReferenceList.Role.IMPLEMENTS_LIST) {
                            return;
                        }
                        
                        // 检查是否正在输入Serializable
                        String text = position.getText();
                        if (!text.contains("Serializable") && !text.contains("serializable")) {
                            return;
                        }
                        
                        // 添加Serializable接口的代码完成项
                        result.addElement(LookupElementBuilder.create("Serializable")
                                .withPresentableText("Serializable")
                                .withTypeText("java.io")
                                .withTailText(" (with serialVersionUID)", true)
                                .withInsertHandler((context1, item) -> {
                                    // 获取当前类
                                    PsiClass psiClass = PsiTreeUtil.getParentOfType(context1.getFile().findElementAt(context1.getStartOffset()), PsiClass.class);
                                    if (psiClass == null) {
                                        return;
                                    }
                                    
                                    // 检查是否已经有serialVersionUID字段
                                    if (SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass) != null) {
                                        return;
                                    }
                                    
                                    // 添加导入
                                    Project project = context1.getProject();
                                    PsiFile file = context1.getFile();
                            if (file instanceof PsiJavaFile javaFile) {
                                        PsiImportList importList = javaFile.getImportList();
                                        if (importList != null) {
                                            boolean hasSerializableImport = false;
                                            for (PsiImportStatement importStatement : importList.getImportStatements()) {
                                                if ("java.io.Serializable".equals(importStatement.getQualifiedName())) {
                                                    hasSerializableImport = true;
                                                    break;
                                                }
                                            }
                                            
                                            if (!hasSerializableImport) {
                                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                                                    PsiClass serializableClass = JavaPsiFacade.getInstance(project)
                                                            .findClass("java.io.Serializable", psiClass.getResolveScope());
                                                    if (serializableClass != null) {
                                                        PsiImportStatement importStatement = factory.createImportStatement(serializableClass);
                                                        importList.add(importStatement);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    
                                    // 生成serialVersionUID值
                                    long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
                                    
                                    // 使用invokeLater延迟执行PSI修改操作
                                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                        // 创建serialVersionUID字段
                                        WriteCommandAction.runWriteCommandAction(project, () -> {
                                            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                                            String fieldText = SerialVersionUIDGenerator.createSerialVersionUIDFieldText(serialVersionUID, project);
                                            PsiField field = factory.createFieldFromText(fieldText, psiClass);
                                            
                                            // 添加字段到类中
                                    PsiElement anchor = SerialVersionUIDUtils.findAnchorForField(psiClass);
                                            if (anchor != null) {
                                                psiClass.addBefore(field, anchor);
                                            } else {
                                                psiClass.add(field);
                                            }
                                            
                                            // 添加Serial注解的导入
                                            if (SerialVersionUIDGenerator.shouldUseSerialAnnotation(project)) {
                                        SerialVersionUIDUtils.addSerialAnnotationIfNeeded(psiClass, project);
                                            }
                                            
                                            // 优化导入
                                    SerialVersionUIDUtils.optimizeImports(psiClass, project);
                                        });
                                    });
                                }));
                    }
                });
    }
} 
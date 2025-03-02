package com.sohocn.serialVersionUID;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

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
                        
                        if (!(parent instanceof PsiJavaCodeReferenceElement)) {
                            return;
                        }
                        
                        PsiJavaCodeReferenceElement ref = (PsiJavaCodeReferenceElement) parent;
                        PsiElement refParent = ref.getParent();
                        
                        // 检查是否在实现接口列表中
                        if (!(refParent instanceof PsiReferenceList)) {
                            return;
                        }
                        
                        PsiReferenceList refList = (PsiReferenceList) refParent;
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
                                    if (file instanceof PsiJavaFile) {
                                        PsiJavaFile javaFile = (PsiJavaFile) file;
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
                                    
                                    // 创建serialVersionUID字段
                                    WriteCommandAction.runWriteCommandAction(project, () -> {
                                        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                                        String fieldText = SerialVersionUIDGenerator.createSerialVersionUIDFieldText(serialVersionUID, project);
                                        PsiField field = factory.createFieldFromText(fieldText, psiClass);
                                        
                                        // 添加字段到类中
                                        PsiElement anchor = null;
                                        PsiField[] fields = psiClass.getFields();
                                        if (fields.length > 0) {
                                            anchor = fields[0];
                                        } else {
                                            PsiMethod[] methods = psiClass.getMethods();
                                            if (methods.length > 0) {
                                                anchor = methods[0];
                                            } else {
                                                PsiClass[] innerClasses = psiClass.getInnerClasses();
                                                if (innerClasses.length > 0) {
                                                    anchor = innerClasses[0];
                                                }
                                            }
                                        }
                                        
                                        if (anchor != null) {
                                            psiClass.addBefore(field, anchor);
                                        } else {
                                            psiClass.add(field);
                                        }
                                        
                                        // 添加Serial注解的导入
                                        if (SerialVersionUIDGenerator.shouldUseSerialAnnotation(project)) {
                                            PsiFile file1 = psiClass.getContainingFile();
                                            if (file1 instanceof PsiJavaFile) {
                                                PsiJavaFile javaFile = (PsiJavaFile) file1;
                                                PsiImportList importList = javaFile.getImportList();
                                                
                                                if (importList != null) {
                                                    boolean hasSerialImport = false;
                                                    for (PsiImportStatement importStatement : importList.getImportStatements()) {
                                                        if ("java.io.Serial".equals(importStatement.getQualifiedName())) {
                                                            hasSerialImport = true;
                                                            break;
                                                        }
                                                    }
                                                    
                                                    if (!hasSerialImport) {
                                                        PsiClass serialClass = JavaPsiFacade.getInstance(project)
                                                                .findClass("java.io.Serial", psiClass.getResolveScope());
                                                        if (serialClass != null) {
                                                            PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                                                            importList.add(importStatement);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // 优化导入
                                        JavaCodeStyleManager.getInstance(project).optimizeImports(psiClass.getContainingFile());
                                    });
                                }));
                    }
                });
    }
} 
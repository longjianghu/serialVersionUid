package com.sohocn.serialVersionUID;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * 为实现了Serializable接口的类添加serialVersionUID字段的意图动作
 */
public class AddSerialVersionUIDIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return "Generate serialVersionUID";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "SerialVersionUID Generator";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 检查当前元素是否在一个类中
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return false;
        }

        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return false;
        }

        // 检查类是否已经有serialVersionUID字段
        PsiField serialVersionUIDField = SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass);
        
        // 如果已经有字段，我们仍然提供更新选项
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField existingField = SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass);
        
        long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);
        
        if (existingField != null) {
            // 更新现有字段的值
            PsiExpression initializer = factory.createExpressionFromText(serialVersionUID + "L", psiClass);
            existingField.getInitializer().replace(initializer);
        } else {
            // 创建新的serialVersionUID字段
            String fieldText = SerialVersionUIDGenerator.createSerialVersionUIDFieldText(serialVersionUID, project);
            PsiField field = factory.createFieldFromText(fieldText, psiClass);
            
            // 添加字段到类中
            PsiElement anchor = findAnchorForField(psiClass);
            if (anchor != null) {
                psiClass.addBefore(field, anchor);
            } else {
                psiClass.add(field);
            }
            
            // 检查是否需要导入Serial注解
            if (SerialVersionUIDGenerator.shouldUseSerialAnnotation(project)) {
                addSerialAnnotationIfNeeded(psiClass, project);
            }
            
            // 优化导入
            JavaCodeStyleManager.getInstance(project).optimizeImports(psiClass.getContainingFile());
        }
    }

    /**
     * 查找添加字段的位置
     *
     * @param psiClass 要添加字段的类
     * @return 添加字段的锚点元素
     */
    private PsiElement findAnchorForField(PsiClass psiClass) {
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            return fields[0];
        }
        
        PsiMethod[] methods = psiClass.getMethods();
        if (methods.length > 0) {
            return methods[0];
        }
        
        PsiClass[] innerClasses = psiClass.getInnerClasses();
        if (innerClasses.length > 0) {
            return innerClasses[0];
        }
        
        return null;
    }

    /**
     * 如果需要，添加Serial注解的导入
     *
     * @param psiClass 要添加导入的类
     * @param project 当前项目
     */
    private void addSerialAnnotationIfNeeded(PsiClass psiClass, Project project) {
        PsiFile file = psiClass.getContainingFile();
        if (file instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
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
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    PsiClass serialClass = JavaPsiFacade.getInstance(project).findClass("java.io.Serial", psiClass.getResolveScope());
                    if (serialClass != null) {
                        PsiImportStatement importStatement = factory.createImportStatement(serialClass);
                        importList.add(importStatement);
                    }
                }
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
} 
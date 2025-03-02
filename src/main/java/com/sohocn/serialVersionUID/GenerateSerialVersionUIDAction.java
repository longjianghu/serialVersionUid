package com.sohocn.serialVersionUID;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 在右键菜单中添加生成serialVersionUID的选项
 */
public class GenerateSerialVersionUIDAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // 获取当前光标位置的元素
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return;
        }

        // 查找包含当前元素的类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return;
        }

        // 检查类是否实现了Serializable接口
        if (!SerialVersionUIDGenerator.isSerializable(psiClass)) {
            return;
        }

        // 在写入命令中执行添加serialVersionUID的操作
        WriteCommandAction.runWriteCommandAction(project, () -> {
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
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 只有在编辑Java文件且光标在类内部时才启用此操作
        boolean enabled = false;
        if (project != null && editor != null && psiFile instanceof PsiJavaFile) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = psiFile.findElementAt(offset);
            if (element != null) {
                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                if (psiClass != null && SerialVersionUIDGenerator.isSerializable(psiClass)) {
                    enabled = true;
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(enabled);
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
} 
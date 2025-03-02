package com.sohocn.serialVersionUID;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.annotations.NotNull;

/**
 * 项目启动时注册监听器，当用户实现Serializable接口时自动添加serialVersionUID字段
 */
public class SerialVersionUIDPostStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        // 注册PsiTreeChangeListener，监听Java文件的变化
        PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
            @Override
            public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
                processEvent(event);
            }

            @Override
            public void childAdded(@NotNull PsiTreeChangeEvent event) {
                processEvent(event);
            }

            private void processEvent(PsiTreeChangeEvent event) {
                PsiElement element = event.getParent();
                if (element instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) element;
                    // 检查类是否实现了Serializable接口
                    if (SerialVersionUIDGenerator.isSerializable(psiClass) && 
                        SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass) == null) {
                        // 在写入命令中执行添加serialVersionUID的操作
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            addSerialVersionUID(psiClass, project);
                        });
                    }
                }
            }
        }, project);
    }

    /**
     * 为类添加serialVersionUID字段
     *
     * @param psiClass 要添加字段的类
     * @param project 当前项目
     */
    private void addSerialVersionUID(PsiClass psiClass, Project project) {
        // 生成serialVersionUID值
        long serialVersionUID = SerialVersionUIDGenerator.generateSerialVersionUID(psiClass);

        // 创建serialVersionUID字段
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        String fieldText = SerialVersionUIDGenerator.createSerialVersionUIDFieldText(serialVersionUID, project);
        PsiField field = factory.createFieldFromText(fieldText, psiClass);

        // 添加字段到类中
        PsiElement anchor = findAnchorForField(psiClass);
        if (anchor != null) {
            psiClass.addBefore(field, anchor);
        } else {
            psiClass.add(field);
        }

        // 添加Serial注解的导入
        if (SerialVersionUIDGenerator.shouldUseSerialAnnotation(project)) {
            addSerialAnnotationIfNeeded(psiClass, project);
        }

        // 优化导入
        JavaCodeStyleManager.getInstance(project).optimizeImports(psiClass.getContainingFile());
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
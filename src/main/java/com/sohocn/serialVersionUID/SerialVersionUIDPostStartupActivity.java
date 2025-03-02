package com.sohocn.serialVersionUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.psi.*;

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
                if (element instanceof PsiClass psiClass) {
                    // 检查类是否实现了Serializable接口
                    if (SerialVersionUIDGenerator.isSerializable(psiClass) && 
                        SerialVersionUIDGenerator.findSerialVersionUIDField(psiClass) == null) {
                        // 使用invokeLater延迟执行PSI修改操作，避免在事件处理过程中直接修改PSI
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                            // 在写入命令中执行添加serialVersionUID的操作
                            WriteCommandAction
                                .runWriteCommandAction(project, () -> addSerialVersionUID(psiClass, project));
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
    }

}
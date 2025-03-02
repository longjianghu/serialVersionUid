package com.sohocn.serialVersionUID;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.*;

/**
 * 用于生成serialVersionUID的工具类
 */
public class SerialVersionUIDGenerator {
    private static final Logger LOG = Logger.getInstance(SerialVersionUIDGenerator.class);

    /**
     * 生成serialVersionUID
     *
     * @param psiClass
     *            需要生成serialVersionUID的类
     * @return 生成的serialVersionUID值
     */
    public static long generateSerialVersionUID(PsiClass psiClass) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

            // 写入类名
            dataOutputStream.writeUTF(psiClass.getQualifiedName());

            // 写入类修饰符
            dataOutputStream
                .writeInt(psiClass.getModifierList() != null ? psiClass.getModifierList().getTextLength() : 0);

            // 写入接口
            PsiClass[] interfaces = psiClass.getInterfaces();
            for (PsiClass anInterface : interfaces) {
                dataOutputStream.writeUTF(anInterface.getQualifiedName());
            }

            // 写入字段
            PsiField[] fields = psiClass.getFields();
            for (PsiField field : fields) {
                if (!field.hasModifierProperty(PsiModifier.STATIC)
                    && !field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                    dataOutputStream.writeUTF(field.getName());
                    dataOutputStream.writeUTF(field.getType().getCanonicalText());
                }
            }

            // 写入方法
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                if (!method.isConstructor() && method.hasModifierProperty(PsiModifier.PUBLIC)) {
                    dataOutputStream.writeUTF(method.getName());
                    dataOutputStream.writeUTF(method.getReturnType().getCanonicalText());

                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (PsiParameter parameter : parameters) {
                        dataOutputStream.writeUTF(parameter.getType().getCanonicalText());
                    }
                }
            }

            dataOutputStream.flush();
            byte[] bytes = byteArrayOutputStream.toByteArray();

            // 使用SHA-1计算哈希值
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1Bytes = md.digest(bytes);

            // 将哈希值转换为long
            long serialVersionUID = 0;
            for (int i = Math.min(sha1Bytes.length, 8) - 1; i >= 0; i--) {
                serialVersionUID = (serialVersionUID << 8) | (sha1Bytes[i] & 0xFF);
            }

            return serialVersionUID;
        } catch (IOException | NoSuchAlgorithmException e) {
            LOG.error("生成serialVersionUID时出错", e);
            // 如果出错，返回一个基于当前时间的值
            return System.currentTimeMillis();
        }
    }

    /**
     * 检查类是否实现了Serializable接口
     *
     * @param psiClass
     *            要检查的类
     * @return 如果类实现了Serializable接口，则返回true
     */
    public static boolean isSerializable(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        PsiClass[] interfaces = psiClass.getInterfaces();
        for (PsiClass anInterface : interfaces) {
            if ("java.io.Serializable".equals(anInterface.getQualifiedName())) {
                return true;
            }
        }

        // 检查父类是否实现了Serializable
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            return isSerializable(superClass);
        }

        return false;
    }

    /**
     * 检查类是否已经有serialVersionUID字段
     *
     * @param psiClass
     *            要检查的类
     * @return 如果类已经有serialVersionUID字段，则返回该字段；否则返回null
     */
    public static PsiField findSerialVersionUIDField(PsiClass psiClass) {
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            if ("serialVersionUID".equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * 检查项目的Java SDK版本是否大于等于指定版本
     *
     * @param project
     *            当前项目
     * @param targetVersion
     *            目标版本
     * @return 如果项目的Java SDK版本大于等于目标版本，则返回true
     */
    public static boolean isJavaVersionAtLeast(Project project, JavaSdkVersion targetVersion) {
        if (project == null) {
            return false;
        }

        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk != null && projectSdk.getSdkType() instanceof JavaSdk) {
            JavaSdkVersion version = JavaSdk.getInstance().getVersion(projectSdk);
            return version != null && version.isAtLeast(targetVersion);
        }

        return false;
    }

    /**
     * 检查是否应该使用@Serial注解
     * 
     * @param project
     *            当前项目
     * @return 如果应该使用@Serial注解，则返回true
     */
    public static boolean shouldUseSerialAnnotation(Project project) {
        return isJavaVersionAtLeast(project, JavaSdkVersion.JDK_14);
    }

    /**
     * 创建serialVersionUID字段的文本
     *
     * @param serialVersionUID
     *            生成的serialVersionUID值
     * @param project
     *            当前项目
     * @return 字段的文本表示
     */
    public static String createSerialVersionUIDFieldText(long serialVersionUID, Project project) {
        if (shouldUseSerialAnnotation(project)) {
            return "@Serial\nprivate static final long serialVersionUID = " + serialVersionUID + "L;";
        } else {
            return "private static final long serialVersionUID = " + serialVersionUID + "L;";
        }
    }
}
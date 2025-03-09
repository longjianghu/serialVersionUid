package com.sohocn.serialVersionUid.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生成serialVersionUID的工具类
 */
public class SerialVersionUIDGenerator {

    /**
     * 检查类是否实现了Serializable接口
     *
     * @param psiClass 要检查的类
     * @return 如果类实现了Serializable接口则返回true，否则返回false
     */
    public static boolean isSerializable(PsiClass psiClass) {
        if (psiClass == null || psiClass.isInterface() || psiClass.isEnum() || psiClass.isAnnotationType()) {
            return false;
        }

        PsiClassType[] implementsListTypes = psiClass.getImplementsListTypes();
        for (PsiClassType type : implementsListTypes) {
            if ("java.io.Serializable".equals(type.getCanonicalText())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查类是否已经有serialVersionUID字段
     *
     * @param psiClass 要检查的类
     * @return 如果类已经有serialVersionUID字段则返回true，否则返回false
     */
    public static boolean hasSerialVersionUID(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }

        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            if ("serialVersionUID".equals(field.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成serialVersionUID字段的代码
     *
     * @param psiClass 要生成serialVersionUID的类
     * @return 生成的serialVersionUID字段代码
     */
    public static String generateSerialVersionUID(PsiClass psiClass) {
        long serialVersionUID = computeSerialVersionUID(psiClass);
        boolean useSerialAnnotation = shouldUseSerialAnnotation(psiClass.getProject());

        StringBuilder builder = new StringBuilder();
        if (useSerialAnnotation) {
            builder.append("@Serial\n");
        }
        builder.append("private static final long serialVersionUID = ").append(serialVersionUID).append("L;");

        return builder.toString();
    }

    /**
     * 计算类的serialVersionUID值
     *
     * @param psiClass 要计算serialVersionUID的类
     * @return 计算得到的serialVersionUID值
     */
    private static long computeSerialVersionUID(PsiClass psiClass) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // 写入类名
            dos.writeUTF(psiClass.getQualifiedName());

            // 写入类修饰符
            dos.writeInt(getClassModifiers(psiClass));

            // 写入接口名称（按字母顺序排序）
            PsiClassType[] interfaces = psiClass.getImplementsListTypes();
            List<String> interfaceNames = new ArrayList<>();
            for (PsiClassType anInterface : interfaces) {
                interfaceNames.add(anInterface.getCanonicalText());
            }
            Collections.sort(interfaceNames);
            for (String interfaceName : interfaceNames) {
                dos.writeUTF(interfaceName);
            }

            // 写入字段信息（按字母顺序排序）
            List<PsiField> fields = new ArrayList<>();
            for (PsiField field : psiClass.getFields()) {
                if (!field.hasModifierProperty(PsiModifier.STATIC) && !field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                    fields.add(field);
                }
            }
            fields.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));
            for (PsiField field : fields) {
                dos.writeUTF(field.getName());
                dos.writeInt(getFieldModifiers(field));
                dos.writeUTF(field.getType().getCanonicalText());
            }

            // 写入方法信息（按字母顺序排序）
            List<PsiMethod> methods = new ArrayList<>();
            for (PsiMethod method : psiClass.getMethods()) {
                if (!method.isConstructor() && !method.hasModifierProperty(PsiModifier.PRIVATE) && 
                    !method.hasModifierProperty(PsiModifier.STATIC)) {
                    methods.add(method);
                }
            }
            methods.sort((m1, m2) -> m1.getName().compareTo(m2.getName()));
            for (PsiMethod method : methods) {
                dos.writeUTF(method.getName());
                dos.writeInt(getMethodModifiers(method));
                dos.writeUTF(method.getReturnType().getCanonicalText());
                for (PsiParameter parameter : method.getParameterList().getParameters()) {
                    dos.writeUTF(parameter.getType().getCanonicalText());
                }
            }

            dos.flush();
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(baos.toByteArray());

            // 取前8个字节转换为long
            long hash = 0;
            for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            // 如果计算失败，返回一个默认值
            return 1L;
        }
    }

    /**
     * 获取类的修饰符
     */
    private static int getClassModifiers(PsiClass psiClass) {
        int modifiers = 0;
        if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) modifiers |= 1;
        if (psiClass.hasModifierProperty(PsiModifier.FINAL)) modifiers |= 2;
        if (psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) modifiers |= 4;
        if (psiClass.isInterface()) modifiers |= 8;
        return modifiers;
    }

    /**
     * 获取字段的修饰符
     */
    private static int getFieldModifiers(PsiField field) {
        int modifiers = 0;
        if (field.hasModifierProperty(PsiModifier.PUBLIC)) modifiers |= 1;
        if (field.hasModifierProperty(PsiModifier.PRIVATE)) modifiers |= 2;
        if (field.hasModifierProperty(PsiModifier.PROTECTED)) modifiers |= 4;
        if (field.hasModifierProperty(PsiModifier.STATIC)) modifiers |= 8;
        if (field.hasModifierProperty(PsiModifier.FINAL)) modifiers |= 16;
        if (field.hasModifierProperty(PsiModifier.VOLATILE)) modifiers |= 32;
        if (field.hasModifierProperty(PsiModifier.TRANSIENT)) modifiers |= 64;
        return modifiers;
    }

    /**
     * 获取方法的修饰符
     */
    private static int getMethodModifiers(PsiMethod method) {
        int modifiers = 0;
        if (method.hasModifierProperty(PsiModifier.PUBLIC)) modifiers |= 1;
        if (method.hasModifierProperty(PsiModifier.PRIVATE)) modifiers |= 2;
        if (method.hasModifierProperty(PsiModifier.PROTECTED)) modifiers |= 4;
        if (method.hasModifierProperty(PsiModifier.STATIC)) modifiers |= 8;
        if (method.hasModifierProperty(PsiModifier.FINAL)) modifiers |= 16;
        if (method.hasModifierProperty(PsiModifier.SYNCHRONIZED)) modifiers |= 32;
        if (method.hasModifierProperty(PsiModifier.NATIVE)) modifiers |= 64;
        if (method.hasModifierProperty(PsiModifier.ABSTRACT)) modifiers |= 128;
        return modifiers;
    }

    /**
     * 判断是否应该使用@Serial注解（Java 14+）
     *
     * @param project 当前项目
     * @return 如果应该使用@Serial注解则返回true，否则返回false
     */
    public static boolean shouldUseSerialAnnotation(Project project) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        try {
            // 尝试创建一个@Serial注解，如果成功则表示当前Java版本支持该注解
            factory.createAnnotationFromText("@java.io.Serial", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取需要导入的包列表
     *
     * @param useSerialAnnotation 是否使用@Serial注解
     * @return 需要导入的包列表
     */
    public static List<String> getImportsToAdd(boolean useSerialAnnotation) {
        if (useSerialAnnotation) {
            return Collections.singletonList("java.io.Serial");
        }
        return Collections.emptyList();
    }
    
    /**
     * 检查导入语句是否已存在
     * 
     * @param importList 导入列表
     * @param qualifiedName 完全限定名
     * @return 如果导入已存在则返回true，否则返回false
     */
    public static boolean hasImport(PsiImportList importList, String qualifiedName) {
        if (importList == null) {
            return false;
        }
        
        PsiImportStatement[] statements = importList.getImportStatements();
        for (PsiImportStatement statement : statements) {
            if (qualifiedName.equals(statement.getQualifiedName())) {
                return true;
            }
        }
        
        return false;
    }
}
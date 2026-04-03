package com.sohocn.serialVersionUid.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * The type Serial version uid generator.
 *
 * @author longjianghu
 */
public class SerialVersionUIDGenerator {
    /**
     * Add serializable interface.
     *
     * @param psiClass
     *            the psi class
     * @param project
     *            the project
     */
    public static void addSerializableInterface(PsiClass psiClass, Project project) {
        if (isSerializable(psiClass)) {
            return;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile instanceof PsiJavaFile javaFile) {
            PsiImportList importList = javaFile.getImportList();
            if (importList != null && !hasImport(importList, "java.io.Serializable")) {
                PsiClass serializableClass = JavaPsiFacade
                    .getInstance(project)
                    .findClass("java.io.Serializable", GlobalSearchScope.allScope(project));
                if (serializableClass != null) {
                    PsiImportStatement importStatement = factory.createImportStatement(serializableClass);
                    importList.add(importStatement);
                }
            }
        }

        PsiJavaCodeReferenceElement serializableRef = factory.createReferenceFromText("Serializable", psiClass);

        PsiReferenceList implementsList = psiClass.getImplementsList();
        if (implementsList != null) {
            implementsList.add(serializableRef);
        } else {
            PsiReferenceList newImplementsList =
                factory.createReferenceList(new PsiJavaCodeReferenceElement[] {serializableRef});
            psiClass.addAfter(newImplementsList, psiClass.getNameIdentifier());
        }
    }

    /**
     * Generate and add serial version uid.
     *
     * @param psiClass
     *            the psi class
     * @param project
     *            the project
     */
    public static void generateAndAddSerialVersionUID(PsiClass psiClass, Project project) {
        if (psiClass == null || project == null) {
            return;
        }

        addSerializableInterface(psiClass, project);

        String serialVersionUIDCode = generateSerialVersionUID(psiClass);
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField field = factory.createFieldFromText(serialVersionUIDCode, psiClass);

        if (hasSerialVersionUID(psiClass)) {
            PsiField[] fields = psiClass.getFields();
            for (PsiField existingField : fields) {
                if ("serialVersionUID".equals(existingField.getName())) {
                    existingField.replace(field);
                    break;
                }
            }
        } else {
            PsiElement anchor = findAnchor(psiClass);
            if (anchor != null) {
                psiClass.addBefore(field, anchor);
            } else {
                psiClass.add(field);
            }
        }

        boolean useSerialAnnotation = shouldUseSerialAnnotation(project);
        if (useSerialAnnotation) {
            PsiFile containingFile = psiClass.getContainingFile();
            if (containingFile instanceof PsiJavaFile javaFile) {
                PsiImportList importList = javaFile.getImportList();
                if (importList != null && !hasImport(importList, "java.io.Serial")) {
                    PsiClass serialClass =
                        JavaPsiFacade.getInstance(project).findClass("java.io.Serial", psiClass.getResolveScope());
                    if (serialClass != null) {
                        importList.add(factory.createImportStatement(serialClass));
                    }
                }
            }
        }
    }

    /**
     * Generate serial version uid string.
     *
     * @param psiClass
     *            the psi class
     * @return the string
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
     * Has import boolean.
     *
     * @param importList
     *            the import list
     * @param qualifiedName
     *            the qualified name
     * @return the boolean
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

    /**
     * Has serial version uid boolean.
     *
     * @param psiClass
     *            the psi class
     * @return the boolean
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
     * Is serializable boolean.
     *
     * @param psiClass
     *            the psi class
     * @return the boolean
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
     * Should use serial annotation boolean.
     *
     * @param project
     *            the project
     * @return the boolean
     */
    public static boolean shouldUseSerialAnnotation(Project project) {
        if (project == null || project.isDisposed()) {
            return false;
        }

        return JavaPsiFacade
            .getInstance(project)
            .findClass("java.io.Serial", GlobalSearchScope.allScope(project)) != null;
    }

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
            fields.sort(Comparator.comparing(PsiField::getName));
            for (PsiField field : fields) {
                dos.writeUTF(field.getName());
                dos.writeInt(getFieldModifiers(field));
                dos.writeUTF(field.getType().getCanonicalText());
            }

            // 写入方法信息（按字母顺序排序）
            List<PsiMethod> methods = new ArrayList<>();
            for (PsiMethod method : psiClass.getMethods()) {
                if (!method.isConstructor() && !method.hasModifierProperty(PsiModifier.PRIVATE)
                    &&
                    !method.hasModifierProperty(PsiModifier.STATIC)) {
                    methods.add(method);
                }
            }
            methods.sort(Comparator.comparing(PsiMethod::getName));
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
            // 如果计算失败，记录错误并返回默认值
            Logger.getInstance(SerialVersionUIDGenerator.class).error(
                "Failed to compute serialVersionUID for class: " + psiClass.getQualifiedName(),
                e
            );
            return 1L;
        }
    }

    private static PsiElement findAnchor(PsiClass psiClass) {
        PsiElement firstCodeElement = null;

        for (PsiElement child : psiClass.getChildren()) {
            if ((child instanceof PsiField || child instanceof PsiMethod || child instanceof PsiClass)
                && child.getParent() == psiClass) {
                firstCodeElement = child;
                break;
            }
        }

        return firstCodeElement;
    }

    private static int getClassModifiers(PsiClass psiClass) {
        int modifiers = 0;
        if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) modifiers |= 1;
        if (psiClass.hasModifierProperty(PsiModifier.FINAL)) modifiers |= 2;
        if (psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) modifiers |= 4;
        if (psiClass.isInterface()) modifiers |= 8;
        return modifiers;
    }

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
}
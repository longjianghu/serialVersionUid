package com.sohoch.serialVersionUid.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;

public class SerialVersionUIDGenerator {
    public static long generateSerialVersionUID(PsiClass psiClass) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            // 获取类名
            dout.writeUTF(psiClass.getQualifiedName());

            // 获取类的修饰符
            int classModifiers = PsiUtil.getAccessLevel(psiClass.getModifierList());
            dout.writeInt(classModifiers);

            // 获取接口
            PsiClass[] interfaces = psiClass.getInterfaces();
            List<String> interfaceNames = new ArrayList<>();
            for (PsiClass anInterface : interfaces) {
                interfaceNames.add(anInterface.getQualifiedName());
            }
            Collections.sort(interfaceNames);
            for (String interfaceName : interfaceNames) {
                dout.writeUTF(interfaceName);
            }

            // 获取字段
            PsiField[] fields = psiClass.getAllFields();
            List<PsiField> serializableFields = new ArrayList<>();
            for (PsiField field : fields) {
                if (!field.hasModifierProperty(PsiModifier.STATIC) && 
                    !field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                    serializableFields.add(field);
                }
            }

            Collections.sort(serializableFields, (f1, f2) -> 
                f1.getName().compareTo(f2.getName()));

            for (PsiField field : serializableFields) {
                dout.writeUTF(field.getName());
                dout.writeInt(PsiUtil.getAccessLevel(field.getModifierList()));
                dout.writeUTF(field.getType().getCanonicalText());
            }

            // 获取构造函数和方法
            PsiMethod[] methods = psiClass.getMethods();
            List<PsiMethod> serialMethods = new ArrayList<>();
            for (PsiMethod method : methods) {
                if (!method.hasModifierProperty(PsiModifier.PRIVATE)) {
                    serialMethods.add(method);
                }
            }

            Collections.sort(serialMethods, (m1, m2) -> 
                m1.getName().compareTo(m2.getName()));

            for (PsiMethod method : serialMethods) {
                dout.writeUTF(method.getName());
                dout.writeInt(PsiUtil.getAccessLevel(method.getModifierList()));
                dout.writeUTF(method.getReturnType().getCanonicalText());
                PsiParameter[] parameters = method.getParameterList().getParameters();
                for (PsiParameter parameter : parameters) {
                    dout.writeUTF(parameter.getType().getCanonicalText());
                }
            }

            dout.flush();

            // 计算SHA-1哈希
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());

            // 生成serialVersionUID
            long hash = 0;
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            return 1L; // 默认值
        }
    }

    public static boolean isSerializable(PsiClass psiClass) {
        Project project = psiClass.getProject();
        PsiClass serializableClass = JavaPsiFacade.getInstance(project)
            .findClass("java.io.Serializable", GlobalSearchScope.allScope(project));
        return serializableClass != null && psiClass.isInheritor(serializableClass, true);
    }

    public static boolean isJavaVersionAtLeast14(Module module) {
        if (module == null) return false;
        LanguageLevel languageLevel = ModuleRootManager.getInstance(module).getModuleExtension(LanguageLevelModuleExtension.class).getLanguageLevel();
        if (languageLevel == null) return false;
        String languageLevelStr = languageLevel.toString();
        try {
            int version = Integer.parseInt(languageLevelStr.replaceAll("JDK_", ""));
            return version >= 14;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
package com.sohocn.serialVersionUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.LocalInspectionTool;

/**
 * 检查实现了Serializable接口的类是否有serialVersionUID字段
 */
public class SerialVersionUIDInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public String getShortName() {
        return "SerialVersionUID";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Serializable class should have serialVersionUID";
    }

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Serialization";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

}
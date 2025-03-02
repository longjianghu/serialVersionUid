package com.sohocn.serialVersionUID.test;

import java.io.Serial;
import java.io.Serializable;

/**
 * 测试类，用于测试SerialVersionUID Generator插件
 * 
 * 这个类实现了Serializable接口，并且已经有带@Serial注解的serialVersionUID字段
 * 插件应该能够检测到这一点，并提供更新serialVersionUID的功能
 */
public class TestSerializableWithSerial implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int age;
    
    public TestSerializableWithSerial(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "TestSerializableWithSerial{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
} 
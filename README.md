# SerialVersionUID Generator

这是一个IntelliJ IDEA插件，用于自动为实现了`java.io.Serializable`接口的类生成`serialVersionUID`字段。

## 功能特点

- 自动为实现`Serializable`接口的类添加`serialVersionUID`字段
- 当用户继承`Serializable`接口时提示是否自动生成serialVersionUID变量（使用自动完成，当用户按Tab自完成后自动添加serialVersionUID）
- 在编辑器右键菜单中提供生成`serialVersionUID`的选项
- 根据Java SDK版本决定是否添加@Serial注解并引入类文件（Java 14+）

## 使用方法

### 方法1：自动生成

当您在类中实现Serializable接口时，插件会自动为您生成serialVersionUID字段。

### 方法2：使用自动完成

当您在类的实现接口列表中输入"Serializable"时，代码完成会提示您选择"Serializable (with serialVersionUID)"选项，选择后会自动添加serialVersionUID字段。

### 方法3：使用右键菜单

1. 在实现了Serializable接口的类中，右键点击编辑器
2. 在弹出的菜单中选择"生成 serialVersionUID"选项

### 方法4：使用意图动作（Intention Action）

1. 在实现了Serializable接口的类中，将光标放在类的任意位置
2. 按下Alt+Enter（Windows/Linux）或Option+Enter（Mac）
3. 选择"生成 serialVersionUID"选项

### 方法5：使用字段代码完成

1. 在实现了Serializable接口的类中，开始输入"serialVersionUID"
2. 在代码完成列表中选择serialVersionUID选项

### 方法6：通过检查器自动提示

插件会自动检测实现了Serializable接口但没有serialVersionUID字段的类，并在编辑器中显示警告。
点击警告上的快速修复选项，可以自动添加serialVersionUID字段。

## 生成算法

插件使用类的结构信息（类名、修饰符、接口、字段和方法）生成一个唯一的哈希值作为serialVersionUID。
这确保了只要类的结构不变，生成的serialVersionUID就不会改变。

## 注意事项

- 只有在Java SDK版本大于等于14时，才会添加@Serial注解
- 在较低版本的Java中，将只生成serialVersionUID字段，不添加注解

## 要求

- IntelliJ IDEA 2023.1或更高版本
- Java 17或更高版本

## 许可证

本项目采用MIT许可证。详情请参阅LICENSE文件。
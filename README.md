# SerialVersionUID Generator

这是一个IntelliJ IDEA插件，用于自动为实现了`java.io.Serializable`接口的类生成`serialVersionUID`字段。

## 功能特点

- 当用户在接口列表中输入`Serializable`时，按Tab键自动补全接口名称并自动生成`serialVersionUID`字段
- 根据Java SDK版本智能决定是否添加`@Serial`注解（Java 14+）
- 如果用户没有引用`Serializable`和`Serial`类文件，则自动导入（如果存在则不导入）
- 在编辑器的右键菜单提供`SerialVersionUID`菜单项
- 当多次触发时,如果相关的字段、注解和文件存在(不存在根据上面的规则进行自动实全),智能判断是否需要更新`serialVersionUID`值
- 用户可以通过快捷键（Alt+Insert（Windows/Linux）或Cmd+N（Mac）进行触发

## 使用方法

### 方法1：使用自动完成

在已实现Serializable接口的类中，输入`serialVersionUID`并使用代码补全功能，选择"serialVersionUID (generate field)"选项即可自动生成完整的serialVersionUID字段。

## 生成算法

插件使用类的结构信息（类名、修饰符、接口、字段和方法）生成一个唯一的哈希值作为serialVersionUID。这确保了只要类的结构不变，生成的serialVersionUID就不会改变，符合Java序列化规范。

## 注意事项

- 导入文件时注意不要重复导入

## 兼容性

- 插件兼容IntelliJ IDEA 2023.1及以上版本
- 支持所有基于IntelliJ平台且包含Java支持的IDE（如WebStorm、Android Studio等）

## 许可证

本项目采用MIT许可证。详情请参阅LICENSE文件。
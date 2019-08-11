#Dockcmd-maven-plugin

## 插件状态: Alpha
当前阶段本插件尚未加入到Maven公开仓库，所以只支持本地引用模式

### 怎么安装插件？
1.在IDEA中 使用 `maven install` 命令即可把maven插件安装到本地仓库中

2.然后 插件会自动安装到maven仓库硬盘地址 `.m2\repository` 目录下

### 怎么使用插件？
在开发项目中 **pom.xml** 文件中引用

```xml
<plugin>
         <groupId>com.talkweb.plugin</groupId>
          <artifactId>dockercmd-maven-plugin</artifactId>
          <version>1.1.2</version>
          <configuration>
              <container_ports>8080</container_ports>
              <host_ports>8080</host_ports>
              <remote_HTTPSAPI>https://api-docker.talkyun.com.cn:2376</remote_HTTPSAPI>
              <completetag>${docker.image.prefix}/registry/${project.build.finalName}${project.version}}</completetag>
          </configuration>
</plugin>
```
配置解释

`<container_ports>` 你的容器端口

`<host_ports>` 你的主机端口

相当于把docker容器的端口映射到主机端口，使容器能内外网访问

`<remote_HTTPSAPI>` docker服务远程API接口地址（目前仅支持https加密访问）

`<completetag>` 镜像的完整标签（TAG）

### 注意事项
>1.TAG带有URL验证：只支持域名URL（不支持带端口号）只能包含小写, 数字, '-', '_' , '.'和':'

>2.本插件无需设置环境变量DOCKER_HOST，只支持自定义remote_HTTPSAPI参数

>3.容器启动默认绑定IP：0.0.0.0，目前仅支持绑定映射一组端口，多余端口映射会出现异常，请悉知

>4.如有其他功能需要或者BUG请联系作者
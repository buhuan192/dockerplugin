#Dockcmd-maven-plugin

## 插件状态: Alpha

###怎么安装插件？
1.在IDEA中 使用 `maven install` 命令即可把maven插件安装到本地仓库中

2.然后 插件会自动安装到maven仓库硬盘地址 `.m2\repository` 目录下

###怎么使用插件？
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

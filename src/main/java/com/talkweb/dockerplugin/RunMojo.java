package com.talkweb.dockerplugin;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mojo(name = "run",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresProject = true,
        threadSafe = true)
public class RunMojo extends AbstractMojo {

    //容器端口
    @Parameter(property = "dockerplugin.container_ports")
    String container_ports ;
    //主机端口
    @Parameter(property = "dockerplugin.host_ports")
    String host_ports ;
    //标签
    @Parameter(property = "dockerplugin.tag")
    String tag;

    @Override
    public void execute() throws MojoExecutionException{
        DockerClient dc = openDockerClient();

        // Bind container ports to host ports
        final String[] ports = {container_ports, host_ports};
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : ports) {
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port, hostPorts);
        }
        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        // Create container with exposed ports
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(tag).exposedPorts(ports)
                .tty(false)//后台运行
                //.cmd("sh", "-c", "while :; do sleep 1; done") //可以自定义cmd参数
                .build();

        final ContainerCreation creation;
        try {

            creation = dc.createContainer(containerConfig);
            final String id = creation.id();

            // 启动容器
            dc.startContainer(id);

        } catch ( DockerException e ) {
            e.printStackTrace();
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }

        dc.close();
        return;
    }


    private DockerClient openDockerClient() throws MojoExecutionException {

        try {
            return DefaultDockerClient.fromEnv()
                    .build();
        } catch ( DockerCertificateException e) {
            throw new MojoExecutionException("docker 远程 API 创建连接错误", e);
        }
    }
}

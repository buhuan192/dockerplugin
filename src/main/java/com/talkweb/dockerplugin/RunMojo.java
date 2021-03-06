package com.talkweb.dockerplugin;

import com.google.common.annotations.VisibleForTesting;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


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
    @Parameter(property = "dockerplugin.completetag")
    String completetag;
    //远程API地址
    @Parameter(property = "dockerplugin.remote_HTTPSAPI")
    String remote_HTTPSAPI;

    final Log log = getLog();

    //URL验证规则
    private static final String VALID_REPO_REGEX = "^([a-z0-9_.:-])+(\\/[a-z0-9_.:-]+)*$";

    @Override
    public void execute() throws MojoExecutionException,MojoFailureException {
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
        if (!validateRepository(completetag)) {
            throw new MojoFailureException(
                    "标签名称\"" + completetag
                            + "\" 只能是 小写, 数字, '-', '_' , '.'和':'");
        }

        // 创建容器
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(completetag).exposedPorts(ports)
                .tty(false)//后台运行
                //.cmd("sh", "-c", "while :; do sleep 1; done") //可以自定义cmd参数
                .build();

        final ContainerCreation creation;
        try {
            //停止占有该端口的相关容器
            StopContainer(dc);
            creation = dc.createContainer(containerConfig);
            final String id = creation.id();
            //启动容器
            dc.startContainer(id);
            log.info("新容器启动成功！");
        } catch ( InterruptedException|DockerException e ) {
            throw new MojoExecutionException("新容器启动失败！", e);
        }

        dc.close();
        return;
    }


    /*
    * 校验URL的合法性
    *
    * */
    @VisibleForTesting
    static boolean validateRepository(@Nonnull String repository) {
        Pattern pattern = Pattern.compile(VALID_REPO_REGEX);
        return pattern.matcher(repository).matches();
    }

    /*
    * 停止docker容器
    * */
    private void StopContainer(DockerClient dc) throws MojoExecutionException
    {
        try {
            //DockerClient.ListContainersParam lc = DockerClient.ListContainersParam.filter("publish",container_ports+"/"+host_ports);
            final List<Container> containers = dc.listContainers(DockerClient.ListContainersParam.withStatusRunning());

            for (Container x : containers) {
                try {
                    if (x.ports().get(0).publicPort().toString().equals(host_ports) && x.ports().get(0).privatePort().toString().equals(container_ports)) {
                        dc.stopContainer(x.id(), 1);
                        log.info("已成功停止映射端口["+container_ports+":"+host_ports+"]的容器"+containers.size()+"个！");
                    }
                } catch (DockerException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (DockerException|InterruptedException e) {
            throw new MojoExecutionException("停止容器操作失败！",e);
        }
    }

    /*
     * 建立远程连接
     * */
    private DockerClient openDockerClient() throws MojoExecutionException  {
        //docker TLS证书保存路径
        String caPath =System.getProperty("user.home").replaceAll("\\\\","\\\\\\\\")+"\\.docker\\";
        try {
            return DefaultDockerClient.fromEnv().uri(URI.create(remote_HTTPSAPI))
                    .dockerCertificates(new DockerCertificates(Paths.get(caPath)))
                    .build();
        } catch ( DockerCertificateException e) {
            throw new MojoExecutionException("docker 远程 API 创建连接错误", e);
        }
    }
}

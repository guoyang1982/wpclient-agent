package com.gy.woodpecker.agent;

import com.gy.woodpecker.log.LoggerFacility;
import com.gy.woodpecker.netty.NettyTelnetServer;
import com.gy.woodpecker.tools.ConfigPropertyUtile;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * Created by guoyang on 17/10/25.
 */
public class WpClientAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        main(agentArgs, inst);

    }

    public static void agentmain(String args, Instrumentation inst) {main(args, inst);
    }

    private static void main(String agentArgs, Instrumentation inst) {
        try {
            inst.appendToBootstrapClassLoaderSearch(
                    new JarFile(WpClientAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(null == agentArgs || agentArgs.equals("")){
            System.out.println("传入的配置文件为空!");
            return;
        }

        //初始化配置文件
        ConfigPropertyUtile.initProperties(agentArgs);
        //启动日志搜集
        LoggerFacility loggerFacility = LoggerFacility.getInstall();
        //loggerFacility.setAppName(agentArgs);
        loggerFacility.healthCheck();

        //处理要插桩的类
        inst.addTransformer(new WoodpeckTransformer());

        //初始化 监控端口
        String nettyS = ConfigPropertyUtile.getProperties().getProperty("log.netty.server");
        if(null == nettyS || nettyS.equals("")){
            nettyS = "true";
        }
        if(nettyS.equals("true")){
            NettyTelnetServer nettyTelnetServer = new NettyTelnetServer();
            try {
                nettyTelnetServer.open();
            } catch (InterruptedException e) {
                nettyTelnetServer.close();
            }
        }
    }

}

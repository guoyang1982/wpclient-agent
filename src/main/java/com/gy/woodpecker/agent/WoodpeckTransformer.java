package com.gy.woodpecker.agent;

import com.gy.woodpecker.tools.ConfigPropertyUtile;
import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Created by guoyang on 17/10/27.
 */
public class WoodpeckTransformer implements ClassFileTransformer {

    private String loggerClassic;
    private String methodName;
    private String javassistInfo;
    private String loger = "logback";
    public static final String SP = (char)18 + "";

    private final String logbakInfo = "if(level.levelStr.equals(\"ERROR\")){" +
            "com.gy.woodpecker.log.LoggerFacility loggerFacility = com.gy.woodpecker.log.LoggerFacility.getInstall();" +
            "loggerFacility.sendToRedis(msg);}";

    private final String log4jInfo = "if(level.levelStr.equals(\"ERROR\")){" +
            "com.gy.woodpecker.log.LoggerFacility loggerFacility = com.gy.woodpecker.log.LoggerFacility.getInstall();" +
            "loggerFacility.sendToRedis(message.toString());}";


    public boolean validLevel(String level){
        if(null == level || level.equals("")){
            return false;
        }
        if(level.toUpperCase().equals("ERROR")){
            return true;
        }
        if(level.toUpperCase().equals("INFO")){
            return true;
        }
        if(level.toUpperCase().equals("DEBUG")){
            return true;
        }

        return false;
    }

    public WoodpeckTransformer(){
        String logerT = ConfigPropertyUtile.getProperties().getProperty("agent.log.name");
        String level = ConfigPropertyUtile.getProperties().getProperty("agent.log.level");

        if(null != logerT && !logerT.equals("")){
            loger = logerT;
        }
        if(loger.equals("logback")){
            loggerClassic = "ch.qos.logback.classic.Logger";
            methodName = "buildLoggingEventAndAppend";
            if(validLevel(level)){
                javassistInfo = logbakInfo.replaceFirst("ERROR",level);
            }else {
                javassistInfo = logbakInfo;
            }
        }
        if(loger.equals("log4j")){
            loggerClassic = "org.apache.log4j.Category";
            methodName = "forcedLog";
            if(validLevel(level)){
                javassistInfo = log4jInfo.replaceFirst("ERROR",level);
            }else {
                javassistInfo = log4jInfo;
            }
        }
    }

    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        className = className.replace('/', '.');
        if (isNeedLogExecuteInfo(className)) {
            if (null == loader) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            byteCode = aopLog(loader, className, byteCode);
        }
        return byteCode;
    }

    private byte[] aopLog(ClassLoader loader, String className, byte[] byteCode) {
        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = null;
            try {
                cc = cp.get(className);
            } catch (NotFoundException e) {
                cp.insertClassPath(new LoaderClassPath(loader));
                cc = cp.get(className);
            }
            byteCode = aopLog(cc, className, byteCode);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return byteCode;
    }

    private byte[] aopLog(CtClass cc, String className, byte[] byteCode) throws CannotCompileException, IOException {
        if (null == cc) {
            return byteCode;
        }
        if (!cc.isInterface()) {
            CtMethod[] methods = cc.getDeclaredMethods();
            if (null != methods && methods.length > 0) {
                for (CtMethod m : methods) {
                    if (m.getName().equals(methodName)) {
                        aopLog(className, m);
                    }
                }
                byteCode = cc.toBytecode();
            }
        }
        cc.detach();
        return byteCode;
    }

    private void aopLog(String className, CtMethod m) throws CannotCompileException {
        if (null == m || m.isEmpty()) {
            return;
        }
        String ip=com.gy.woodpecker.tools.IPUtile.getIntranetIP();
        m.insertBefore(javassistInfo);
    }

    private boolean isNeedLogExecuteInfo(String className) {
        if (className.equals(loggerClassic)) {
            return true;
        }
        return false;
    }
}


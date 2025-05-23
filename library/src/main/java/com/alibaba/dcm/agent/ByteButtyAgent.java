package com.alibaba.dcm.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ByteButtyAgent {
    public static void premain0(String agentArgs, Instrumentation inst) {
    System.out.println("[ByteButtyAgent] Agent Premain 开始...");

    try {
        // 获取当前 Agent Jar 的路径
        // 注意：这里的获取方式依赖于 Agent 是通过 -javaagent 参数加载的
        URL agentJarUrl = ByteButtyAgent.class.getProtectionDomain().getCodeSource().getLocation();
        System.out.println("[ByteButtyAgent] Agent Jar URL: " + agentJarUrl);

        // 创建一个独立的 ClassLoader 来加载 IsolatedAgentLogic 及其依赖
        // 将父 ClassLoader 设置为 null 或 Bootstrap ClassLoader，避免向上委派
        URLClassLoader isolatedClassLoader = new URLClassLoader(new URL[]{agentJarUrl}, null); // 或者 BootClassLoader

        // 使用独立的 ClassLoader 加载 IsolatedAgentLogic 类
        Class<?> isolatedLogicClass = isolatedClassLoader.loadClass("com.alibaba.dcm.agent.IsolatedAgentLogic");

        // 创建 IsolatedAgentLogic 实例
        Object isolatedLogicInstance = isolatedLogicClass.getDeclaredConstructor().newInstance();

        // 获取并调用 startInstrumentation 方法
        Method startMethod = isolatedLogicClass.getMethod("startInstrumentation", String.class, Instrumentation.class);
        startMethod.invoke(isolatedLogicInstance, agentArgs, inst);

        System.out.println("[ByteButtyAgent] IsolatedAgentLogic 启动成功.");

    } catch (Exception e) {
        System.err.println("[ByteButtyAgent] Agent 启动失败: " + e.getMessage());
        e.printStackTrace();
        // 可以在这里选择是否让应用启动失败
        // System.exit(1); // 注释掉，避免 Agent 失败导致应用退出
    }
}
}

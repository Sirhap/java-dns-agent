package com.alibaba.dcm.agent;

import com.alibaba.dcm.DnsInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.Instrumentation;

public class IsolatedAgentLogic {

    // 这个方法将被 AgentMain 通过反射调用
    public void startInstrumentation(String agentArgs, Instrumentation inst) {
        System.out.println("[IsolatedAgentLogic] 启动，准备监控方法耗时...");

        // 2. 定义对 DnsNameResolver.doResolve 的拦截
        new AgentBuilder.Default()
                .with(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE)
                .type(ElementMatchers.nameEndsWith("io.netty.resolver.dns.DnsNameResolver"))
                .transform((builder, type, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doResolve")
                                        .and(ElementMatchers.takesArguments(4)))
                                .intercept(MethodDelegation.to(DnsInterceptor.class))
                ).installOn(inst);

        System.out.println("[IsolatedAgentLogic] Instrumentation 已安装.");
    }

}

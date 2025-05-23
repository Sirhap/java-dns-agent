package com.alibaba.dcm;

import io.netty.handler.codec.dns.DnsRecord;
import io.netty.resolver.dns.DnsCache;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Promise;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.Callable;

import static io.netty.util.NetUtil.isValidIpV4Address;
import static io.netty.util.NetUtil.isValidIpV6Address;

public class DnsInterceptor {

    // 在 premain 中加载配置，并设置 Byte Buddy 插桩


    // 加载配置文件，返回域名->InetAddress 映射


    // 拦截器：检查配置映射，若命中则返回指定 IP，否则调用原方法
        public DnsInterceptor() {
        }

        // 注意：方法签名应匹配被拦截的方法
        @RuntimeType
        public static Object intercept(@Origin Method method,
                                       @AllArguments Object[] args,
                                       @SuperCall Callable<?> callable) {


            try {
                System.out.println("[DnsInterceptor] 拦截 doResolve 方法");

                String inetHost = (String) args[0];
                DnsRecord[] additionals = (DnsRecord[]) args[1];
                Promise<InetAddress> promise = (Promise<InetAddress>) args[2];
                io.netty.resolver.dns.DnsCache resolveCache = (DnsCache) args[3];

                DnsCacheEntry ip = DnsCacheManipulator.getDnsCache(inetHost);
                if (ip != null) {
                    final byte[] bytes = NetUtil.createByteArrayFromIpAddressString(ip.getIp());
                    if (bytes != null) {
                        // The inetHost is actually an ipaddress.
                        promise.setSuccess(InetAddress.getByAddress(bytes));
                        return null;
                    }
                    // 调用原始方法
                    Object result = callable.call();
                    System.out.println("[DnsInterceptor] 原始方法执行完成");
                    return result;
                } else {
                    // 调用原始方法
                    Object result = callable.call();
                    System.out.println("[DnsInterceptor] 原始方法执行完成");
                    return result;
                }
            } catch (Exception e) {
                System.err.println("[DnsInterceptor] 方法执行出错: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }


}

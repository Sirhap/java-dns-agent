package com.alibaba.dcm.transformer;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

public class VersionPatchingTransformer implements AgentBuilder.Transformer {
    private final int targetVersion;

    public VersionPatchingTransformer(int targetVersion) {
        this.targetVersion = targetVersion;
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription typeDescription,
                                            ClassLoader classLoader,
                                            JavaModule module,
                                            ProtectionDomain protectionDomain) {
        System.err.println("[VersionPatchingTransformer] Checking " + typeDescription.getName() + ", target version: " + targetVersion);

        try {
            // 获取类的字节码
            String resourcePath = typeDescription.getName().replace('.', '/') + ".class";
            InputStream is = classLoader.getResourceAsStream(resourcePath);

            if (is == null) {
                System.err.println("[VersionPatchingTransformer] Could not get InputStream for " + typeDescription.getName() + ". Skipping version check.");
                return builder;
            }

            // 读取类文件版本
            ClassReader cr = new ClassReader(is);
            int currentVersion = cr.readShort(6); // 类文件版本在字节码的第6-7字节
            is.close();

            System.err.println("[VersionPatchingTransformer] Current version for " + typeDescription.getName() + ": " + currentVersion);

            // 只有当当前版本低于目标版本时才进行转换
            if (currentVersion < targetVersion) {
                System.err.println("[VersionPatchingTransformer] Version upgrade needed for " + typeDescription.getName());
                return builder.visit(new AsmVisitorWrapper() {
                    @Override
                    public int mergeWriter(int flags) {
                        return flags;
                    }

                    @Override
                    public int mergeReader(int flags) {
                        return flags;
                    }

                    @Override
                    public net.bytebuddy.jar.asm.ClassVisitor wrap(TypeDescription typeDescription,
                                                                   net.bytebuddy.jar.asm.ClassVisitor classVisitor,
                                                                   Implementation.Context context,
                                                                   TypePool typePool,
                                                                   FieldList<FieldDescription.InDefinedShape> fieldList,
                                                                   MethodList<?> methodList,
                                                                   int writerFlags,
                                                                   int readerFlags) {
                        return new net.bytebuddy.jar.asm.ClassVisitor(Opcodes.ASM9, classVisitor) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                              String superName, String[] interfaces) {
                                System.err.println("[VersionPatchingTransformer] Modifying version for " + name + " from " + version + " to " + targetVersion);
                                super.visit(targetVersion, access, name, signature, superName, interfaces);
                            }
                        };
                    }
                });
            } else {
                System.err.println("[VersionPatchingTransformer] No version upgrade needed for " + typeDescription.getName());
                return builder;
            }

        } catch (IOException e) {
            System.err.println("[VersionPatchingTransformer] IOException while checking version for " + typeDescription.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return builder;
        } catch (Exception e) {
            System.err.println("[VersionPatchingTransformer] Unexpected error while checking version for " + typeDescription.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return builder;
        }
    }
}

package org.jetbrains.research.kotlinrminer.cli.util;

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.environment.UtilKt;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

public class EnvironmentManager {

    /**
     * Creates KotlinCoreEnvironment with specified classpath.
     */
    public static KotlinCoreEnvironment createKotlinCoreEnvironment(Set<File> libraries) {
        UtilKt.setIdeaIoUseFallback();

        CompilerConfiguration configuration = new CompilerConfiguration();
        List<File> files = PathUtil.getJdkClassesRootsFromCurrentJre();
        files.addAll(libraries);
        JvmContentRootsKt.addJvmClasspathRoots(configuration, files);
        configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME);
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.Companion.getNONE());
        return KotlinCoreEnvironment.createForProduction(() -> {
        }, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

}

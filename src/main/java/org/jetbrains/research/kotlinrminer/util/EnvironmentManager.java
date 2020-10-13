package org.jetbrains.research.kotlinrminer.util;

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.cli.common.environment.UtilKt.setIdeaIoUseFallback;
import static org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt.addJvmClasspathRoots;
import static org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil.DEFAULT_MODULE_NAME;

public class EnvironmentManager {

    /**
     * Creates KotlinCoreEnvironment with specified classpath.
     */
    public static KotlinCoreEnvironment createKotlinCoreEnvironment(Set<File> libraries) {
        setIdeaIoUseFallback();

        CompilerConfiguration configuration = new CompilerConfiguration();
        List<File> files = PathUtil.getJdkClassesRootsFromCurrentJre();
        files.addAll(libraries);
        addJvmClasspathRoots(configuration, files);
        configuration.put(CommonConfigurationKeys.MODULE_NAME, DEFAULT_MODULE_NAME);
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.Companion.getNONE());
        return KotlinCoreEnvironment.createForProduction(() -> {
        }, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

}

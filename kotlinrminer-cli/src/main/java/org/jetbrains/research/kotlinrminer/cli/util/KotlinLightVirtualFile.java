package org.jetbrains.research.kotlinrminer.cli.util;

import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import java.io.File;
import java.io.IOException;

/**
 * Wrapper for VirtualFile that retains path on machine.
 */
public class KotlinLightVirtualFile extends LightVirtualFile {
    private final String path;

    public KotlinLightVirtualFile(File file, String text) throws IOException {
        super(file.getName(), KotlinLanguage.INSTANCE, text);
        this.path = file.getCanonicalPath();
    }

    @Override
    public String getPath() {
        return this.path;
    }
}

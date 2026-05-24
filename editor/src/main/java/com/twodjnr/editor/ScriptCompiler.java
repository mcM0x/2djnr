package com.twodjnr.editor;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ScriptCompiler {

    public static class Result {
        private final boolean success;
        private final Map<String, Class<?>> classes;
        private final List<String> diagnostics;

        Result(boolean success, Map<String, Class<?>> classes, List<String> diagnostics) {
            this.success = success;
            this.classes = classes;
            this.diagnostics = diagnostics;
        }

        public boolean success() { return success; }
        public Map<String, Class<?>> classes() { return classes; }
        public List<String> diagnostics() { return diagnostics; }
    }

    public static Result compile(File projectDir) {
        if (projectDir == null) return new Result(true, Map.of(), List.of());

        File scriptsDir = new File(projectDir, "scripts");
        if (!scriptsDir.exists() || !scriptsDir.isDirectory()) return new Result(true, Map.of(), List.of());

        File[] javaFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) return new Result(true, Map.of(), List.of());

        File cacheDir = new File(projectDir, ".cache/scripts");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return new Result(false, Map.of(), List.of("Failed to create cache directory"));
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new Result(false, Map.of(), List.of("No Java compiler available (not running on JDK)"));
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFiles);

        String classpath = System.getProperty("java.class.path");
        String outputDir = cacheDir.getAbsolutePath();

        List<String> options = List.of(
                "-d", outputDir,
                "-cp", classpath,
                "-source", "21",
                "-target", "21"
        );

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
        );

        boolean success = task.call();

        try {
            fileManager.close();
        } catch (IOException e) {
            // ignore
        }

        List<String> diagMessages = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String msg = d.getKind() + ": " + d.getMessage(null);
            if (d.getSource() != null) {
                msg = d.getSource().getName() + " line " + d.getLineNumber() + ": " + msg;
            }
            diagMessages.add(msg);
        }

        if (!success) {
            return new Result(false, Map.of(), diagMessages);
        }

        Map<String, Class<?>> classes = new HashMap<>();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{cacheDir.toURI().toURL()},
                ScriptCompiler.class.getClassLoader())) {

            for (File javaFile : javaFiles) {
                String fileName = javaFile.getName();
                String className = fileName.substring(0, fileName.length() - ".java".length());
                String fqcn = "scripts." + className;
                try {
                    Class<?> clazz = loader.loadClass(fqcn);
                    classes.put(className, clazz);
                } catch (ClassNotFoundException e) {
                    diagMessages.add("Warning: could not load compiled class " + fqcn);
                }
            }
        } catch (IOException e) {
            diagMessages.add("Error loading compiled classes: " + e.getMessage());
            return new Result(false, classes, diagMessages);
        }

        return new Result(true, classes, diagMessages);
    }
}

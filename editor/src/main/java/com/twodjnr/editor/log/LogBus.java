package com.twodjnr.editor.log;

import com.twodjnr.editor.signal.EditorSignals;
import com.twodjnr.engine.signal.SignalBus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;

public class LogBus {

    private static File logFile;

    private LogBus() {}

    public static synchronized void init(File projectDir) {
        if (projectDir == null) {
            logFile = null;
            return;
        }
        File newLog = new File(projectDir, ".editor-log.txt");
        File oldLog = new File(projectDir, ".editor-log-old.txt");
        if (newLog.exists()) {
            oldLog.delete();
            newLog.renameTo(oldLog);
        }
        logFile = newLog;
        try (Writer w = new FileWriter(logFile)) {
            w.write("=== 2DJNR Editor Log ===\n");
            w.write("Started: " + Instant.now() + "\n\n");
        } catch (IOException e) {
            System.err.println("LogBus: failed to init log file: " + e.getMessage());
        }
    }

    public static void connect(Object target, String methodName) {
        SignalBus.connect(EditorSignals.LOG, target, methodName);
    }

    public static void disconnect(Object target) {
        SignalBus.disconnect(EditorSignals.LOG, target);
    }

    public static void log(String component, String action, String detail, Runnable actionBlock) {
        LogEntry entry = new LogEntry(Instant.now(), Thread.currentThread().getName(),
                component, action, detail != null ? detail : "");

        System.out.println(entry.format());
        appendToFile(entry);
        SignalBus.emit(EditorSignals.LOG, entry);

        if (actionBlock != null) {
            actionBlock.run();
        }
    }

    public static void log(String component, String action, String detail) {
        log(component, action, detail, null);
    }

    private static synchronized void appendToFile(LogEntry entry) {
        if (logFile == null) return;
        try (Writer w = new FileWriter(logFile, true)) {
            w.write(entry.format() + "\n");
        } catch (IOException e) {
            System.err.println("LogBus: failed to write: " + e.getMessage());
        }
    }
}

package com.twodjnr.log;

import com.twodjnr.signal.SignalBus;
import com.twodjnr.signal.Signals;

public final class LogBus {
    private LogBus() {}

    public static void log(String component, String action, String detail) {
        System.out.println("[" + component + "] " + action + ": " + detail);
        SignalBus.emit(Signals.LOG, null, new LogEntry(component, action, detail));
    }
}

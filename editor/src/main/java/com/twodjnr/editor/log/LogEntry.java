package com.twodjnr.editor.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record LogEntry(Instant timestamp, String thread, String component, String action, String detail) {

    static final DateTimeFormatter TIME_FMT = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));

    public String formattedTime() {
        return TIME_FMT.format(timestamp);
    }

    public String format() {
        return "[" + formattedTime() + "] [" + thread + "] " + component + " | " + action
                + (detail != null && !detail.isEmpty() ? " | " + detail : "");
    }

    public String toTsv() {
        return timestamp + "\t" + thread + "\t" + component + "\t" + action + "\t" + (detail != null ? detail : "");
    }

    public static String tsvHeader() {
        return "Timestamp\tThread\tComponent\tAction\tDetail";
    }
}

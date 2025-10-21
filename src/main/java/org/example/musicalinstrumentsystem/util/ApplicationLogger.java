package org.example.musicalinstrumentsystem.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class ApplicationLogger {
    
    // Volatile ensures visibility across threads
    private static volatile ApplicationLogger instance;

    private static final Object lock = new Object();
    
    // Log storage
    private final List<LogEntry> logHistory;
    private final DateTimeFormatter formatter;
    private final int maxHistorySize = 1000;
    
    // Private constructor
    private ApplicationLogger() {
        logHistory = new ArrayList<>();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(" ApplicationLogger Singleton initialized (Thread-Safe)");
    }
    

    public static ApplicationLogger getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ApplicationLogger();
                }
            }
        }
        return instance;
    }
    

    public synchronized void info(String category, String message) {
        log(LogLevel.INFO, category, message);
    }
    

    public synchronized void warn(String category, String message) {
        log(LogLevel.WARN, category, message);
    }
    

    public synchronized void error(String category, String message) {
        log(LogLevel.ERROR, category, message);
    }
    

    public synchronized void debug(String category, String message) {
        log(LogLevel.DEBUG, category, message);
    }
    

    private void log(LogLevel level, String category, String message) {
        LocalDateTime timestamp = LocalDateTime.now();
        LogEntry entry = new LogEntry(timestamp, level, category, message);
        
        // Add to history
        logHistory.add(entry);
        
        // Maintain max size
        if (logHistory.size() > maxHistorySize) {
            logHistory.remove(0);
        }
        
        // Console output
        System.out.println(formatLogEntry(entry));
    }
    

    private String formatLogEntry(LogEntry entry) {
        return String.format("[%s] [%s] [%s] %s",
            entry.timestamp.format(formatter),
            entry.level,
            entry.category,
            entry.message);
    }
    

    public synchronized List<LogEntry> getRecentLogs(int count) {
        int size = logHistory.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(logHistory.subList(fromIndex, size));
    }
    

    public synchronized List<LogEntry> getAllLogs() {
        return new ArrayList<>(logHistory);
    }
    

    public synchronized void clearLogs() {
        logHistory.clear();
        System.out.println("️ Log history cleared");
    }
    

    public synchronized void exportLogsToFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("=== APPLICATION LOG EXPORT ===\n");
            writer.write("Generated: " + LocalDateTime.now().format(formatter) + "\n");
            writer.write("Total Entries: " + logHistory.size() + "\n\n");
            
            for (LogEntry entry : logHistory) {
                writer.write(formatLogEntry(entry) + "\n");
            }
            
            System.out.println(" Logs exported to: " + filePath);
        } catch (IOException e) {
            System.err.println(" Failed to export logs: " + e.getMessage());
        }
    }
    

    public synchronized LogStatistics getStatistics() {
        int infoCount = 0;
        int warnCount = 0;
        int errorCount = 0;
        int debugCount = 0;
        
        for (LogEntry entry : logHistory) {
            switch (entry.level) {
                case INFO -> infoCount++;
                case WARN -> warnCount++;
                case ERROR -> errorCount++;
                case DEBUG -> debugCount++;
            }
        }
        
        return new LogStatistics(infoCount, warnCount, errorCount, debugCount);
    }
    
    // Inner classes
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
    
    public static class LogEntry {
        public final LocalDateTime timestamp;
        public final LogLevel level;
        public final String category;
        public final String message;
        
        public LogEntry(LocalDateTime timestamp, LogLevel level, String category, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.category = category;
            this.message = message;
        }
    }
    
    public static class LogStatistics {
        public final int infoCount;
        public final int warnCount;
        public final int errorCount;
        public final int debugCount;
        
        public LogStatistics(int infoCount, int warnCount, int errorCount, int debugCount) {
            this.infoCount = infoCount;
            this.warnCount = warnCount;
            this.errorCount = errorCount;
            this.debugCount = debugCount;
        }
        
        @Override
        public String toString() {
            return String.format("INFO: %d, WARN: %d, ERROR: %d, DEBUG: %d",
                infoCount, warnCount, errorCount, debugCount);
        }
    }
}

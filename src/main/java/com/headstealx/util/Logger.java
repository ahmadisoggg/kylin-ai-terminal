package com.headstealx.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Enhanced logging utility for HeadStealX with phase-based logging
 * Supports console and file logging with different levels
 */
public class Logger {
    
    private final JavaPlugin plugin;
    private final java.util.logging.Logger bukkitLogger;
    private final boolean fileLogging;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    
    // Log levels
    public enum LogLevel {
        STARTUP("STARTUP", "§a"),
        LIBLOAD("LIBLOAD", "§b"),
        REGISTER("REGISTER", "§e"),
        READY("READY", "§2"),
        INFO("INFO", "§f"),
        DEBUG("DEBUG", "§7"),
        WARNING("WARNING", "§6"),
        SEVERE("SEVERE", "§c");
        
        private final String name;
        private final String color;
        
        LogLevel(String name, String color) {
            this.name = name;
            this.color = color;
        }
        
        public String getName() {
            return name;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    public Logger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bukkitLogger = plugin.getLogger();
        this.fileLogging = plugin.getConfig().getBoolean("logging.file_logging", true);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Setup log file
        if (fileLogging) {
            String logFileName = plugin.getConfig().getString("logging.log_file", "headstealx.log");
            this.logFile = new File(plugin.getDataFolder(), logFileName);
            
            // Create log file if it doesn't exist
            if (!logFile.exists()) {
                try {
                    logFile.getParentFile().mkdirs();
                    logFile.createNewFile();
                } catch (IOException e) {
                    bukkitLogger.warning("Failed to create log file: " + e.getMessage());
                }
            }
        } else {
            this.logFile = null;
        }
    }
    
    /**
     * Log startup phase messages
     */
    public void startup(String message) {
        log(LogLevel.STARTUP, message);
    }
    
    /**
     * Log library loading phase messages
     */
    public void libload(String message) {
        log(LogLevel.LIBLOAD, message);
    }
    
    /**
     * Log registration phase messages
     */
    public void register(String message) {
        log(LogLevel.REGISTER, message);
    }
    
    /**
     * Log ready phase messages
     */
    public void ready(String message) {
        log(LogLevel.READY, message);
    }
    
    /**
     * Log info messages
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Log debug messages
     */
    public void debug(String message) {
        if (plugin.getConfig().getBoolean("general.behavior.debug_mode", false)) {
            log(LogLevel.DEBUG, message);
        }
    }
    
    /**
     * Log warning messages
     */
    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }
    
    /**
     * Log severe messages
     */
    public void severe(String message) {
        log(LogLevel.SEVERE, message);
    }
    
    /**
     * Log exception with stack trace
     */
    public void severe(String message, Throwable throwable) {
        log(LogLevel.SEVERE, message);
        if (plugin.getConfig().getBoolean("general.behavior.debug_mode", false)) {
            throwable.printStackTrace();
        }
        
        // Log to file with full stack trace
        if (fileLogging && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(formatLogMessage(LogLevel.SEVERE, message));
                throwable.printStackTrace(writer);
            } catch (IOException e) {
                bukkitLogger.warning("Failed to write to log file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Core logging method
     */
    private void log(LogLevel level, String message) {
        // Console logging
        Level bukkitLevel = getBukkitLevel(level);
        String formattedMessage = formatConsoleMessage(level, message);
        bukkitLogger.log(bukkitLevel, formattedMessage);
        
        // File logging
        if (fileLogging && logFile != null) {
            logToFile(level, message);
        }
    }
    
    /**
     * Format message for console output
     */
    private String formatConsoleMessage(LogLevel level, String message) {
        return String.format("[%s] %s", level.getName(), message);
    }
    
    /**
     * Format message for file output
     */
    private String formatLogMessage(LogLevel level, String message) {
        return String.format("[%s] [%s] %s", 
            dateFormat.format(new Date()), 
            level.getName(), 
            message);
    }
    
    /**
     * Write to log file
     */
    private void logToFile(LogLevel level, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(formatLogMessage(level, message));
        } catch (IOException e) {
            bukkitLogger.warning("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Convert custom log level to Bukkit level
     */
    private Level getBukkitLevel(LogLevel level) {
        switch (level) {
            case SEVERE:
                return Level.SEVERE;
            case WARNING:
                return Level.WARNING;
            case DEBUG:
                return Level.FINE;
            case STARTUP:
            case LIBLOAD:
            case REGISTER:
            case READY:
            case INFO:
            default:
                return Level.INFO;
        }
    }
    
    /**
     * Rotate log files if they get too large
     */
    public void rotateLogFile() {
        if (!fileLogging || logFile == null || !logFile.exists()) {
            return;
        }
        
        long maxSize = plugin.getConfig().getLong("logging.max_file_size", 10) * 1024 * 1024; // MB to bytes
        
        if (logFile.length() > maxSize) {
            int maxFiles = plugin.getConfig().getInt("logging.max_files", 5);
            
            // Rotate existing files
            for (int i = maxFiles - 1; i > 0; i--) {
                File oldFile = new File(plugin.getDataFolder(), "headstealx.log." + i);
                File newFile = new File(plugin.getDataFolder(), "headstealx.log." + (i + 1));
                
                if (oldFile.exists()) {
                    if (newFile.exists()) {
                        newFile.delete();
                    }
                    oldFile.renameTo(newFile);
                }
            }
            
            // Move current log to .1
            File rotatedFile = new File(plugin.getDataFolder(), "headstealx.log.1");
            if (rotatedFile.exists()) {
                rotatedFile.delete();
            }
            logFile.renameTo(rotatedFile);
            
            // Create new log file
            try {
                logFile.createNewFile();
                info("Log file rotated");
            } catch (IOException e) {
                bukkitLogger.warning("Failed to create new log file: " + e.getMessage());
            }
        }
    }
}
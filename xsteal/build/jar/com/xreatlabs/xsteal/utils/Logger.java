package com.xreatlabs.xsteal.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Enhanced logging utility for XSteal with file logging support
 */
public class Logger {
    
    private final JavaPlugin plugin;
    private final java.util.logging.Logger bukkitLogger;
    private final boolean fileLogging;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    
    public Logger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bukkitLogger = plugin.getLogger();
        this.fileLogging = plugin.getConfig().getBoolean("logging.file_logging", true);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Setup log file
        if (fileLogging) {
            String logFileName = plugin.getConfig().getString("logging.log_file", "xsteal.log");
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
     * Log info messages
     */
    public void info(String message) {
        log(Level.INFO, message);
    }
    
    /**
     * Log debug messages (only if debug mode is enabled)
     */
    public void debug(String message) {
        if (plugin.getConfig().getBoolean("general.debug_mode", false)) {
            log(Level.FINE, "[DEBUG] " + message);
        }
    }
    
    /**
     * Log warning messages
     */
    public void warning(String message) {
        log(Level.WARNING, message);
    }
    
    /**
     * Log severe messages
     */
    public void severe(String message) {
        log(Level.SEVERE, message);
    }
    
    /**
     * Log severe messages with exception
     */
    public void severe(String message, Throwable throwable) {
        log(Level.SEVERE, message);
        if (plugin.getConfig().getBoolean("general.debug_mode", false)) {
            throwable.printStackTrace();
        }
        
        // Log to file with full stack trace
        if (fileLogging && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(formatLogMessage(Level.SEVERE, message));
                throwable.printStackTrace(writer);
            } catch (IOException e) {
                bukkitLogger.warning("Failed to write to log file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Core logging method
     */
    private void log(Level level, String message) {
        // Console logging with color
        String coloredMessage = addColor(level, message);
        bukkitLogger.log(level, coloredMessage);
        
        // File logging
        if (fileLogging && logFile != null) {
            logToFile(level, message);
        }
    }
    
    /**
     * Add color codes to console messages
     */
    private String addColor(Level level, String message) {
        ChatColor color;
        switch (level.getName()) {
            case "SEVERE":
                color = ChatColor.RED;
                break;
            case "WARNING":
                color = ChatColor.YELLOW;
                break;
            case "FINE":
                color = ChatColor.GRAY;
                break;
            default:
                color = ChatColor.WHITE;
                break;
        }
        return color + message + ChatColor.RESET;
    }
    
    /**
     * Format message for file output
     */
    private String formatLogMessage(Level level, String message) {
        return String.format("[%s] [%s] %s", 
            dateFormat.format(new Date()), 
            level.getName(), 
            ChatColor.stripColor(message));
    }
    
    /**
     * Write to log file
     */
    private void logToFile(Level level, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(formatLogMessage(level, message));
        } catch (IOException e) {
            bukkitLogger.warning("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Rotate log files if they get too large
     */
    public void rotateLogFile() {
        if (!fileLogging || logFile == null || !logFile.exists()) {
            return;
        }
        
        long maxSize = plugin.getConfig().getLong("logging.max_file_size_mb", 10) * 1024 * 1024;
        
        if (logFile.length() > maxSize) {
            int maxFiles = plugin.getConfig().getInt("logging.max_log_files", 5);
            
            // Rotate existing files
            for (int i = maxFiles - 1; i > 0; i--) {
                File oldFile = new File(plugin.getDataFolder(), "xsteal.log." + i);
                File newFile = new File(plugin.getDataFolder(), "xsteal.log." + (i + 1));
                
                if (oldFile.exists()) {
                    if (newFile.exists()) {
                        newFile.delete();
                    }
                    oldFile.renameTo(newFile);
                }
            }
            
            // Move current log to .1
            File rotatedFile = new File(plugin.getDataFolder(), "xsteal.log.1");
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
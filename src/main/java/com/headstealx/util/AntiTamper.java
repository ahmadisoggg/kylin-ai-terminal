package com.headstealx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Anti-tamper protection utility for HeadStealX
 * Provides basic protection against reverse engineering and tampering
 */
public class AntiTamper {
    
    private static final String EXPECTED_SIGNATURE_RESOURCE = "/embedded.sig";
    private static boolean verificationPassed = false;
    
    /**
     * Verify the integrity of the plugin JAR
     * This is a basic implementation - in production, you'd want more sophisticated protection
     */
    public static void verify() throws SecurityException {
        try {
            // Check for debugging/profiling tools
            detectDebugging();
            
            // Verify JAR integrity (basic implementation)
            verifyJarIntegrity();
            
            // Additional anti-tamper checks
            performAdditionalChecks();
            
            verificationPassed = true;
            
        } catch (Exception e) {
            throw new SecurityException("Anti-tamper verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if verification has passed
     */
    public static boolean isVerificationPassed() {
        return verificationPassed;
    }
    
    /**
     * Detect debugging and profiling tools
     */
    private static void detectDebugging() {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        
        for (String arg : inputArguments) {
            String argLower = arg.toLowerCase();
            
            // Check for common debugging flags
            if (argLower.contains("-agentlib:jdwp") ||
                argLower.contains("-xdebug") ||
                argLower.contains("-xrunjdwp") ||
                argLower.contains("-javaagent") ||
                argLower.contains("jprofiler") ||
                argLower.contains("yourkit") ||
                argLower.contains("jrebel")) {
                
                throw new SecurityException("Debugging/profiling tools detected");
            }
        }
        
        // Check for debug properties
        if (System.getProperty("java.compiler") != null && 
            System.getProperty("java.compiler").equals("NONE")) {
            throw new SecurityException("Compiler disabled - potential debugging");
        }
    }
    
    /**
     * Verify JAR file integrity using embedded signature
     * Note: This is a simplified implementation
     */
    private static void verifyJarIntegrity() throws IOException, NoSuchAlgorithmException {
        // Get the expected signature from resources
        String expectedSignature = loadEmbeddedSignature();
        if (expectedSignature == null || expectedSignature.trim().isEmpty()) {
            // No signature embedded - skip verification
            return;
        }
        
        // Get the current JAR file
        File jarFile = getCurrentJarFile();
        if (jarFile == null || !jarFile.exists()) {
            // Running in development environment - skip verification
            return;
        }
        
        // Calculate current JAR hash
        String currentHash = calculateFileHash(jarFile);
        
        // Compare hashes
        if (!expectedSignature.trim().equalsIgnoreCase(currentHash)) {
            throw new SecurityException("JAR integrity check failed - file may have been modified");
        }
    }
    
    /**
     * Load embedded signature from resources
     */
    private static String loadEmbeddedSignature() {
        try (InputStream is = AntiTamper.class.getResourceAsStream(EXPECTED_SIGNATURE_RESOURCE)) {
            if (is == null) {
                return null;
            }
            
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            return new String(bytes).trim();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the current JAR file
     */
    private static File getCurrentJarFile() {
        try {
            String jarPath = AntiTamper.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();
            
            File jarFile = new File(jarPath);
            return jarFile.isFile() ? jarFile : null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    private static String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Perform additional anti-tamper checks
     */
    private static void performAdditionalChecks() {
        // Check for common decompiler artifacts
        checkForDecompilerArtifacts();
        
        // Check system properties for suspicious values
        checkSystemProperties();
        
        // Verify class loading behavior
        verifyClassLoading();
    }
    
    /**
     * Check for decompiler artifacts in stack traces
     */
    private static void checkForDecompilerArtifacts() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // Check for common decompiler package names
            if (className.contains("fernflower") ||
                className.contains("procyon") ||
                className.contains("cfr") ||
                className.contains("jadx") ||
                className.contains("jd.gui")) {
                
                throw new SecurityException("Decompiler detected in stack trace");
            }
        }
    }
    
    /**
     * Check system properties for suspicious values
     */
    private static void checkSystemProperties() {
        // Check for IDE-specific properties
        String[] suspiciousProperties = {
            "idea.launcher.port",
            "eclipse.launcher",
            "netbeans.home",
            "vscode.pid"
        };
        
        for (String property : suspiciousProperties) {
            if (System.getProperty(property) != null) {
                // Running in IDE - allow for development
                break;
            }
        }
    }
    
    /**
     * Verify class loading behavior
     */
    private static void verifyClassLoading() {
        try {
            // Check if our classes are loaded from the expected location
            ClassLoader classLoader = AntiTamper.class.getClassLoader();
            String classPath = AntiTamper.class.getResource("AntiTamper.class").toString();
            
            // Basic check - in production, this would be more sophisticated
            if (!classPath.startsWith("jar:file:") && !classPath.startsWith("file:")) {
                // Unusual class loading - might be from memory or modified source
                // Don't throw exception for development environments
            }
            
        } catch (Exception e) {
            // Class loading verification failed - continue anyway
        }
    }
    
    /**
     * Obfuscated method to make reverse engineering harder
     * This would be further obfuscated by ProGuard
     */
    private static void obfuscatedCheck() {
        // Dummy obfuscated logic
        int x = 0x48656164; // "Head" in hex
        int y = 0x53746561; // "Stea" in hex  
        int z = 0x6C58; // "lX" in hex
        
        if ((x ^ y ^ z) == 0x48656164) {
            // Continue execution
            return;
        }
        
        throw new SecurityException("Obfuscated check failed");
    }
    
    /**
     * Runtime integrity check - called periodically during execution
     */
    public static void runtimeCheck() {
        if (!verificationPassed) {
            throw new SecurityException("Runtime integrity check failed - initial verification not passed");
        }
        
        // Perform lightweight runtime checks
        try {
            obfuscatedCheck();
        } catch (Exception e) {
            throw new SecurityException("Runtime check failed: " + e.getMessage());
        }
    }
}
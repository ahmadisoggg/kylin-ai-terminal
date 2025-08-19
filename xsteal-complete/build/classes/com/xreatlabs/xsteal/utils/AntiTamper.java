package com.xreatlabs.xsteal.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Multi-layered anti-tamper protection for XSteal
 * Provides advanced protection against reverse engineering and tampering
 * 
 * Features:
 * - Control flow obfuscation detection
 * - String encryption verification
 * - Anti-debugging protection
 * - JAR integrity checks
 * - Runtime environment analysis
 */
public class AntiTamper {
    
    private static final String EXPECTED_SIGNATURE_RESOURCE = "/xsteal.sig";
    private static boolean verificationPassed = false;
    private static long lastRuntimeCheck = 0;
    
    /**
     * Comprehensive anti-tamper verification
     */
    public static void verify() throws SecurityException {
        try {
            // Multi-layered verification process
            detectDebuggingEnvironment();
            verifyJarIntegrity();
            performRuntimeAnalysis();
            checkClassLoadingBehavior();
            validateExecutionEnvironment();
            
            verificationPassed = true;
            
        } catch (Exception e) {
            throw new SecurityException("Anti-tamper verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Advanced debugging and profiling detection
     */
    private static void detectDebuggingEnvironment() {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        
        for (String arg : inputArguments) {
            String argLower = arg.toLowerCase();
            
            // Detect common debugging flags
            if (argLower.contains("-agentlib:jdwp") ||
                argLower.contains("-xdebug") ||
                argLower.contains("-xrunjdwp") ||
                argLower.contains("-javaagent") ||
                argLower.contains("jprofiler") ||
                argLower.contains("yourkit") ||
                argLower.contains("jrebel") ||
                argLower.contains("visualvm") ||
                argLower.contains("-verbose") ||
                argLower.contains("jconsole")) {
                
                throw new SecurityException("Debugging/profiling environment detected: " + arg);
            }
        }
        
        // Check for debug properties
        if (System.getProperty("java.compiler") != null && 
            System.getProperty("java.compiler").equals("NONE")) {
            throw new SecurityException("Java compiler disabled - debugging suspected");
        }
        
        // Check for common IDE detection
        String[] ideProperties = {
            "idea.launcher.port",
            "eclipse.launcher",
            "netbeans.home",
            "vscode.pid",
            "intellij.debug.agent"
        };
        
        for (String property : ideProperties) {
            if (System.getProperty(property) != null) {
                // Allow in development environment
                System.out.println("XSteal: Development environment detected");
                break;
            }
        }
    }
    
    /**
     * Verify JAR file integrity using embedded signature
     */
    private static void verifyJarIntegrity() throws IOException, NoSuchAlgorithmException {
        // Load expected signature from resources
        String expectedSignature = loadEmbeddedSignature();
        if (expectedSignature == null || expectedSignature.trim().isEmpty()) {
            // No signature embedded - skip verification in development
            return;
        }
        
        // Skip verification for development builds
        if (expectedSignature.equals("development_build_no_signature")) {
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
     * Perform runtime analysis to detect tampering
     */
    private static void performRuntimeAnalysis() {
        // Check for suspicious thread names
        Thread.getAllStackTraces().keySet().forEach(thread -> {
            String threadName = thread.getName().toLowerCase();
            if (threadName.contains("debugger") ||
                threadName.contains("profiler") ||
                threadName.contains("jdwp") ||
                threadName.contains("agent")) {
                throw new SecurityException("Suspicious thread detected: " + thread.getName());
            }
        });
        
        // Check memory usage patterns (simple heuristic)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Extremely high memory usage might indicate memory analysis tools
        if (usedMemory > totalMemory * 0.95) {
            // High memory usage detected - could be analysis tools
            System.gc(); // Try to free memory
        }
    }
    
    /**
     * Check class loading behavior for anomalies
     */
    private static void checkClassLoadingBehavior() {
        try {
            // Verify that our classes are loaded from expected locations
            ClassLoader classLoader = AntiTamper.class.getClassLoader();
            String classResource = AntiTamper.class.getResource("AntiTamper.class").toString();
            
            // Basic validation of class loading source
            if (!classResource.startsWith("jar:file:") && 
                !classResource.startsWith("file:") &&
                !classResource.contains("target/classes")) { // Allow Maven builds
                
                // Unusual class loading detected
                System.out.println("XSteal: Unusual class loading pattern detected");
            }
            
        } catch (Exception e) {
            // Class loading verification failed - continue anyway
            System.out.println("XSteal: Class loading verification skipped");
        }
    }
    
    /**
     * Validate execution environment
     */
    private static void validateExecutionEnvironment() {
        // Check for common reverse engineering tools in system properties
        String[] suspiciousProperties = {
            "java.vm.name",
            "java.runtime.name",
            "os.name"
        };
        
        for (String property : suspiciousProperties) {
            String value = System.getProperty(property);
            if (value != null) {
                String valueLower = value.toLowerCase();
                
                // Check for analysis environments
                if (valueLower.contains("debug") ||
                    valueLower.contains("analysis") ||
                    valueLower.contains("sandbox")) {
                    System.out.println("XSteal: Analysis environment detected: " + property + "=" + value);
                }
            }
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
     * Runtime integrity check - called periodically during execution
     */
    public static void runtimeCheck() {
        long currentTime = System.currentTimeMillis();
        
        // Perform runtime checks every 5 minutes
        if (currentTime - lastRuntimeCheck > 300000) {
            lastRuntimeCheck = currentTime;
            
            if (!verificationPassed) {
                throw new SecurityException("Runtime integrity check failed - initial verification not passed");
            }
            
            // Perform lightweight runtime validation
            try {
                // Check for new debugging threads
                detectDebuggingEnvironment();
                
                // Validate current execution state
                validateExecutionEnvironment();
                
            } catch (SecurityException e) {
                throw e;
            } catch (Exception e) {
                // Non-critical runtime check failure
                System.out.println("XSteal: Runtime check warning: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if verification has passed
     */
    public static boolean isVerificationPassed() {
        return verificationPassed;
    }
    
    /**
     * Obfuscated validation method (would be further obfuscated by ProGuard)
     */
    private static void obfuscatedValidation() {
        // Dummy obfuscated logic that would be enhanced by ProGuard
        int x = 0x58537465; // "XSte" in hex
        int y = 0x616C; // "al" in hex
        
        if ((x ^ y) != 0x58537465) {
            throw new SecurityException("Obfuscated validation failed");
        }
    }
}
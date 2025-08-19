package com.headstealx;

import com.headstealx.managers.AbilityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbilityManager
 */
public class AbilityManagerTest {
    
    @Mock
    private Main plugin;
    
    private AbilityManager abilityManager;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        abilityManager = new AbilityManager(plugin);
    }
    
    @Test
    public void testAbilityManagerInitialization() {
        assertNotNull(abilityManager);
        assertEquals(0, abilityManager.getRegisteredAbilityCount());
        assertEquals(0, abilityManager.getActiveAbilityCount());
    }
    
    @Test
    public void testRegisterAbilities() {
        abilityManager.registerAbilities();
        assertTrue(abilityManager.getRegisteredAbilityCount() > 0);
    }
    
    @Test
    public void testGetRegisteredAbilityTypes() {
        abilityManager.registerAbilities();
        var abilityTypes = abilityManager.getRegisteredAbilityTypes();
        assertNotNull(abilityTypes);
        assertFalse(abilityTypes.isEmpty());
        assertTrue(abilityTypes.contains("lifesteal"));
        assertTrue(abilityTypes.contains("arrow_spread"));
    }
    
    @Test
    public void testIsAbilityRegistered() {
        abilityManager.registerAbilities();
        assertTrue(abilityManager.isAbilityRegistered("lifesteal"));
        assertFalse(abilityManager.isAbilityRegistered("nonexistent_ability"));
    }
    
    // Additional tests would be added here for:
    // - Ability execution
    // - Cooldown management
    // - Boss ability combos
    // - Context validation
    // - Error handling
}
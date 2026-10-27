package com.example.unit;

import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.example.config.ConfigReader;

/** Framework unit tests - no browser or backend. Keys are namespaced so no env var can shadow them. */
public class ConfigReaderTest {

    private static final String KEY = "qaunit.configreader.key";
    private static final String MISSING_KEY = "qaunit.configreader.missing";

    @AfterMethod(alwaysRun = true)
    public void clearProperty() {
        System.clearProperty(KEY);
    }

    @Test
    public void systemPropertyWinsAndIsTrimmed() {
        System.setProperty(KEY, "  from-system  ");
        Assert.assertEquals(ConfigReader.get(KEY), "from-system");
        Assert.assertEquals(ConfigReader.get(KEY, "fallback"), "from-system");
    }

    @Test
    public void fallsBackToConfigFile() {
        Assert.assertTrue(ConfigReader.get("baseUrl").startsWith("http"), "baseUrl should come from config.properties");
    }

    @Test
    public void blankSystemPropertyIsIgnored() {
        System.setProperty(KEY, "   ");
        Assert.assertEquals(ConfigReader.get(KEY, "fallback"), "fallback");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void missingRequiredKeyThrows() {
        ConfigReader.get(MISSING_KEY);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullKeyThrows() {
        ConfigReader.get(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void nullDefaultForMissingKeyThrows() {
        ConfigReader.get(MISSING_KEY, null);
    }

    @Test
    public void getIntParsesValueOrUsesDefault() {
        Assert.assertEquals(ConfigReader.getInt(MISSING_KEY, 7), 7);
        System.setProperty(KEY, "42");
        Assert.assertEquals(ConfigReader.getInt(KEY, 7), 42);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getIntRejectsNonNumber() {
        System.setProperty(KEY, "forty-two");
        ConfigReader.getInt(KEY, 7);
    }

    @Test
    public void getBooleanIsCaseInsensitive() {
        System.setProperty(KEY, "TRUE");
        Assert.assertTrue(ConfigReader.getBoolean(KEY, false));
        System.setProperty(KEY, "False");
        Assert.assertFalse(ConfigReader.getBoolean(KEY, true));
        Assert.assertTrue(ConfigReader.getBoolean(MISSING_KEY, true));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getBooleanRejectsOtherValues() {
        System.setProperty(KEY, "yes");
        ConfigReader.getBoolean(KEY, false);
    }

    @Test
    public void getDurationParsesIsoValueOrUsesDefault() {
        Duration fallback = Duration.ofSeconds(3);
        Assert.assertEquals(ConfigReader.getDuration(MISSING_KEY, fallback), fallback);
        System.setProperty(KEY, "PT2S");
        Assert.assertEquals(ConfigReader.getDuration(KEY, fallback), Duration.ofSeconds(2));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getDurationRejectsZero() {
        System.setProperty(KEY, "PT0S");
        ConfigReader.getDuration(KEY, Duration.ofSeconds(1));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getDurationRejectsGarbage() {
        System.setProperty(KEY, "soon");
        ConfigReader.getDuration(KEY, Duration.ofSeconds(1));
    }
}

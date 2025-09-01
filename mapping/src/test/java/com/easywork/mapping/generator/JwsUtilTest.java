package com.easywork.mapping.generator;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

public class JwsUtilTest {

    @Test
    void testSignAndVerifyAttached() throws JOSEException, ParseException, NoSuchAlgorithmException {
        String payload = "Hello, JWS!";
        String key = JwsUtil.generateHMACKey(256);

        // Attached mode
        String jws = JwsUtil.sign(payload, key, false);
        System.out.println("Attached JWS: " + jws);

        assertTrue(JwsUtil.verify(jws, null, key));
    }

    @Test
    void testSignAndVerifyDetached() throws JOSEException, ParseException, NoSuchAlgorithmException {
        String payload = "Hello, detached JWS!";
        String key = JwsUtil.generateHMACKey(256);

        // Detached mode
        String jws = JwsUtil.sign(payload, key, true);
        System.out.println("Detached JWS: " + jws);

        assertTrue(JwsUtil.verify(jws, payload, key));
    }

    @Test
    void testVerifyWithWrongKey() throws JOSEException, ParseException, NoSuchAlgorithmException {
        String payload = "Hello, JWS!";
        String key = JwsUtil.generateHMACKey(256);
        String wrongKey = JwsUtil.generateHMACKey(256);

        String jws = JwsUtil.sign(payload, key, false);

        assertFalse(JwsUtil.verify(jws, null, wrongKey));
    }

    @Test
    void testVerifyDetachedWithWrongPayload() throws JOSEException, ParseException, NoSuchAlgorithmException {
        String payload = "Hello, detached JWS!";
        String wrongPayload = "Wrong payload!";
        String key = JwsUtil.generateHMACKey(256);

        String jws = JwsUtil.sign(payload, key, true);

        assertFalse(JwsUtil.verify(jws, wrongPayload, key));
    }

    @Test
    void testGenerateHMACKey() throws NoSuchAlgorithmException {
        String key1 = JwsUtil.generateHMACKey(256);
        String key2 = JwsUtil.generateHMACKey(256);

        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2);
        assertEquals(43, key1.length()); // Base64Url encoded 256-bit key length
    }
}
// src/main/java/com/odp/simulator/client/crypto/OdpPasswordEncryptor.java
package com.odp.simulator.client.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Password encryptor for ODP protocol
 * 
 * Encryption requirements (Section 4.4 & 4.5):
 * 1. Prefix the password with login time in UTC format (YYYYMMDDHHMMSS)
 * 2. Encrypt using 2048-bit RSA with OAEP padding (PKCS#1)
 * 3. Encode the binary output in Base64
 * 
 * The password format before encryption: YYYYMMDDHHMMSS + password
 * For example: "20231215143052Aa123"
 */
@Slf4j
@Component
public class OdpPasswordEncryptor {

    private static final String RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final DateTimeFormatter UTC_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final ResourceLoader resourceLoader;
    private PublicKey publicKey;

    public OdpPasswordEncryptor(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Load the RSA public key from the specified path
     */
    public void loadPublicKey(String publicKeyPath) throws Exception {
        log.info("Loading RSA public key from: {}", publicKeyPath);
        
        Resource resource = resourceLoader.getResource(publicKeyPath);
        if (!resource.exists()) {
            throw new IllegalStateException("Public key file not found: " + publicKeyPath);
        }

        try (PEMParser pemParser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            Object object = pemParser.readObject();
            
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            
            if (object instanceof SubjectPublicKeyInfo) {
                this.publicKey = converter.getPublicKey((SubjectPublicKeyInfo) object);
            } else {
                throw new IllegalStateException("Unsupported key format: " + object.getClass().getName());
            }
            
            if (this.publicKey instanceof RSAPublicKey rsaKey) {
                int keySize = rsaKey.getModulus().bitLength();
                log.info("Loaded RSA public key, size: {} bits", keySize);
                if (keySize < 2048) {
                    log.warn("RSA key size is less than 2048 bits, which may not meet security requirements");
                }
            }
        }
    }

    /**
     * Encrypt the password according to ODP protocol requirements
     * 
     * @param password The plain text password
     * @return Base64 encoded encrypted password with login time prefix
     */
    public String encryptPassword(String password) throws Exception {
        if (publicKey == null) {
            throw new IllegalStateException("Public key not loaded. Call loadPublicKey() first.");
        }

        // Step 1: Prefix the password with login time in UTC (YYYYMMDDHHMMSS)
        String loginTime = UTC_FORMATTER.format(Instant.now());
        String prefixedPassword = loginTime + password;
        
        log.debug("Password with login time prefix: {} (password masked)", loginTime + "****");

        // Step 2: Encrypt using RSA-OAEP
        Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        byte[] plainBytes = prefixedPassword.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(plainBytes);

        // Step 3: Encode in Base64
        String base64Encoded = Base64.getEncoder().encodeToString(encryptedBytes);
        
        log.debug("Encrypted password length: {} bytes, Base64 length: {}", 
                encryptedBytes.length, base64Encoded.length());

        return base64Encoded;
    }

    /**
     * Get the current login time in UTC format
     */
    public String getCurrentLoginTime() {
        return UTC_FORMATTER.format(Instant.now());
    }

    /**
     * Check if the public key is loaded
     */
    public boolean isKeyLoaded() {
        return publicKey != null;
    }
}
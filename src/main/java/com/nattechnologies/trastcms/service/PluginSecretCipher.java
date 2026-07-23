package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component
public class PluginSecretCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PluginSecretCipher(TrastCmsProperties properties) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(properties.getPlugins().getMasterKey().getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo cifrar el secreto del plugin", exception);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] value = Base64.getDecoder().decode(encryptedText);
            byte[] iv = Arrays.copyOfRange(value, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(value, 12, value.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No se pudo descifrar el secreto del plugin", exception);
        }
    }
}

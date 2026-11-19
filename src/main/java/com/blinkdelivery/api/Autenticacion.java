package com.blinkdelivery.api;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cifrado de contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria.
 * Nunca se guarda la contraseña en texto plano: solo la sal y el hash derivado.
 */
final class Autenticacion {

    private static final int ITERACIONES = 120_000;
    private static final int BITS_CLAVE = 256;
    private static final int BYTES_SAL = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Autenticacion() {
    }

    /** @return {sal en Base64, hash en Base64} */
    static String[] cifrar(String clave) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(clave, sal);
        return new String[] {
            Base64.getEncoder().encodeToString(sal),
            Base64.getEncoder().encodeToString(hash)
        };
    }

    /** Compara en tiempo constante para no revelar información por tiempos de respuesta. */
    static boolean verificar(String clave, String saltBase64, String hashBase64) {
        byte[] sal = Base64.getDecoder().decode(saltBase64);
        byte[] esperado = Base64.getDecoder().decode(hashBase64);
        return MessageDigest.isEqual(derivar(clave, sal), esperado);
    }

    private static byte[] derivar(String clave, byte[] sal) {
        try {
            PBEKeySpec especificacion = new PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, BITS_CLAVE);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar la contraseña.", e);
        }
    }
}

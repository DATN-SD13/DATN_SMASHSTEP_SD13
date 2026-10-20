package com.smashstep.datn.employee.util;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Component
public class NhanVienImportMailCipher {
    private final SecretKeySpec key;
    private final SecureRandom random=new SecureRandom();
    public NhanVienImportMailCipher(@Value("${app.employee-import.mail-secret:${app.jwt.secret}}") String secret) {
        if(secret==null || secret.length()<32)throw new IllegalArgumentException("Employee import mail key must have at least 32 characters");
        try { key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(("smashstep:employee-mail:v1:"+secret).getBytes(StandardCharsets.UTF_8)),"AES"); }
        catch(GeneralSecurityException e){throw new IllegalStateException("Cannot initialize mail encryption");}
    }
    public String encrypt(String password,String employeeCode) {
        try { byte[] iv=new byte[12];random.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));c.updateAAD(employeeCode.getBytes(StandardCharsets.UTF_8));byte[] encrypted=c.doFinal(password.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(iv)+"."+Base64.getEncoder().encodeToString(encrypted); }
        catch(GeneralSecurityException e){throw new IllegalStateException("Cannot protect account email");}
    }
    public String decrypt(String encrypted,String employeeCode) {
        try { String[] p=encrypted.split("\\.",-1);if(p.length!=2)throw new IllegalArgumentException();byte[] iv=Base64.getDecoder().decode(p[0]);if(iv.length!=12)throw new IllegalArgumentException();Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));c.updateAAD(employeeCode.getBytes(StandardCharsets.UTF_8));return new String(c.doFinal(Base64.getDecoder().decode(p[1])),StandardCharsets.UTF_8); }
        catch(GeneralSecurityException|IllegalArgumentException|NullPointerException e){throw new IllegalStateException("Cannot open pending account email");}
    }
}

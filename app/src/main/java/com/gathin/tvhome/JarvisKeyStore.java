package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Only ciphertext is persisted. Keystore does not protect against a compromised running device. */
final class JarvisKeyStore {
    interface KeySource { SecretKey get() throws Exception; }
    static final String PREFS="jarvis-direct-ai", ALIAS="fayyadhos.jarvis.groq.v1";
    private static final Object LOCK=new Object();
    private final SharedPreferences prefs;
    private final KeySource keys;
    private final byte[] aad;
    JarvisKeyStore(Context c){this(c,JarvisKeyStore::androidKey);}
    JarvisKeyStore(Context c,KeySource keys){prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);this.keys=keys;aad=(c.getPackageName()+":groq:v1").getBytes(StandardCharsets.UTF_8);}
    private static synchronized SecretKey androidKey()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    boolean configured(){return prefs.contains("ciphertext")||prefs.contains("iv");}
    long revision(){return prefs.getLong("revision",0);}
    String model(){return prefs.getString("model","");}
    long testedAt(){return prefs.getLong("tested_at",0);}
    String read()throws StorageFailure{synchronized(LOCK){
        if(!configured())return "";
        try{
            byte[] iv=Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP);
            byte[] encrypted=Base64.decode(prefs.getString("ciphertext",""),Base64.NO_WRAP);
            if(iv.length!=12||encrypted.length<16||encrypted.length>8192)throw new IOException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,keys.get(),new GCMParameterSpec(128,iv));cipher.updateAAD(aad);
            return new String(cipher.doFinal(encrypted),StandardCharsets.UTF_8);
        }catch(Exception e){throw new StorageFailure();}
    }}
    void save(String value)throws StorageFailure{synchronized(LOCK){
        if(value==null||value.trim().isEmpty()||value.length()>4096)throw new StorageFailure();
        String key=value.trim();for(int i=0;i<key.length();i++)if(key.charAt(i)<33||key.charAt(i)>126)throw new StorageFailure();
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,keys.get());cipher.updateAAD(aad);
            String encrypted=Base64.encodeToString(cipher.doFinal(key.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
            if(!prefs.edit().putString("ciphertext",encrypted).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                .remove("model").remove("tested_at").putLong("revision",revision()+1).commit())throw new IOException();
        }catch(Exception e){throw new StorageFailure();}
    }}
    void selectModel(String model)throws StorageFailure{synchronized(LOCK){
        if(!configured()||model==null||model.isEmpty()||model.length()>200)throw new StorageFailure();
        if(!prefs.edit().putString("model",model).remove("tested_at").putLong("revision",revision()+1).commit())throw new StorageFailure();
    }}
    void remove()throws StorageFailure{synchronized(LOCK){
        if(!prefs.edit().remove("ciphertext").remove("iv").remove("model").remove("tested_at").putLong("revision",revision()+1).commit())throw new StorageFailure();
    }}
    boolean markTested(long expectedRevision,String expectedModel){synchronized(LOCK){
        if(!configured()||revision()!=expectedRevision||!model().equals(expectedModel))return false;
        return prefs.edit().putLong("tested_at",System.currentTimeMillis()).commit();
    }}
    void invalidateTest(){prefs.edit().remove("tested_at").apply();}
    static final class StorageFailure extends IOException {
        StorageFailure(){super("Secure key storage is unavailable or unreadable. Re-enter the key, or remove the saved key. No plaintext fallback is used.");}
    }
}

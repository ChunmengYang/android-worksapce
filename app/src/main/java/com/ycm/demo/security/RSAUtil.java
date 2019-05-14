package com.ycm.demo.security;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
 
 
/**
 * <p>
 * RSA公钥/私钥/签名工具包
 * </p>
 * <p>
 * 罗纳德·李维斯特（Ron [R]ivest）、阿迪·萨莫尔（Adi [S]hamir）和伦纳德·阿德曼（Leonard [A]dleman）
 * </p>
 * <p>
 * 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 * 
 * @author IceWee
 * @date 2012-4-26
 * @version 1.0
 */
public class RSAUtil {
	/**
	 * 接口传输数据加密公钥、私钥  
	 */
	public static final String API_PUBLICKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArzWM5WS1kSlD/53/JhCV3izrQGbQEjBsZJ/GIoEhh5C7ovSdWNwLiB40eIESRefnHUQZ9JtYZmkIyDeafU4SVhZsEKeYNjIhF1G9kLjVwOMB0++JBIAuuTgUgjllCc0llFvQCMBUOSUVb4pebb86F+q0ttS5GhiDe/oFMtpXLQx0g6QzxEHMJc37RQ44En0c6rgVXHt1jz2HuNyxm9dXQwVb9ovj7ayXUcxKauhCEuT15XTUBvxtxO1vq+1n4wC4yARGzaJliCEJscMa1vb+Po6GQjM9bzwU8klAlbbMDhm46y3k8k4GON+tJ29aWPlAssgZK72KtK8DE07VJFQW2QIDAQAB";
    public static final String API_PRIVATEKEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvNYzlZLWRKUP/nf8mEJXeLOtAZtASMGxkn8YigSGHkLui9J1Y3AuIHjR4gRJF5+cdRBn0m1hmaQjIN5p9ThJWFmwQp5g2MiEXUb2QuNXA4wHT74kEgC65OBSCOWUJzSWUW9AIwFQ5JRVvil5tvzoX6rS21LkaGIN7+gUy2lctDHSDpDPEQcwlzftFDjgSfRzquBVce3WPPYe43LGb11dDBVv2i+PtrJdRzEpq6EIS5PXldNQG/G3E7W+r7WfjALjIBEbNomWIIQmxwxrW9v4+joZCMz1vPBTySUCVtswOGbjrLeTyTgY4360nb1pY+UCyyBkrvYq0rwMTTtUkVBbZAgMBAAECggEADPoUY3EgGY5K4QXcvf8AeqItEmoLAy3e+Vz0Gd4ik53ep+UiOlCh8gT6seotZkwUzBAdNAbFd2BULDAiGyHLLeEfQ+SF242+8bE2Lx/hhRXLLt1Q8qpjy4ghzLtfhmhRXjmmoN9N3aWnfslq0QoWzWqu09PRmeMy6d3MwMV3K3jfGiOSZje5ac7LRBzRqYlzU8J8pNbjyC0qnZbtcFf0Mx8OPHXYxwdHgDmxLE+Z4RpuJEQRHKqYkg4Ai5Sz26A07q8LZXjktO5afrBQNU0x6PkRHwzaMHK5sQ0fMjUsW16Nlucwbsj+OtM7wJUrUZqv3N0FcW9cOJYk9VJONEuRYQKBgQDuB9/6dX1Bbzw44xa7gcIvQSfMo2wETlNvXzqep4Ijs/ZReMwNpTrPQ7S5h1KdUwHYGfRJgUtxC2H+E/5zKEEpO/UI7CS6eURcZYutQTvjcO9n8vGH9d0y21FsioHus1d9vzk6W0Ya79ixeClT/SweWq+3Njz/DsNxfARJehGoJQKBgQC8b5nK4ZbjclAYFxJKVo1Qv+9n4AHNfgr2joCwKHjTMuiTHQUSd0ZLuGEeBWcd6LENnqmACoGPuse/JK71jlZ5I2yxWqwR6QtUUrax9MzC7fPQWotfwXiB51qLZ0VlLyXA8vSYjeJrjv0knuAi5yPd6UtrN5iB8l+56Zq8QnqrpQKBgF8+53Dg8m3shLx+oeoF0h32hGZuPhq6/Mfj4yD5BiojKL0RCRWsuAuXnAlhDL3HKUW/nPCMvBNP6rYwafDKesh1JWecllXHkIMoXuvE3qHz3thJbjxpNUnUWJCGG9fYC3pVuiCPxU2iC6N34ZGqDK1YvFcjbOTik3IlndGI9ufBAoGAL6PjdPiNxlkrSLvkgujY93ZfF5r5ubNJIEas4FyLF5JCXJc0phODsTcfTpC9HkGrKa9YWSfO08C8KWYj2a8vy5aaL0imQizyEVKMH/etIQc54g5SOFZYmjccnCvyzn8aA2spTbxpZxoShASq1Agt+De4OpYmt4vIid82oKwKCYUCgYB9gJK2ccnJr+WCEALDOfn4Ook+WNzsgVL/lGF6P7sL923Dq2FZE5/QTi/1reVUjScjEKK0mX+TjLWfbJAaj0aGp+BBmgdgUporaWAVzZqp9eqrZCTdcEx2uJv9Z90HbDhClyxtyheCxjUmOLy5cIZKCyy6ln5JJ8n/LlFHisYOog==";

    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";
    
    /**
     * 签名算法
     */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";
    
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 245;
    
    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 256;
 
    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     * 
     * @param data 已加密数据
     * @param privateKey 私钥(BASE64编码)
     * 
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
        byte[] keyBytes = Base64Utils.decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return Base64Utils.encode(signature.sign());
    }
 
 
    /**
     * <p>
     * 校验数字签名
     * </p>
     * 
     * @param data 已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @param sign 数字签名
     * 
     * @return
     * @throws Exception
     * 
     */
    public static boolean verify(byte[] data, String publicKey, String sign) throws Exception {
        byte[] keyBytes = Base64Utils.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicK);
        signature.update(data);
        return signature.verify(Base64Utils.decode(sign));
    }
 
 
    /**
     * <P>
     * 私钥解密
     * </p>
     * 
     * @param encryptedData 已加密数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey) throws Exception {
        byte[] keyBytes = Base64Utils.decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }
 
    /**
     * <p>
     * 公钥解密
     * </p>
     * 
     * @param encryptedData 已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey) throws Exception {
        byte[] keyBytes = Base64Utils.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }
 
 
    /**
     * <p>
     * 公钥加密
     * </p>
     * 
     * @param data 源数据
     * @param publicKey 公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
        byte[] keyBytes = Base64Utils.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        // 对数据加密
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }
 
 
    /**
     * <p>
     * 私钥加密
     * </p>
     * 
     * @param data 源数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPrivateKey(byte[] data, String privateKey)throws Exception {
        byte[] keyBytes = Base64Utils.decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, privateK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }
 
 
}



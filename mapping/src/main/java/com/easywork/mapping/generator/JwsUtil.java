package com.easywork.mapping.generator;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;

public class JwsUtil {

    /**
     * 生成JWS签名。
     * @param payload 待签名的数据。
     * @param key 签名密钥（HMAC密钥）。
     * @param detached 是否使用分离模式（payload不包含在JWS字符串中）。
     * @return JWS字符串。
     * @throws JOSEException 如果签名过程中发生错误。
     */
    public static String sign(String payload, String key, boolean detached) throws JOSEException {
        // 创建HMAC密钥
        OctetSequenceKey hmacJWK = new OctetSequenceKey.Builder(key.getBytes())
                .algorithm(JWSAlgorithm.HS256) // 假设使用HS256算法
                .build();

        // 创建JWS头部
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.HS256);
        if (detached) {
            headerBuilder.base64URLEncodePayload(false);
            headerBuilder.criticalParams(java.util.Collections.singleton("b64"));
        }
        JWSHeader header = headerBuilder.build();

        // 创建Payload
        Payload jwsPayload = new Payload(payload);

        // 创建JWS对象
        JWSObject jwsObject = new JWSObject(header, jwsPayload);

        // 签名
        jwsObject.sign(new MACSigner(hmacJWK));

        // 序列化JWS对象
        return jwsObject.serialize(detached);
    }

    /**
     * 验证JWS签名。
     * @param jws JWS字符串。
     * @param payload 原始payload数据（如果JWS是分离模式，则必须提供）。
     * @param key 验证密钥（HMAC密钥）。
     * @return 验证是否成功。
     * @throws ParseException 如果JWS字符串解析失败。
     * @throws JOSEException 如果验证过程中发生错误。
     */
    public static boolean verify(String jws, String payload, String key) throws ParseException, JOSEException {
        // 创建HMAC密钥
        OctetSequenceKey hmacJWK = new OctetSequenceKey.Builder(key.getBytes())
                .algorithm(JWSAlgorithm.HS256) // 假设使用HS256算法
                .build();

        JWSObject jwsObject;
        if (jws.split("\\.").length == 3 && jws.split("\\.")[1].isEmpty()) {
            // 分离模式，需要提供原始payload
            jwsObject = JWSObject.parse(jws, new Payload(payload));
        } else {
            // 非分离模式，payload包含在JWS中
            jwsObject = JWSObject.parse(jws);
        }

        // 验证签名
        return jwsObject.verify(new MACVerifier(hmacJWK));
    }

    /**
     * 生成一个随机的HMAC密钥。
     * @param bitLength 密钥的位长度（例如：256位）。
     * @return 生成的密钥字符串。
     * @throws NoSuchAlgorithmException 如果不支持指定的算法。
     */
    public static String generateHMACKey(int bitLength) throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] keyBytes = new byte[bitLength / 8];
        random.nextBytes(keyBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}
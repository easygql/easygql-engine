package com.easygql.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;

/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Slf4j
public class JwtUtil {
    /**
     * JWT 加解密类型
     */
    private static final SignatureAlgorithm JWT_ALG = SignatureAlgorithm.HS256;
    /**
     * JWT 生成密钥使用的密码
     */
    private static final String JWT_RULE = "easygqltoken";

    /**
     * JWT 添加至HTTP HEAD中的前缀
     */
    private static final String JWT_SEPARATOR = "Bearer ";

    /**
     * 使用JWT默认方式，生成加解密密钥
     *
     * @param alg 加解密类型
     * @return
     */
    public static SecretKey generateKey(SignatureAlgorithm alg) {
        return MacProvider.generateKey(alg);
    }

    /**
     * 使用指定密钥生成规则，生成JWT加解密密钥
     *
     * @param alg  加解密类型
     * @param rule 密钥生成规则
     * @return
     */
    public static SecretKey generateKey(SignatureAlgorithm alg, String rule) {
        // 将密钥生成键转换为字节数组
        byte[] bytes = Base64.decodeBase64(rule);
        // 根据指定的加密方式，生成密钥
        return new SecretKeySpec(bytes, alg.getJcaName());
    }

    /**
     * 构建JWT
     *
     * @param alg      jwt 加密算法
     * @param key      jwt 加密密钥
     * @param sub      jwt 面向的用户
     * @param aud      jwt 接收方
     * @param jti      jwt 唯一身份标识
     * @param iss      jwt 签发者
     * @param nbf      jwt 生效日期时间
     * @param duration jwt 有效时间，单位：秒
     * @return JWT字符串
     */
    public static String buildJWT(SignatureAlgorithm alg, Key key, String sub, String aud, String jti, String iss, Date nbf, HashMap<String, Object> userObject, Integer duration) {
        // jwt的签发时间
        DateTime iat = DateTime.now();
        // jwt的过期时间，这个过期时间必须要大于签发时间
        DateTime exp = null;
        if (null != duration) {
            exp = (nbf == null ? iat.plusSeconds(duration) : new DateTime(nbf).plusSeconds(duration));
        }

        // 获取JWT字符串
        String compact = Jwts.builder()
                .signWith(alg, key)
                .setSubject(sub)
                .setAudience(aud)
                .setId(jti)
                .setIssuer(iss)
                .setNotBefore(nbf)
                .setIssuedAt(iat.toDate())
                .setExpiration(exp != null ? exp.toDate() : null)
                .setClaims(userObject)
                .compact();

        // 在JWT字符串前添加"Bearer "字符串，用于加入"Authorization"请求头
        return JWT_SEPARATOR + compact;
    }

    /**
     * 构建JWT
     *
     * @param sub      jwt 面向的用户
     * @param aud      jwt 接收方
     * @param jti      jwt 唯一身份标识
     * @param iss      jwt 签发者
     * @param nbf      jwt 生效日期时间
     * @param duration jwt 有效时间，单位：秒
     * @return JWT字符串
     */
    public static String buildJWT(String sub, String aud, String jti, String iss, Date nbf, HashMap<String, Object> userobject, Integer duration) {
        return buildJWT(JWT_ALG, generateKey(JWT_ALG, JWT_RULE), sub, aud, jti, iss, nbf, userobject, duration);
    }

    /**
     * 构建JWT
     *
     * @param userInfo 用户信息
     * @param duration 持续时间
     * @return JWT字符串
     */
    public static String buildJWT(HashMap<String, Object> userInfo, Integer duration) {
        return buildJWT(String.valueOf(userInfo.get("username")), null, String.valueOf(userInfo.get("id")), null, null, userInfo, duration);
    }

    /**
     * 构建JWT
     *
     * @param userInfo 用户信息
     * @return JWT字符串
     */
    public static String buildJWT(HashMap<String, Object> userInfo) {
        return buildJWT(String.valueOf(userInfo.get("username")), null, String.valueOf(userInfo.get("id")), null, null, userInfo, 600);
    }

    /**
     * 解析JWT
     *
     * @param key       jwt 加密密钥
     * @param claimsJws jwt 内容文本
     * @return {@link Jws}
     * @throws Exception
     */
    public static Jws<Claims> parseJWT(Key key, String claimsJws) {
        // 移除 JWT 前的"Bearer "字符串
        claimsJws = StringUtils.substringAfter(claimsJws, JWT_SEPARATOR);
        // 解析 JWT 字符串
        return Jwts.parser().setSigningKey(key).parseClaimsJws(claimsJws);
    }

    /**
     * 校验JWT
     *
     * @param claimsJws jwt 内容文本
     * @return ture or false
     */
    public static Boolean checkJWT(String claimsJws) {
        boolean flag = false;
        try {
            SecretKey key = generateKey(JWT_ALG, JWT_RULE);
            // 获取 JWT 的 payload 部分
            flag = (parseJWT(key, claimsJws).getBody() != null);
        } catch (Exception e) {
            log.error("JWT验证出错，错误原因：{}", e.getMessage());
        }
        return flag;
    }

    public static Jws<Claims> parser(String token) {
        SecretKey key = generateKey(JWT_ALG, JWT_RULE);
        return parseJWT(key, token);
    }

    /**
     * 校验JWT
     *
     * @param key       jwt 加密密钥
     * @param claimsJws jwt 内容文本
     * @param sub       jwt 面向的用户
     * @return ture or false
     */
    public static Boolean checkJWT(Key key, String claimsJws, String sub) {
        boolean flag = false;
        try {
            // 获取 JWT 的 payload 部分
            Claims claims = parseJWT(key, claimsJws).getBody();
            // 比对JWT中的 sub 字段
            flag = claims.getSubject().equals(sub);
        } catch (Exception e) {
            log.error("JWT验证出错，错误原因：{}", e.getMessage());
        }
        return flag;
    }

    /**
     * 校验JWT
     *
     * @param claimsJws jwt 内容文本
     * @param sub       jwt 面向的用户
     * @return ture or false
     */
    public static Boolean checkJWT(String claimsJws, String sub) {
        return checkJWT(generateKey(JWT_ALG, JWT_RULE), claimsJws, sub);
    }

}

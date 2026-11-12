package com.lifeshop.unit;

import com.lifeshop.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 身份鉴权层回归：令牌的签发、还原、篡改与过期。
 *
 * <p>拦截器链完全依赖 parse() 的返回值决定是否放行，
 * 因此「非法令牌必须返回 null 而不是抛异常」是这里最关键的断言。
 */
@DisplayName("L3 JWT：签发 / 解析 / 失效")
class JwtUtilTest {

    private static final String SECRET = "LifeShopSecretKey2026ForJwtTokenMustBeLongEnough";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expire", 604800000L);
    }

    @Test
    @DisplayName("签发的令牌能还原出 userId / username / role，且带过期时间")
    void shouldRoundTripClaims() {
        String token = jwtUtil.createToken(2L, "zhangsan", "USER");

        assertThat(token).isNotBlank().contains(".");
        Claims claims = jwtUtil.parse(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("2");
        assertThat(claims.get("username", String.class)).isEqualTo("zhangsan");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
    }

    @Test
    @DisplayName("被篡改的令牌返回 null（拦截器据此返回 401）")
    void shouldRejectTamperedToken() {
        String token = jwtUtil.createToken(2L, "zhangsan", "USER");
        String tampered = token.substring(0, token.length() - 3) + "aaa";

        assertThat(jwtUtil.parse(tampered)).isNull();
        assertThat(jwtUtil.parse("not.a.jwt")).isNull();
        assertThat(jwtUtil.parse("")).isNull();
    }

    @Test
    @DisplayName("签名密钥不同的令牌无法通过校验")
    void shouldRejectForeignSignature() {
        String foreignToken = jwtUtil.createToken(1L, "admin", "ADMIN");

        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "AnotherSecretKeyThatIsDefinitelyDifferent");
        ReflectionTestUtils.setField(other, "expire", 604800000L);

        assertThat(other.parse(foreignToken)).isNull();
    }

    @Test
    @DisplayName("过期令牌返回 null")
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expire", -1000L);
        String expired = jwtUtil.createToken(2L, "zhangsan", "USER");

        assertThat(jwtUtil.parse(expired)).isNull();
    }
}

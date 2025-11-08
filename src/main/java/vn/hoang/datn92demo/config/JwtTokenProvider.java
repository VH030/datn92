package vn.hoang.datn92demo.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String JWT_SECRET = "kTJhWQ93LsG8pMxNwAeTnXzK9VrYcBpFqD5G7H9J2L3Q8W4R6E7T9U2V5Y6Z8A1B"; // nên >= 32 ký tự
    private static final long JWT_EXPIRATION = 86400000L; // 1 ngày

    private SecretKey getSigningKey() {
        // tạo key an toàn cho HMAC
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // Tạo token
    public String generateToken(String phone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(phone)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Lấy số điện thoại từ token
    public String getPhoneFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }


    // Xác thực token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println(" JWT invalid: " + e.getMessage());
            return false;
        }
    }
}

package cotato.growingpain.security.jwt;

import cotato.growingpain.common.exception.AppException;
import cotato.growingpain.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secretKey}")
    String secretKey;

    @Value("${jwt.access.expiration}")
    Long accessExpiration;

    @Value("${jwt.refresh.expiration}")
    Long refreshExpiration;

    public boolean validateToken(String accessToken) {
        if (accessToken.isEmpty()) {
            throw new AppException(ErrorCode.JWT_NOT_EXISTS);
        }
        return isExpired(accessToken);
    }

    public boolean isExpired(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }

    public String getEmail(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token)
                .getBody().get("email", String.class);
    }

    public String getRole(String token) {
        Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        return claims.get("role", String.class);
    }

    public Token createToken(String email, String authority) {
        return Token.builder()
                .accessToken(createAccessToken(email,authority))
                .refreshToken(createRefreshToken(email,authority))
                .build();
    }

    private String createAccessToken(String email,String authority) {
        Claims claims = Jwts.claims();
        claims.put("email", email);
        claims.put("role",authority);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    private String createRefreshToken(String email,String authority) {
        Claims claims = Jwts.claims();
        claims.put("email", email);
        claims.put("role",authority);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
}
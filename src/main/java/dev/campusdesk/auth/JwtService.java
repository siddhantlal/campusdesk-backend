package dev.campusdesk.auth;

import java.time.Instant;
import java.util.List;
import dev.campusdesk.users.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    public JwtService(JwtEncoder encoder) { this.encoder = encoder; }
    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("campusdesk").audience(List.of("campusdesk-api"))
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
}

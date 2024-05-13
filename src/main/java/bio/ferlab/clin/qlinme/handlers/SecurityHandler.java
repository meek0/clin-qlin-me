package bio.ferlab.clin.qlinme.handlers;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Enums;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import io.javalin.security.RouteRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SecurityHandler {

  private static final String USER_ROLES_CLIN_PREFIX = "clin";
  private static final String TOKEN_ATTR_REALM_ACCESS = "realm_access";
  private static final String BEARER_PREFIX = "Bearer ";

  public enum Roles implements RouteRole {
    anonymous,
    clin_qlin_me,
  }

  private final JwkProvider jwkProvider;
  private final String issuer;
  private final String audience;
  private final String system;

  public SecurityHandler(String issuer, String audience, String system) {
    try {
      this.issuer = issuer;
      this.audience = audience;
      this.system = system;
      final String jwkUrl = StringUtils.appendIfMissing(issuer, "/") + "protocol/openid-connect/certs";
      this.jwkProvider = new JwkProviderBuilder(new URL(jwkUrl)).build(); // cached + rate limited by default
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Algorithm getAlgorithm(String keyId) throws JwkException {
    final Jwk jwk = jwkProvider.get(keyId);
    return Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
  }

  private String removeBearerPrefix(String token) {
    return token.replace(BEARER_PREFIX, "");
  }

  public List<Roles> checkAuthorization(Context ctx) {
      var authorization = ctx.header(HttpHeaders.AUTHORIZATION);
      if (StringUtils.isBlank(authorization)) {
        throw new HttpResponseException(HttpStatus.UNAUTHORIZED.getCode(), "missing token");
      }
      try {
        final String token = removeBearerPrefix(authorization);
        DecodedJWT jwt = JWT.decode(token);
        checkToken(jwt);
        return getUserRoles(token);
      } catch(JWTDecodeException e) {
        log.warn("Invalid token: {}", e.getMessage());  // hide from the user + log the reason
        throw new HttpResponseException(HttpStatus.FORBIDDEN.getCode(), "invalid token");
      }
  }

  private void checkToken(DecodedJWT jwt) {
    try {
      final Algorithm algorithm = getAlgorithm(jwt.getKeyId());
      JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build().verify(jwt);
    } catch (JwkException e) {
      log.warn("Invalid token: {}", e.getMessage()); // hide from the user + log the reason
      throw new HttpResponseException(HttpStatus.FORBIDDEN.getCode(), "invalid token");
    } catch( JWTVerificationException e) {
      throw new HttpResponseException(HttpStatus.FORBIDDEN.getCode(), e.getMessage());
    }
  }

  public List<Roles> getUserRoles(String token) {
    final var jwt = JWT.decode(token);

    final List<String> roles = new ArrayList<>();

    roles.addAll(Optional.ofNullable(jwt.getClaim(TOKEN_ATTR_REALM_ACCESS))
      .map(c -> c.as(RealmAccess.class))
      .map(c -> c.roles)
      .orElseThrow(() -> new HttpResponseException(HttpStatus.BAD_REQUEST.getCode(), "missing " + TOKEN_ATTR_REALM_ACCESS)));

    // if system, add roles we want to allow
    if (system.equals(Optional.ofNullable(jwt.getClaim("azp")).map(Claim::asString).orElse(null))) {
      roles.add(Roles.clin_qlin_me.name());
    }

    // ignore all roles that aren't clin or the ones in QLIN enum
    return roles.stream().filter(r -> r.startsWith(USER_ROLES_CLIN_PREFIX))
      .map(r -> Enums.getIfPresent(Roles.class, r).orNull())
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }

  private static class RealmAccess {
    public List<String> roles = new ArrayList<>();
  }
}

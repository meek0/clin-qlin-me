package bio.ferlab.clin.qlinme.handlers;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Slf4j
public class SecurityHandler {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwkProvider jwkProvider;
  private final boolean securityEnabled;
  private final String issuer;
  private final String audience;

  public SecurityHandler(String issuer, String audience, boolean securityEnabled) {
    try {
      this.issuer = issuer;
      this.audience = audience;
      this.securityEnabled = securityEnabled;
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

  public void checkAuthorization(Context ctx) {
    if(securityEnabled) {
      var authorization = ctx.header(HttpHeaders.AUTHORIZATION);
      if (StringUtils.isBlank(authorization)) {
        throw new HttpResponseException(HttpStatus.UNAUTHORIZED.getCode(), "missing token");
      }
      try {
        final String token = removeBearerPrefix(authorization);
        DecodedJWT jwt = JWT.decode(token);
        checkToken(jwt);
      } catch(JWTDecodeException e) {
        log.warn("Invalid token: {}", e.getMessage());  // hide from the user + log the reason
        throw new HttpResponseException(HttpStatus.FORBIDDEN.getCode(), "invalid token");
      }
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
}

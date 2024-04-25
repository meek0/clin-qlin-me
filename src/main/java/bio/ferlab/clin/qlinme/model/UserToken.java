package bio.ferlab.clin.qlinme.model;

import com.auth0.jwt.JWT;
import lombok.Getter;

@Getter
public class UserToken {

  private final String token;
  private final int expiresAt;
  private final int duration;

  public UserToken(String token) {
    this.token = token;
    var decoded = JWT.decode(token);
    this.expiresAt = decoded.getClaim("exp").asInt();
    this.duration = this.expiresAt - decoded.getClaim("iat").asInt();
  }

}

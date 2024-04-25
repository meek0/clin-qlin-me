package bio.ferlab.clin.qlinme.model;

public record UserToken(String token, int expiresAt, int duration) {

}

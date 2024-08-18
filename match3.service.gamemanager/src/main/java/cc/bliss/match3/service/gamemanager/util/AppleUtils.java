package cc.bliss.match3.service.gamemanager.util;

import com.google.gson.JsonObject;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

public class AppleUtils {

    static final String CLIENT_ID = "";
    static final HttpsJwks httpsJkws = new HttpsJwks("https://appleid.apple.com/auth/keys");
    static final HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);

    public static JsonObject fetchData(String token) {
        try {

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKeyResolver(httpsJwksKeyResolver)
                    .setExpectedIssuer("https://appleid.apple.com")
                    .setExpectedAudience(CLIENT_ID)
                    .build();

            JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
            System.out.println("Apple data" + jwtClaims.getClaimsMap());
            JsonObject jsonObject = new JsonObject();
            return jsonObject;
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
}

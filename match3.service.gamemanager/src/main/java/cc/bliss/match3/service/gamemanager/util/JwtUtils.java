/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.util;

import bliss.lib.framework.util.ConvertUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * @author Phong
 */
@Component
public class JwtUtils extends BaseService {

    private final ProfileRepository profileRepository;
    @Value("${match3.app.jwtSecret}")
    private String jwtSecret;

    @Value("${match3.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public JwtUtils(ProfileRepository profileRepository) {
        super();
        this.profileRepository = profileRepository;
    }

    public String generateJwtToken(Authentication authentication) {

        SessionObj userPrincipal = (SessionObj) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getId().toString()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateJwtToken(long userID) {

        return Jwts.builder()
                .setSubject((String.valueOf(userID)))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String generateJwtTokenForLogin(long userID, Date currentTimeStamp) {

        return Jwts.builder()
                .setSubject((String.valueOf(userID)))
                .setIssuedAt(currentTimeStamp)
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public long getUserIdFromJwtToken(String token) {
        return ConvertUtils.toLong(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject(), 0);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken).getBody();
            Date tokenCreatedDate = claims.getIssuedAt();
            long userID = getUserIdFromJwtToken(authToken);
            Date lastLoginDate = getLatestLoginTime(userID);
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return tokenCreatedDate.equals(lastLoginDate) || lastLoginDate.before(tokenCreatedDate);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getTokenExpireTime(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.getExpiration().getTime();
    }

    private Date getLatestLoginTime(long userId) {
        Optional<Profile> profile = profileRepository.read().findById(userId);
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return profile.map(p -> {
            try {
                Date timestamp = input.parse(p.getLastLogin().toString());
                String formattedTimestamp = output.format(timestamp);
                return output.parse(formattedTimestamp);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }).orElse(null);
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * @author Phong
 */
@Service
public class UserDetailsServiceImpl extends BaseService implements UserDetailsService {

    public static Map<Long, Profile> MAP_PROFILE_CACHE = new NonBlockingHashMap<>();
    private final ProfileRepository profileRepository;

    public UserDetailsServiceImpl(ProfileRepository profileRepository) {
        super();
        this.profileRepository = profileRepository;
    }

    @Override
    public SessionObj loadUserByUsername(String username) {
        Optional<Profile> user = profileRepository.read().findByUsername(username);
        if (user.isPresent()) {
            return SessionObj.build(user.get());
        }
        return null;
    }

    public SessionObj loadUserById(long id) {
        Profile profile = MAP_PROFILE_CACHE.get(id);
        if (profile == null) {
            Optional<Profile> optional = profileRepository.read().findById(id);
            if (optional.isPresent()) {
                profile = optional.get();
                MAP_PROFILE_CACHE.put(id, profile);
            }
        }
        return profile != null ? SessionObj.build(profile) : null;
    }

}

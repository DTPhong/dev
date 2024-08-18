/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author appfo
 */
@Data
public class FacebookInfo {

    String fbId = StringUtils.EMPTY;
    String fbName = StringUtils.EMPTY;
    String fbAvatar = StringUtils.EMPTY;
    String fbEmail = StringUtils.EMPTY;
}

package cc.bliss.match3.service.gamemanager.ent.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetect {
    private String ip;
    private String countryCode;
}

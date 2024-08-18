package cc.bliss.match3.service.gamemanager.db.specification;

import cc.bliss.match3.service.gamemanager.ent.enums.ETrophyRoadMileStoneType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TrophyRoadSpecification {

    public static Specification<TrophyRoadMileStoneEnt> withType(ETrophyRoadMileStoneType... eTrophyRoadMileStoneType) {
        List<Integer> listType = Arrays.asList(eTrophyRoadMileStoneType).stream().map(e -> e.ordinal()).collect(Collectors.toList());
        return (root, query, cb) -> root.get("type").in(listType);
    }
}

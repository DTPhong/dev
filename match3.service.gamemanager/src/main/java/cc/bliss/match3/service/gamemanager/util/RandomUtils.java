/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author appfo
 */
public class RandomUtils {

    public static ThreadLocalRandom RAND = ThreadLocalRandom.current();

    public static int random(List<RandomItem> items) {
        List<Integer> lst = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            for (int j = 0; j < items.get(i).percent; j++) {
                lst.add(items.get(i).data);
            }
        }

        for (int i = 0; i < 5; i++) {
            Collections.shuffle(lst);
        }

        Random r = new Random();
        return lst.get(r.nextInt(lst.size()));
    }

    public static int random(int origin, int bound) {
        return RAND.nextInt(origin, bound);
    }
}

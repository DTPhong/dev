/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author appfo
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class RandomItem {

    int data;
    int percent;
}

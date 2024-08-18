package cc.bliss.match3.service.gamemanager.controller;

import cc.bliss.match3.service.gamemanager.service.common.AgonesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/agones")
public class AgonesController {

    @Autowired
    private AgonesService agonesService;
}

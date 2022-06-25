package www.qiuming.top.webfiletrans.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EntryController {
    @GetMapping("/")
    public String entry(){
        return "upLoad";
    }
}

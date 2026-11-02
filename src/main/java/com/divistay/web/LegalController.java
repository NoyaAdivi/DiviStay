package com.divistay.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Privacy policy, terms of use and the accessibility statement. */
@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/accessibility")
    public String accessibility() {
        return "accessibility";
    }
}

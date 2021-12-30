package com.crypto.api.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.crypto.api.service.ValidateService;

@Controller
public class IndexController {

  @Autowired
  private ValidateService validateService;

  @GetMapping("/")
  public String index(Model model) {
    return "index";
  }

  @PostMapping("/validateInstrument")
  public @ResponseBody Map<String, Object> validateInstrument(
      @RequestParam("instrument") String instrument) {
    Map<String, Object> result = new HashMap<>();
    try {
      return validateService.validateInstrument(instrument);
    } catch (Exception ex) {
      result.put("errorCode", "99");
    }
    return result;
  }
}

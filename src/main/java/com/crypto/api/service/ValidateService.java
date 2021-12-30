package com.crypto.api.service;

import java.io.IOException;
import java.util.Map;
import org.json.JSONException;

public interface ValidateService {

  public Map<String, Object> validateInstrument(String instrument)
      throws JSONException, IOException, InterruptedException;
}

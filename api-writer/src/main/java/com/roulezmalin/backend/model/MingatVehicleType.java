package com.roulezmalin.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MingatVehicleType {
    @JsonProperty("title") public String title;
    @JsonProperty("uuid")  public String uuid;

    public MingatVehicleType() {}
}
package com.roulezmalin.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MingatAgence {
    @JsonProperty("nom")       public String nom;
    @JsonProperty("latitude")  public double latitude;
    @JsonProperty("longitude") public double longitude;
    @JsonProperty("uuid")      public String uuid;

    public MingatAgence() {}
}
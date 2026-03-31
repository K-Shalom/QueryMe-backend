package com.year2.queryme.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
  private String token;
  private String type = "Bearer";
  private UUID id;
  private String email;
  private List<String> roles;

  public JwtResponse(String accessToken, UUID id, String email, List<String> roles) {
    this.token = accessToken;
    this.id = id;
    this.email = email;
    this.roles = roles;
  }
}

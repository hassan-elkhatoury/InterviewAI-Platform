package com.interviewai.client.api;

import com.interviewai.client.model.Role;
import java.util.*;

/**
 * Role choices mirror the API's fixed authorization vocabulary; authorization is always enforced by
 * the server.
 */
public class RoleGateway {
  public List<Role> getAllRoles() {
    Role user = new Role();
    user.setId(1);
    user.setName("ROLE_USER");
    Role admin = new Role();
    admin.setId(2);
    admin.setName("ROLE_ADMIN");
    return List.of(user, admin);
  }

  public Role getRoleByName(String name) {
    return getAllRoles().stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
  }
}

package com.github.pius.pichats.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "users")
public class User extends GeneralBaseEntity {

  @NotBlank(message = "Please provide first name")
  @Column(nullable = false, name = "first_name")
  private String firstName;

  @NotBlank(message = "Please provide last name")
  @Column(nullable = false, name = "last_name")
  private String lastName;

  @Email(message = "Email should be valid")
  @NotBlank(message = "Please provide an email")
  @Column(nullable = false)
  private String email;

  @NotBlank(message = "Please provide a username")
  @Column(nullable = false)
  private String username;

  @NotBlank(message = "Please provide a password")
  @Column(nullable = false)
  private String password;

  @Column(name = "is_active", nullable = false)
  private boolean active = false;

  @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true)
  @JsonIgnore
  private EmailVerification emailVerification;

  @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER, mappedBy = "user", orphanRemoval = true)
  @JsonIgnore
  private List<Post> posts;

  @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true)
  private ProfilePic profilePic;

  @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true)
  @JsonIgnore
  private Bio bio;

  @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true)
  @JsonIgnore
  private List<Follow> followers;
}

package com.earthlyz9.restfulwebservice.user;

import com.earthlyz9.restfulwebservice.exceptions.UserNotFoundException;
import com.earthlyz9.restfulwebservice.exceptions.ValidationError;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "User")
@RestController
public class UserController {

  private final UserDaoService service;
  private final UserResourceAssembler assembler;

  // 생성자를 사용한 의존성 주입
  public UserController(UserDaoService service, UserResourceAssembler assembler) {
    this.service = service;
    this.assembler = assembler;
  }

  @Operation(summary = "Get all users", description = "Fetch all users")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200",
          description = "Success",
          content = @Content(schema = @Schema(implementation = SampleUserResponse.class)))
  })
  @GetMapping(path = "/users")
  public MappingJacksonValue listAllUsers() {
    List<User> users = service.getAllUsers();
    CollectionModel<EntityModel<User>> collectionModel = assembler.toCollectionModel(users);
    return applyUserInfoFilter(collectionModel);
  }

  @GetMapping(path = "/users/{id}")
  public MappingJacksonValue retrieveUserById(@PathVariable int id) {
    User user = service.findUserById(id);
    if (user == null) {
      throw new UserNotFoundException();
    } else {
      EntityModel<User> entityModel = assembler.toModel(user);
      return applyUserInfoFilter(entityModel);
    }
  }

  @PostMapping(path = "/users")
  public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    User savedUser = service.saveUser(user);

    // Location header 에 추가된 유저를 조회할 수 있는 uri 가 부착됨
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{userId}")
        .buildAndExpand(savedUser.getId()).toUri();

    // 201 상태코드와 유저 정보 반환
    return ResponseEntity.created(location).body(savedUser);
  }

  @DeleteMapping(path = "/users/{id}")
  public User deleteUser(@PathVariable int id) {
    User user = service.deleteUserById(id);
    if (user == null) {
      throw new UserNotFoundException();
    }

    return user;
  }

  @PatchMapping(path = "/users/{id}")
  public User updateUser(@PathVariable int id, @RequestBody User user) {
    if (user == null) {
      throw new ValidationError("name field is required");
    }
    User updatedUser = service.changeUserName(id, user.getName());
    if (updatedUser == null) {
      throw new UserNotFoundException();
    }

    return updatedUser;
  }

  static <T extends RepresentationModel<? extends T>> MappingJacksonValue applyUserInfoFilter(
      RepresentationModel<T> entityModel) {
    SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.filterOutAllExcept("id", "name",
        "birthDate", "joinDate");

    FilterProvider filters = new SimpleFilterProvider().addFilter("UserInfo", filter);

    MappingJacksonValue mapping = new MappingJacksonValue(entityModel);
    mapping.setFilters(filters);

    return mapping;
  }
}

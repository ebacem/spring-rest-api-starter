package com.monogramm.starter.api.user.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.madmath03.password.Passwords;
import com.monogramm.Application;
import com.monogramm.starter.api.AbstractControllerIT;
import com.monogramm.starter.api.AbstractControllerMockIT;
import com.monogramm.starter.config.data.GenericOperation;
import com.monogramm.starter.config.data.InitialDataLoader;
import com.monogramm.starter.dto.user.PasswordResetDto;
import com.monogramm.starter.dto.user.RegistrationDto;
import com.monogramm.starter.dto.user.UserDto;
import com.monogramm.starter.persistence.user.entity.PasswordResetToken;
import com.monogramm.starter.persistence.user.entity.User;
import com.monogramm.starter.persistence.user.entity.VerificationToken;
import com.monogramm.starter.persistence.user.exception.UserNotFoundException;
import com.monogramm.starter.persistence.user.service.IPasswordResetTokenService;
import com.monogramm.starter.persistence.user.service.IVerificationTokenService;
import com.monogramm.starter.utils.validation.PasswordConfirmationDto;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * {@link UserController} Mock Integration Test.
 * 
 * <p>
 * We assume the environment is freshly created and only contains the initial data.
 * </p>
 * 
 * <p>
 * Spring boot test is searching {@code @SpringBootConfiguration} or {@code @SpringBootApplication}.
 * In this case it will automatically find {@link Application} boot main class.
 * </p>
 * 
 * @see Application
 * @see InitialDataLoader
 * @see AbstractControllerIT
 * 
 * @author madmath03
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class UserControllerMockIT extends AbstractControllerMockIT {

  /**
   * The managed type of this tested controller.
   */
  public static final String TYPE = "Users";
  /**
   * The request base path of this tested controller.
   */
  public static final String CONTROLLER_PATH = '/' + TYPE;

  /**
   * The request path for registration.
   */
  public static final String REGISTER_PATH = CONTROLLER_PATH + "/register";

  /**
   * The request path for registration.
   */
  public static final String RESET_PWD_PATH = CONTROLLER_PATH + "/reset_password";

  /**
   * The request path for account verification.
   */
  public static final String VERIFY_PATH = CONTROLLER_PATH + "/verify";

  private static final String DUMMY_USERNAME = "Foo";
  private static final String DUMMY_EMAIL = "foo@email.com";
  private static final char[] DUMMY_PASSWORD = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

  private UUID randomId;

  private User testCreatedBy;
  private User testOwner;
  private User testEntity;

  @Autowired
  private InitialDataLoader initialDataLoader;

  @Autowired
  private IVerificationTokenService verificationService;

  @Autowired
  private IPasswordResetTokenService passwordResetTokenService;

  @Before
  public void setUp() {
    super.setUpMockMvc();
    super.setUpValidUser(GenericOperation.allPermissionNames(TYPE));

    this.randomId = UUID.randomUUID();

    // Add the users
    testCreatedBy =
        User.builder(DUMMY_USERNAME + "_Creator", DUMMY_USERNAME + ".creator@creation.org").build();
    assertTrue(getUserService().add(testCreatedBy));
    testOwner =
        User.builder(DUMMY_USERNAME + "_Owner", DUMMY_USERNAME + ".owner@creation.org").build();
    assertTrue(getUserService().add(testOwner));

    // Add a test user
    testEntity =
        User.builder(DUMMY_USERNAME, DUMMY_EMAIL).createdBy(testCreatedBy).owner(testOwner).build();
    assertTrue(getUserService().add(testEntity));
  }

  @After
  public void tearDown() {
    super.deleteUser(testEntity);
    testEntity = null;

    super.deleteUser(testCreatedBy);
    testCreatedBy = null;

    super.deleteUser(testOwner);
    testOwner = null;
  }

  /**
   * Test method for {@link UserController#getDataById(java.lang.String)}.
   * 
   * @throws Exception if the test crashes.
   */
  @Test
  public void testGetUserById() throws Exception {
    // No user returned
    getMockMvc().perform(get(CONTROLLER_PATH + '/' + randomId).headers(getHeaders(getMockToken())))
        .andExpect(status().isNotFound()).andExpect(content().bytes(new byte[] {}));

    // User previously created should be returned
    getMockMvc()
        .perform(get(CONTROLLER_PATH + '/' + this.testEntity.getId())
            .headers(getHeaders(getMockToken())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.id", equalToIgnoringCase(this.testEntity.getId().toString())))
        .andExpect(jsonPath("$.username", equalToIgnoringCase(DUMMY_USERNAME)))
        .andExpect(jsonPath("$.email", equalToIgnoringCase(DUMMY_EMAIL)))
        .andExpect(jsonPath("$.enabled", equalTo(Boolean.TRUE)))
        .andExpect(jsonPath("$.verified", equalTo(Boolean.FALSE)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()))
        .andExpect(jsonPath("$.createdBy", equalToIgnoringCase(testCreatedBy.getId().toString())))
        .andExpect(jsonPath("$.modifiedAt", nullValue()))
        .andExpect(jsonPath("$.modifiedBy", nullValue()))
        .andExpect(jsonPath("$.owner", notNullValue()))
        .andExpect(jsonPath("$.owner", equalToIgnoringCase(testOwner.getId().toString())));
  }

  /**
   * Test method for {@link UserController#getAllData()}.
   * 
   * @throws Exception if the test crashes.
   */
  @Test
  public void testGetAllUsers() throws Exception {
    // There should at least be the test entities...
    int expectedSize = 4;
    // ...plus the users created at application initialization
    if (initialDataLoader.getUsers() != null) {
      expectedSize += initialDataLoader.getUsers().size();
    }

    // We assume the environment is freshly created with only the initial data and test data
    getMockMvc().perform(get(CONTROLLER_PATH).headers(getHeaders(getMockToken())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$", hasSize(expectedSize)));
  }

  /**
   * Test method for
   * {@link UserController#addData(com.monogramm.starter.persistence.user.entity.User, org.springframework.web.util.UriComponentsBuilder)}.
   * 
   * @throws Exception if the test crashes.
   */
  @Test
  public void testAddUser() throws Exception {
    final String newUsername = "Bar";
    final String newEmail = "bar@email.com";
    final User model = User.builder(newUsername, newEmail).build();
    final UserDto dto = getUserService().toDto(model);

    final String userJson = dto.toJson();

    // Insert test user should work
    getMockMvc()
        .perform(post(CONTROLLER_PATH).headers(getHeaders(getMockToken())).content(userJson))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.username", equalToIgnoringCase(newUsername)))
        .andExpect(jsonPath("$.email", equalToIgnoringCase(newEmail)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", nullValue()))
        .andExpect(jsonPath("$.modifiedAt", nullValue()))
        .andExpect(jsonPath("$.modifiedBy", nullValue()));

    // Insert again should generate conflict
    getMockMvc()
        .perform(post(CONTROLLER_PATH).headers(getHeaders(getMockToken())).content(userJson))
        .andExpect(status().isConflict()).andExpect(content().bytes(new byte[] {}));
  }

  /**
   * Test method for {@link UserController#updateData(String, User)}.
   * 
   * @throws UserNotFoundException if the user entity to update is not found.
   */
  @Test
  public void testUpdateUser() throws Exception {
    // Update on random UUID should not find any user
    final User dummyModel = User.builder("God", "god@creation.org").id(randomId).build();
    final UserDto dummyDto = getUserService().toDto(dummyModel);
    getMockMvc().perform(put(CONTROLLER_PATH + '/' + randomId).headers(getHeaders(getMockToken()))
        .content(dummyDto.toJson())).andExpect(status().isNotFound());

    // Update the user
    final String newEmail = "new@email.com";
    this.testEntity.setEmail(newEmail);
    this.testEntity.setModifiedBy(testOwner);
    final UserDto dto = getUserService().toDto(this.testEntity);

    // Update test user should work
    final String entityJson = this.testEntity.toJson();
    final String dtoJson = dto.toJson();

    assertEquals(dtoJson, entityJson);

    getMockMvc()
        .perform(put(CONTROLLER_PATH + '/' + this.testEntity.getId())
            .headers(getHeaders(getMockToken())).content(dtoJson))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.id", equalToIgnoringCase(this.testEntity.getId().toString())))
        .andExpect(jsonPath("$.username", equalToIgnoringCase(DUMMY_USERNAME)))
        .andExpect(jsonPath("$.email", equalToIgnoringCase(newEmail)))
        .andExpect(jsonPath("$.enabled", equalTo(Boolean.TRUE)))
        .andExpect(jsonPath("$.verified", equalTo(Boolean.FALSE)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()))
        .andExpect(jsonPath("$.createdBy", equalToIgnoringCase(testCreatedBy.getId().toString())))
        .andExpect(jsonPath("$.modifiedAt", notNullValue()))
        .andExpect(jsonPath("$.modifiedBy", notNullValue()))
        .andExpect(jsonPath("$.modifiedBy", equalToIgnoringCase(testOwner.getId().toString())))
        .andExpect(jsonPath("$.owner", notNullValue()))
        .andExpect(jsonPath("$.owner", equalToIgnoringCase(testOwner.getId().toString())));
  }

  /**
   * Test method for {@link UserController#deleteData(java.lang.String)}.
   * 
   * @throws UserNotFoundException if the user entity to delete is not found.
   */
  @Test
  public void testDeleteUser() throws Exception {
    // Delete on random UUID should not find any user
    getMockMvc()
        .perform(delete(CONTROLLER_PATH + '/' + randomId).headers(getHeaders(getMockToken())))
        .andExpect(status().isNotFound());

    // Delete test user should work
    getMockMvc().perform(
        delete(CONTROLLER_PATH + '/' + this.testEntity.getId()).headers(getHeaders(getMockToken())))
        .andExpect(status().isNoContent());
  }

  /**
   * Test method for {@link UserController#getUserByUsernameOrEmail(String, String)}.
   * 
   * @throws Exception if the test crashes.
   */
  @Test
  public void testGetUserByUsernameOrEmail() throws Exception {
    // No user returned
    getMockMvc()
        .perform(get(CONTROLLER_PATH + "/get").param("username", "").param("email", "")
            .headers(getHeaders(getMockToken())))
        .andExpect(status().isNotFound()).andExpect(content().bytes(new byte[] {}));

    // User previously created should be returned
    getMockMvc()
        .perform(get(CONTROLLER_PATH + "/get").param("username", DUMMY_USERNAME)
            .param("email", DUMMY_EMAIL).headers(getHeaders(getMockToken())))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.id", equalToIgnoringCase(this.testEntity.getId().toString())))
        .andExpect(jsonPath("$.username", equalToIgnoringCase(DUMMY_USERNAME)))
        .andExpect(jsonPath("$.email", equalToIgnoringCase(DUMMY_EMAIL)))
        .andExpect(jsonPath("$.enabled", equalTo(Boolean.TRUE)))
        .andExpect(jsonPath("$.verified", equalTo(Boolean.FALSE)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()))
        .andExpect(jsonPath("$.createdBy", equalToIgnoringCase(testCreatedBy.getId().toString())))
        .andExpect(jsonPath("$.modifiedAt", nullValue()))
        .andExpect(jsonPath("$.modifiedBy", nullValue()))
        .andExpect(jsonPath("$.owner", notNullValue()))
        .andExpect(jsonPath("$.owner", equalToIgnoringCase(testOwner.getId().toString())));
  }

  /**
   * Test method for
   * {@link UserController#resetPassword(String, org.springframework.web.context.request.WebRequest)}
   * and {@link UserController#resetPassword(com.monogramm.starter.dto.user.PasswordResetDto)}.
   *
   * @throws UserNotFoundException if the user entity to update is not found.
   */
  @Test
  public void testResetPassword() throws Exception {
    // Resetting password on random email should not find any user
    getMockMvc().perform(post(RESET_PWD_PATH).content("dummy_email"))
        .andExpect(status().isNoContent());

    // Request to send a password reset email should work
    getMockMvc().perform(post(RESET_PWD_PATH).content(this.testEntity.getEmail()))
        .andExpect(status().isNoContent());

    // Retrieve password reset request and check its content
    final List<PasswordResetToken> tokens = passwordResetTokenService.findAll();
    assertNotNull(tokens);
    assertEquals(1, tokens.size());
    assertNotNull(tokens.get(0));
    assertNotNull(tokens.get(0).getCode());
    final PasswordResetToken token = tokens.get(0);
    final Date initialExpiration = token.getExpiryDate();
    assertNotNull(initialExpiration);

    final char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    final PasswordResetDto dto =
        new PasswordResetDto(this.testEntity.getEmail(), token.getCode(), password, password);

    getMockMvc().perform(put(RESET_PWD_PATH).headers(getHeaders()).content(toJsonBytes(dto)))
        .andExpect(status().isNoContent());

    final PasswordResetToken updatedToken = passwordResetTokenService.findById(token.getId());
    assertTrue(initialExpiration.after(updatedToken.getExpiryDate()));

    passwordResetTokenService.deleteById(token.getId());
  }

  /**
   * Test method for {@link UserController#changePassword(String, PasswordConfirmationDto)}.
   * 
   * @throws UserNotFoundException if the user entity to update is not found.
   */
  @Test
  public void testChangePassword() throws Exception {
    // Update on random UUID should not find any user
    final char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    final PasswordConfirmationDto dto = new PasswordConfirmationDto(password, password);

    getMockMvc()
        .perform(put(CONTROLLER_PATH + "/change_password/" + randomId)
            .headers(getHeaders(getMockToken())).content(toJsonBytes(dto)))
        .andExpect(status().isNotFound());

    // Update test user password should work
    final byte[] passwordJsonBytes = toJsonBytes(dto);
    getMockMvc()
        .perform(put(CONTROLLER_PATH + "/change_password/" + this.testEntity.getId())
            .headers(getHeaders(getMockToken())).content(passwordJsonBytes))
        .andExpect(status().isNoContent());

    final User updatedModel = getUserService().findByEmail(DUMMY_EMAIL);
    assertNotNull(updatedModel.getPassword());
    assertTrue(Passwords.isExpectedPassword(password, updatedModel.getPassword()));
  }

  /**
   * Test method for {@link UserController#activate(String, Boolean)}.
   * 
   * @throws UserNotFoundException if the user entity to update is not found.
   */
  @Test
  public void testActivate() throws Exception {
    // Activating on random UUID should not find any user
    getMockMvc()
        .perform(put(CONTROLLER_PATH + "/" + randomId + "/activate")
            .headers(getHeaders(getMockToken())).content(toJsonBytes(Boolean.TRUE)))
        .andExpect(status().isNotFound());

    // Update test user activation should work
    getMockMvc()
        .perform(put(CONTROLLER_PATH + "/" + this.testEntity.getId() + "/activate")
            .headers(getHeaders(getMockToken())).content(toJsonBytes(Boolean.FALSE)))
        .andExpect(status().isNoContent());

    final User updatedModel = getUserService().findByEmail(DUMMY_EMAIL);
    assertEquals(Boolean.FALSE, updatedModel.isEnabled());
  }

  /**
   * Test method for
   * {@link UserController#register(RegistrationDto, org.springframework.web.context.request.WebRequest)}
   * and {@link UserController#verify(String)}.
   * 
   * @throws Exception if the test crashes.
   */
  @Test
  public void testRegisterAndVerify() throws Exception {
    final String username = "Bar";
    final String email = "bar@monogramm.io";
    final RegistrationDto model = new RegistrationDto();
    model.setUsername(username);
    model.setEmail(email);
    model.setPassword(DUMMY_PASSWORD);
    model.setMatchingPassword(DUMMY_PASSWORD);

    final String userJson = toJson(model);

    // Register test user should work
    getMockMvc()
        .perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON_UTF8).content(userJson))
        .andExpect(status().isNoContent()).andExpect(content().bytes(new byte[] {}));

    final List<VerificationToken> tokens = verificationService.findAll();
    assertNotNull(tokens);
    assertEquals(1, tokens.size());
    assertNotNull(tokens.get(0));
    assertNotNull(tokens.get(0).getCode());
    final VerificationToken token = tokens.get(0);
    final Date initialExpiration = token.getExpiryDate();
    assertNotNull(initialExpiration);

    // Register again should generate conflict
    getMockMvc()
        .perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON_UTF8).content(userJson))
        .andExpect(status().isConflict()).andExpect(content().bytes(new byte[] {}));

    // Verifying on random UUID should not find any user
    getMockMvc().perform(put(VERIFY_PATH + "/" + randomId).headers(getHeaders(getMockToken()))
        .content(token.getCode())).andExpect(status().isNotFound());

    // Update test user verification should work
    final User registeredUser = getUserService().findByEmail(model.getEmail());
    assertNotNull(registeredUser);
    assertNotNull(registeredUser.getId());

    getMockMvc().perform(put(VERIFY_PATH + "/" + registeredUser.getId())
        .headers(getHeaders(getMockToken())).content(token.getCode()))
        .andExpect(status().isNoContent());

    final VerificationToken updatedToken = verificationService.findById(token.getId());
    assertTrue(initialExpiration.after(updatedToken.getExpiryDate()));

    verificationService.deleteById(token.getId());
  }

  /**
   * Test method for
   * {@link UserController#sendVerification(String, org.springframework.web.context.request.WebRequest)}
   * and {@link UserController#verify(String)}.
   *
   * @throws UserNotFoundException if the user entity to update is not found.
   */
  @Test
  public void testSendVerificationAndVerify() throws Exception {
    // Verifying already verified user should not send any email but work anyway
    getMockMvc().perform(post(CONTROLLER_PATH + "/send_verification")
        .headers(getHeaders(getMockToken())).content(getTestUser().getEmail()))
        .andExpect(status().isOk());

    List<VerificationToken> tokens = verificationService.findAll();
    assertNotNull(tokens);
    assertEquals(0, tokens.size());

    // Request to send a verification email should work
    getMockMvc().perform(post(CONTROLLER_PATH + "/send_verification")
        .headers(getHeaders(getMockToken())).content(this.testEntity.getEmail()))
        .andExpect(status().isNoContent());

    tokens = verificationService.findAll();
    assertNotNull(tokens);
    assertEquals(1, tokens.size());
    assertNotNull(tokens.get(0));
    assertNotNull(tokens.get(0).getCode());
    final VerificationToken token = tokens.get(0);
    final Date initialExpiration = token.getExpiryDate();
    assertNotNull(initialExpiration);

    // Verifying on random UUID should not find any user
    getMockMvc().perform(put(VERIFY_PATH + "/" + randomId).headers(getHeaders(getMockToken()))
        .content(token.getCode())).andExpect(status().isNotFound());

    // Update test user verification should work
    getMockMvc()
        .perform(put(VERIFY_PATH + "/" + this.testEntity.getId())
            .headers(getHeaders(getMockToken())).content(token.getCode()))
        .andExpect(status().isNoContent());

    final User updatedModel = getUserService().findByEmail(DUMMY_EMAIL);
    assertEquals(Boolean.TRUE, updatedModel.isVerified());

    final VerificationToken updatedToken = verificationService.findById(token.getId());
    assertTrue(initialExpiration.after(updatedToken.getExpiryDate()));

    verificationService.deleteById(token.getId());
  }

}

package eu.bavenir.ogwapi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.verify.VerificationTimes;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import eu.bavenir.ogwapi.commons.connectors.NeighbourhoodManagerConnector;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Test module for the NM endpoints Mocks the logger and the NM with a
 * mock-server that generates the proper responses The mock-server is ran with
 * Docker: ./_run.sh -e test
 * 
 * @author jorge
 */
public class nmApiTest {

  /**
   * Constants declaration
   */
  private static final String CONFIG_PATH = "config/TestConfig.xml";

  /**
   * Variable declaration
   */
  private static Configurations configurations;
  private static XMLConfiguration config;

  /**
   * Define Mocks
   */
  @Mock
  private Logger logger;

  /**
   * Initialize test - Config - MockServer - Mocks
   */

  @Before
  public void beforeEach() {
    configurations = new Configurations();
    try {
      config = configurations.xml(CONFIG_PATH);
    } catch (final ConfigurationException e) {
      e.printStackTrace();
      System.err.println("Error loading test config");
    }
  }

  // UNIT TESTS

  @Test
  public void testGetAgentObjects() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // use mock in test....
    checkGetRequest("/commServer/agent/someAgid/items");
    Representation rep = nmConnector.getAgentObjects("someAgid");
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'GetSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/agent/someAgid/items", "GET");
  }

  @Test
  public void testStoreObjects() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPostRequest("/commServer/items/register", payload.toString());
    JsonRepresentation test = new JsonRepresentation(payload.toString());
    Representation rep = nmConnector.storeObjects(test);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PostSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/register", "POST");
  }

  @Test
  public void testHeavyweightUpdate() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPutRequest("/commServer/items/modify", payload.toString());
    JsonRepresentation test = new JsonRepresentation(payload.toString());
    Representation rep = nmConnector.heavyweightUpdate(test);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PutSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/modify", "PUT");
  }

  @Test
  public void testLightweightUpdate() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPutRequest("/commServer/items/update", payload.toString());
    JsonRepresentation test = new JsonRepresentation(payload.toString());
    Representation rep = nmConnector.lightweightUpdate(test);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PutSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/update", "PUT");
  }

  @Test
  public void testDeleteObjects() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPostRequest("/commServer/items/remove", payload.toString());
    JsonRepresentation test = new JsonRepresentation(payload.toString());
    Representation rep = nmConnector.deleteObjects(test);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PostSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/remove", "POST");
  }

  @Test
  public void testGetThingDescriptions() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPostRequest("/commServer/items/td", payload.toString());
    JsonRepresentation test = new JsonRepresentation(payload.toString());
    Representation rep = nmConnector.getThingDescriptions(test);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PostSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/td", "POST");
  }

  @Test
  public void testGetThingDescription() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("oid", "someOid"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPostRequest("/commServer/items/td", payload.toString());
    Representation rep = nmConnector.getThingDescription("someOid");
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PostSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/items/td", "POST");
  }

  @Test
  public void testSendCounters() {
    NeighbourhoodManagerConnector nmConnector = new NeighbourhoodManagerConnector(config, logger);
    // Build Post Body
    JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
    mainArrayBuilder.add(Json.createObjectBuilder().add("test", "123"));
    mainObjectBuilder.add("objects", mainArrayBuilder);
    JsonObject payload = mainObjectBuilder.build();
    checkPostRequest("/commServer/counters", payload.toString());
    Representation rep = nmConnector.sendCounters(payload);
    try {
      String str = new String(rep.getStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(str, "{ message: 'PostSuccess'}");
    } catch (IOException err) {
      System.out.println(err);
    }
    verifyRequest("/commServer/counters", "POST");
  }

  // PRIVATE FUNCTIONS

  private void checkGetRequest(String path) {
    new MockServerClient("127.0.0.1", 1080)
        .when(request().withMethod("GET").withPath(path).withHeaders(new Header("content-length"), new Header("Date"),
            new Header("Accept", "application/json"), new Header("User-Agent", "Restlet-Framework/2.3.9"),
            new Header("Cache-Control", "no-cache"), new Header("Host", "localhost:1080"),
            new Header("Connection", "keep-alive")), exactly(1))
        .respond(response().withStatusCode(200).withBody("{ message: 'GetSuccess'}")
            .withHeaders(new Header("Accept", "application/json")));
  }

  private void checkPostRequest(String path, String body) {
    new MockServerClient("127.0.0.1", 1080)
        .when(
            request().withMethod("POST").withPath(path)
                .withHeaders(new Header("content-length"), new Header("Date"), new Header("Accept", "application/json"),
                    new Header("User-Agent", "Restlet-Framework/2.3.9"), new Header("Cache-Control", "no-cache"),
                    new Header("Host", "localhost:1080"), new Header("Connection", "keep-alive"))
                .withBody(exact(body)),
            exactly(1))
        .respond(response().withStatusCode(200).withBody("{ message: 'PostSuccess'}")
            .withHeaders(new Header("Accept", "application/json")));
  }

  private void checkPutRequest(String path, String body) {
    new MockServerClient("127.0.0.1", 1080)
        .when(
            request().withMethod("PUT").withPath(path)
                .withHeaders(new Header("content-length"), new Header("Date"), new Header("Accept", "application/json"),
                    new Header("User-Agent", "Restlet-Framework/2.3.9"), new Header("Cache-Control", "no-cache"),
                    new Header("Host", "localhost:1080"), new Header("Connection", "keep-alive"))
                .withBody(exact(body)),
            exactly(1))
        .respond(response().withStatusCode(200).withBody("{ message: 'PutSuccess'}")
            .withHeaders(new Header("Accept", "application/json")));
  }

  private void verifyRequest(String path, String method) {
    new MockServerClient("127.0.0.1", 1080).verify(request().withMethod(method).withPath(path),
        VerificationTimes.atLeast(1));
  }

}
# Smart Campus REST API

**Module:** 5COSC022W Client-Server Architectures | University of Westminster | 2025/26

---

## Project Overview

The **Smart Campus REST API** is a fully RESTful web service built for the University of Westminster's Smart Campus initiative. It enables IoT sensor management across campus rooms — allowing facilities teams to register rooms, attach sensors, record environmental readings, and monitor real-time campus conditions.

The API is designed around three core resources arranged in a hierarchical structure:

- **Rooms** — physical campus spaces that house sensors
- **Sensors** — IoT devices (CO2, Temperature, Humidity, etc.) installed inside rooms
- **Sensor Readings** — timestamped measurements recorded by sensors over time

The system enforces strict business rules: sensors must always be linked to a valid room, rooms cannot be deleted while sensors are attached, and sensors in MAINTENANCE status cannot accept new readings.

### Technology Stack

| Component | Technology |
|---|---|
| Language | Java (JDK 8+) |
| REST Framework | JAX-RS (Jersey 2.32) |
| Build Tool | Apache Maven |
| Server | Apache Tomcat 9 |
| Data Storage | In-memory (HashMap + ArrayList — no database) |
| Packaging | WAR file deployed to Tomcat |

### Architecture Summary

The application uses a **singleton `InMemoryStore`** shared across all request-scoped JAX-RS resource classes. All methods on `InMemoryStore` are `synchronized` to prevent race conditions when multiple HTTP requests arrive concurrently. On startup, seed data is automatically loaded — one room (`LIB-301`) and one CO2 sensor (`CO2-001`) — so the API is immediately testable without any manual setup.

### Resource Hierarchy

```
/api/v1
├── /                        → Discovery (GET)
├── /rooms
│   ├── GET                  → List all rooms
│   ├── POST                 → Create a new room (201 Created)
│   ├── GET /{roomId}        → Get room by ID
│   └── DELETE /{roomId}     → Delete room (blocked if sensors exist → 409 Conflict)
└── /sensors
    ├── GET                  → List all sensors (supports ?type= filter)
    ├── POST                 → Register sensor (validates roomId exists → 422 if not)
    ├── GET /{sensorId}      → (via InMemoryStore)
    └── /{sensorId}/readings
        ├── GET              → Get reading history for a sensor
        └── POST             → Add new reading (updates parent sensor currentValue; 403 if MAINTENANCE)
```

---

## Build and Run Instructions

### Prerequisites

- Java JDK 8 or higher
- Apache Maven 3.6+
- Apache Tomcat 9

### Step 1 — Clone the Repository

```bash
git clone <your-github-repo-url>
cd smart-campus-api
```

### Step 2 — Build the Project

```bash
mvn clean package
```

A successful build shows `BUILD SUCCESS` and generates:

```
target/smart-campus-api.war
```

### Step 3 — Deploy to Tomcat

```bash
# Windows
copy target\smart-campus-api.war C:\path\to\tomcat\webapps\

# macOS / Linux
cp target/smart-campus-api.war /opt/tomcat/webapps/
```

### Step 4 — Start Tomcat

```bash
# Windows
cd C:\path\to\tomcat\bin && startup.bat

# macOS / Linux
cd /opt/tomcat/bin && ./startup.sh
```

### Step 5 — Verify

Open a browser and navigate to:

```
http://localhost:8080/smart-campus-api/api/v1
```

You should receive a JSON discovery response confirming the API is live.

---

## Full Endpoint Reference

### GET /api/v1 — Discovery

Returns API metadata and links to all resources.

**Method:** `GET`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1`  
**Auth:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
{
  "name": "Smart Campus API",
  "version": "v1",
  "contact": "facilities@westminster.ac.uk",
  "links": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### POST /api/v1/rooms — Create Room

Registers a new room in the system.

**Method:** `POST`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/rooms`  
**Headers:** `Content-Type: application/json`  

**Request Body:**
```json
{
  "id": "LAB-101",
  "name": "Computer Lab 101",
  "capacity": 40
}
```

**Success Response — 201 Created:**
```json
{
  "message": "Room created successfully.",
  "roomId": "LAB-101"
}
```
Also includes a `Location` header: `http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-101`

**Error Response — 500 Internal Server Error:** If an unexpected error occurs (no stack trace exposed).

---

### GET /api/v1/rooms — Get All Rooms

Returns a list of all rooms including their sensor ID lists.

**Method:** `GET`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/rooms`  
**Headers:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
[
  {
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 120,
    "sensorIds": ["CO2-001"]
  },
  {
    "id": "LAB-101",
    "name": "Computer Lab 101",
    "capacity": 40,
    "sensorIds": []
  }
]
```

---

### GET /api/v1/rooms/{roomId} — Get Room by ID

Returns details of a single room.

**Method:** `GET`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-101`  
**Headers:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
{
  "id": "LAB-101",
  "name": "Computer Lab 101",
  "capacity": 40,
  "sensorIds": []
}
```

**Error Response — 422 Unprocessable Entity:** Room ID not found.
```json
{
  "message": "Room with id LAB-999 was not found.",
  "status": 422
}
```

---

### DELETE /api/v1/rooms/{roomId} — Delete Room

Deletes a room. Blocked if any sensors are still assigned to it.

**Method:** `DELETE`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-101`  
**Headers:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
{
  "message": "Room deleted successfully.",
  "roomId": "LAB-101"
}
```

**Error Response — 409 Conflict:** Room has sensors assigned.
```json
{
  "message": "Room cannot be deleted because sensors are still assigned.",
  "status": 409
}
```

**Error Response — 422 Unprocessable Entity:** Room not found.
```json
{
  "message": "Room with id LIB-301 was not found.",
  "status": 422
}
```

---

### POST /api/v1/sensors — Register Sensor

Registers a new sensor. The `roomId` must reference an existing room.

**Method:** `POST`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/sensors`  
**Headers:** `Content-Type: application/json`  

**Request Body (valid):**
```json
{
  "id": "TEMP-002",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LAB-101"
}
```

**Success Response — 201 Created:**
```json
{
  "message": "Sensor created successfully.",
  "sensorId": "TEMP-002"
}
```

**Error Response — 422 Unprocessable Entity:** `roomId` does not exist.
```json
{
  "message": "Cannot create sensor. Referenced roomId does not exist.",
  "status": 422
}
```

**Request Body (invalid roomId — triggers 422):**
```json
{
  "id": "CO2-404",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "NO-ROOM"
}
```

---

### GET /api/v1/sensors — Get All Sensors (with optional filter)

Returns all sensors. Optionally filter by sensor type using a query parameter.

**Method:** `GET`  
**URL (all sensors):** `http://localhost:8080/smart-campus-api/api/v1/sensors`  
**URL (filtered):** `http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2`  
**Headers:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
[
  {
    "id": "CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 426.4,
    "roomId": "LIB-301"
  }
]
```

Filtering is **case-insensitive** — `?type=co2`, `?type=CO2`, and `?type=Co2` all return the same results.

---

### POST /api/v1/sensors/{sensorId}/readings — Add Sensor Reading

Records a new reading for a sensor. Also updates the sensor's `currentValue`. Blocked if the sensor is in MAINTENANCE status.

**Method:** `POST`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001/readings`  
**Headers:** `Content-Type: application/json`  

**Request Body:**
```json
{
  "id": "R-1001",
  "timestamp": 1713850000000,
  "value": 426.4
}
```

> Note: `id` is optional — if omitted, the server auto-generates a UUID.

**Success Response — 201 Created:**
```json
{
  "message": "Reading recorded successfully.",
  "readingId": "R-1001"
}
```

**Error Response — 403 Forbidden:** Sensor is in MAINTENANCE status.
```json
{
  "message": "Sensor is in MAINTENANCE and cannot accept readings.",
  "status": 403
}
```

**Error Response — 422 Unprocessable Entity:** Sensor not found.
```json
{
  "message": "Sensor with id CO2-999 was not found.",
  "status": 422
}
```

---

### GET /api/v1/sensors/{sensorId}/readings — Get Sensor Readings

Returns the full reading history for a sensor.

**Method:** `GET`  
**URL:** `http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001/readings`  
**Headers:** None  
**Body:** None  

**Success Response — 200 OK:**
```json
[
  {
    "id": "R-1001",
    "timestamp": 1713850000000,
    "value": 426.4
  }
]
```

**Error Response — 422 Unprocessable Entity:** Sensor not found.
```json
{
  "message": "Sensor with id CO2-999 was not found.",
  "status": 422
}
```

---

## Error Response Summary

| Scenario | HTTP Status |
|---|---|
| Room not found | 422 Unprocessable Entity |
| Delete room with sensors assigned | 409 Conflict |
| Create sensor with non-existent roomId | 422 Unprocessable Entity |
| Post reading to a MAINTENANCE sensor | 403 Forbidden |
| Sensor not found when fetching readings | 422 Unprocessable Entity |
| Any unexpected server error | 500 Internal Server Error |
| Wrong Content-Type (handled by JAX-RS) | 415 Unsupported Media Type |

---

## Postman Collection Guide

The Postman collection (`postman/smart_campus_aligned_collection.json`) is organized into 6 folders matching the coursework demonstration script. Import the file into Postman and run requests **in order** from top to bottom.

### Folder Structure and Order

**Part 1 — Discovery**

| Request | Method | URL | Expected Status |
|---|---|---|---|
| Discovery Endpoint | GET | `/api/v1` | 200 OK |

**Part 2 — Room Management**

| Request | Method | URL | Body | Expected Status |
|---|---|---|---|---|
| Create Room | POST | `/api/v1/rooms` | `{"id":"LAB-101","name":"Computer Lab 101","capacity":40}` | 201 Created |
| Get All Rooms | GET | `/api/v1/rooms` | — | 200 OK |
| Get Room by ID | GET | `/api/v1/rooms/LAB-101` | — | 200 OK |

**Part 3 — Sensors & Filtering**

| Request | Method | URL | Body | Expected Status |
|---|---|---|---|---|
| Invalid Linked Room Check | POST | `/api/v1/sensors` | `{"id":"CO2-404","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"NO-ROOM"}` | 422 Unprocessable Entity |
| Get All Sensors (Show Seeded) | GET | `/api/v1/sensors` | — | 200 OK |
| Filter Sensors by Type | GET | `/api/v1/sensors?type=CO2` | — | 200 OK |

**Part 4 — Sub-Resource (Readings)**

| Request | Method | URL | Body | Expected Status |
|---|---|---|---|---|
| Add Reading to Sensor | POST | `/api/v1/sensors/CO2-001/readings` | `{"id":"R-1001","timestamp":1713850000000,"value":426.4}` | 201 Created |
| Get Sensor Readings | GET | `/api/v1/sensors/CO2-001/readings` | — | 200 OK |

**Part 5 — Exception Handling**

| Request | Method | URL | Body | Expected Status |
|---|---|---|---|---|
| Delete Room (409 Conflict) | DELETE | `/api/v1/rooms/LIB-301` | — | 409 Conflict |
| Create Maintenance Sensor | POST | `/api/v1/sensors` | `{"id":"CO2-M1","type":"CO2","status":"MAINTENANCE","currentValue":0.0,"roomId":"LIB-301"}` | 201 Created |
| Add Reading to Maintenance Sensor | POST | `/api/v1/sensors/CO2-M1/readings` | `{"id":"R-2001","timestamp":1713850001000,"value":500.0}` | 403 Forbidden |
| Get Readings for Invalid Sensor | GET | `/api/v1/sensors/CO2-999/readings` | — | 422 Unprocessable Entity |

**Part 6 — Logging**

| Request | Method | URL | Expected Status |
|---|---|---|---|
| Logging Demonstration | GET | `/api/v1/sensors` | 200 OK (check Tomcat console) |

---

## Sample curl Commands

```bash
# Discovery
curl -i http://localhost:8080/smart-campus-api/api/v1

# Create Room
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"LAB-101\",\"name\":\"Computer Lab 101\",\"capacity\":40}"

# Get All Rooms
curl -i http://localhost:8080/smart-campus-api/api/v1/rooms

# Get Room by ID
curl -i http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-101

# Delete Room (409 if sensors exist)
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301

# Create Sensor (valid roomId)
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-002\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"LAB-101\"}"

# Create Sensor (invalid roomId — triggers 422)
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-404\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"NO-ROOM\"}"

# Filter Sensors by Type
curl -i "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"

# Add Sensor Reading
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"R-1001\",\"timestamp\":1713850000000,\"value\":426.4}"

# Get Sensor Readings
curl -i http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001/readings
```

---

## 5. Report — Answers to Coursework Questions


### Part 1.1

**Question: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created for every request, or is it treated as a singleton? How does this affect the way you manage in-memory data structures to prevent data loss or race conditions?**

By default, JAX-RS treats resource classes as request-scoped, meaning the runtime creates a fresh instance of the resource class for each incoming HTTP request. Once the response has been sent, that instance is discarded and does not persist into subsequent requests. This is the opposite of a singleton — no single object is shared across multiple requests.

This lifecycle decision has a direct impact on how shared data must be managed. If the rooms or sensors were stored as instance fields within the resource class itself, they would be re-initialised with every new request and all previously stored data would be permanently lost. To prevent this, all shared state in this project is maintained in a separate singleton class called `InMemoryStore`. This class is created once when the application starts and remains alive for the full duration of the server session. All resource classes retrieve the same instance via a static `getInstance()` method, ensuring they all read from and write to the same shared data store.

There is an additional concern related to concurrency. Since multiple HTTP requests can arrive simultaneously and be processed by different threads, two threads could attempt to read or write the same `HashMap` at exactly the same time, which risks corrupted or inconsistent data. To address this, every method in `InMemoryStore` is declared as `synchronized`, ensuring that only one thread can access the shared data at any given moment. This eliminates race conditions and maintains data integrity across concurrent requests.

---

### Part 1.2

**Question: Why is the provision of Hypermedia (HATEOAS) considered a hallmark of advanced RESTful design? How does this approach benefit client developers compared to static documentation?**

HATEOAS, which stands for Hypermedia as the Engine of Application State, is the principle that API responses should contain links to related resources and available next actions, allowing clients to navigate the API dynamically rather than relying on prior knowledge of URL structures.

In this implementation, the discovery endpoint at `GET /api/v1` returns navigational links to `/api/v1/rooms` and `/api/v1/sensors` directly within the JSON response body. A client that reads and follows these links at runtime does not need to have those URLs hardcoded in its source code.

This offers clear advantages over static documentation. When server-side URL structures change, clients that follow hypermedia links rather than hardcoded paths remain functional without requiring updates on the client side. The API also becomes self-describing — a developer can begin at the root endpoint and progressively discover all available resources and operations without consulting external documentation. Static documentation, by contrast, becomes outdated the moment the API changes and requires ongoing manual effort to remain accurate. These qualities are why HATEOAS is regarded as a characteristic of mature REST API design: it reduces the coupling between client and server and promotes long-term maintainability on both sides.

---

### Part 2.1

**Question: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.**

When responding to `GET /api/v1/rooms`, the API could return either a list of room IDs only or a list of complete room objects containing all fields.

Returning only IDs produces a significantly smaller response payload and reduces the volume of data transferred over the network. This is beneficial at scale, particularly when the number of rooms is large. However, any client that requires further details — such as the room name or capacity — would need to issue a separate GET request for each individual room. This multiplies the number of HTTP round trips and increases total latency proportionally.

Returning full room objects increases the size of the initial response but removes the need for any follow-up requests. The client receives all the information it needs in a single call, which simplifies client-side logic and reduces overall latency. In this implementation, full room objects are returned in list responses because the `Room` model is lightweight and the benefit of reducing HTTP round trips outweighs the modest increase in response payload size.

---

### Part 2.2

**Question: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if the same DELETE request is sent multiple times.**

The DELETE operation is idempotent in this implementation. Idempotency means that issuing the same request multiple times produces the same server state as issuing it once.

When `DELETE /api/v1/rooms/{roomId}` is called for the first time against a room with no sensors assigned, the room is removed from the in-memory store and the server returns `200 OK`. If the identical request is sent a second time, the room no longer exists. The server returns `404 Not Found`, but this response does not reflect any new change to server state — the system was already in the state where that room is absent, and it remains in that state. The HTTP response code changes between the first and subsequent calls, but the state of the data store does not change after the first successful deletion. This satisfies the standard definition of idempotency, which concerns the effect on server state rather than the uniformity of the response code.

---

### Part 3.1

**Question: We explicitly use `@Consumes(MediaType.APPLICATION_JSON)` on the POST method. Explain the technical consequences if a client sends data in a different format, such as text/plain or application/xml.**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares to the JAX-RS runtime that the POST method is only capable of processing requests where the `Content-Type` header is set to `application/json`. When a client submits a request with a different content type — for example `text/plain` or `application/xml` — the JAX-RS framework evaluates the incoming header against this declared constraint during the request matching phase. Finding a mismatch, the framework automatically rejects the request and returns an HTTP `415 Unsupported Media Type` response before the resource method body is ever invoked. This content negotiation is handled entirely at the framework level, meaning no manual validation code is required inside the method itself, and the business logic is protected from receiving data it cannot process.

---

### Part 3.2

**Question: You implemented filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path. Why is the query parameter approach generally considered superior for filtering and searching collections?**

Using a query parameter for filtering, as in `GET /api/v1/sensors?type=CO2`, is preferable to embedding the filter value in the URL path, as in `GET /api/v1/sensors/type/CO2`, for several reasons rooted in REST design principles.

Query parameters are inherently optional. When the `type` parameter is omitted entirely, the endpoint returns all sensors — the same URL handles both the filtered and unfiltered scenarios without any change to routing or method signatures. In contrast, placing the filter value in the path implies that `/sensors/type/CO2` is a distinct, uniquely addressable resource. This is semantically incorrect because filtering is an operation performed on a collection, not an identifier for a specific resource.

Query parameters also compose naturally. Multiple filter criteria can be applied simultaneously, such as `?type=CO2&status=ACTIVE`, without requiring any restructuring of the URL hierarchy. This flexibility aligns with established REST API design conventions and makes the interface significantly more intuitive for developers who consume the API.

---

### Part 4.1

**Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs?**

The Sub-Resource Locator pattern involves a method in the parent resource class returning an instance of a dedicated child resource class, without an HTTP method annotation on the locator method itself. The JAX-RS runtime then delegates all further path matching and request handling to the returned child class. In this implementation, `SensorResource` contains a locator method mapped to `/{sensorId}/readings` that instantiates and returns a `SensorReadingResource` object.

The primary architectural benefit is a clean separation of concerns. `SensorResource` is responsible solely for sensor-level operations, while `SensorReadingResource` manages all logic related to reading history independently. Each class is focused, compact, and can be read, tested, and modified in isolation without affecting the other.

Without this pattern, every nested path — including `/sensors/{id}/readings` and any further operations beneath it — would need to be defined directly inside `SensorResource`. As the API grows, this leads to a single class with an increasing number of responsibilities, which becomes difficult to navigate and maintain. By delegating to separate resource classes, the design reflects the single responsibility principle and keeps each component of the system manageable. This becomes especially important in large-scale API projects where dozens of nested sub-resources may exist.

---

### Part 5.2

**Question: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

When a client submits a POST request to register a sensor with a `roomId` that does not correspond to any existing room, the semantically correct response is `422 Unprocessable Entity` rather than `404 Not Found`.

An HTTP `404` response communicates that the requested URI does not exist on the server. In this case, the URI `/api/v1/sensors` is a valid, accessible endpoint. The problem is not with the URL but with the content of the request body. The JSON payload was syntactically well-formed and was parsed successfully by the server; however, it failed semantic validation because the value provided for `roomId` does not reference a resource that exists in the system. The request is understood, but it cannot be acted upon because of a domain-level inconsistency within the payload itself.

HTTP 422 is specifically designed for this type of scenario. It signals that the server comprehended the request and its format was acceptable, but the instruction could not be carried out due to a logical failure in the submitted data. Using `404` in this context would incorrectly imply that the endpoint itself is missing, which would mislead the client. Using `422` communicates the actual problem precisely: the referenced entity within the payload does not exist.

---

### Part 5.3

**Question: A sensor currently marked with the status "MAINTENANCE" is physically disconnected and cannot accept new readings. Create a SensorUnavailableException and map this to an HTTP 403 Forbidden status when a POST reading is attempted.**

When a sensor is placed under maintenance, it is physically disconnected from the campus network and is therefore incapable of capturing or transmitting any new measurement data. If the API were to allow readings to be posted to such a sensor, the result would be fabricated or meaningless entries in the historical log, which would undermine the integrity of the entire dataset. To prevent this, the API enforces a state constraint that blocks any attempt to submit a reading to a sensor whose status is set to MAINTENANCE.

This constraint is implemented through a custom exception class called `SensorUnavailableException`, which extends `RuntimeException`. When a POST request is received at `POST /api/v1/sensors/{sensorId}/readings`, the `SensorReadingResource` class retrieves the sensor from the data store and inspects its current status before proceeding. If the status field is set to MAINTENANCE, the method immediately throws a `SensorUnavailableException` with a descriptive message, and no reading is stored.

A dedicated `ExceptionMapper` class named `SensorUnavailableExceptionMapper` is registered with the JAX-RS runtime through the `@Provider` annotation. This mapper intercepts the thrown exception and constructs an HTTP response with a status code of `403 Forbidden` along with a JSON body that clearly explains the reason for the rejection.

The choice of `403 Forbidden` is semantically deliberate. The server fully understood the request — the endpoint exists, the sensor exists, and the JSON body is valid. The issue is not a missing resource or a malformed payload; it is the current operational state of the sensor that explicitly prohibits this particular action. The `403` status communicates that the server is capable of processing the request type but is refusing to do so based on the business rules governing the sensor's current state. This is distinct from a `404`, which would imply the resource cannot be found, or a `422`, which would imply the payload itself is logically invalid. By using a dedicated custom exception and mapper for this scenario, the error handling logic remains decoupled from the resource method, consistent with the approach used throughout the rest of the API.

---

### Part 5.4

**Question: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**

Exposing raw Java stack traces in API responses represents a significant security vulnerability. A stack trace contains several categories of sensitive information that an attacker can directly exploit.

**Internal class and package names** disclose the architectural structure of the application, revealing how the codebase is organised and where sensitive logic is implemented. **Method names and line numbers** indicate the precise location in the source code where an error occurred, allowing an attacker to focus analysis on specific code paths when crafting an exploit. **Third-party library names and version numbers** reveal which dependencies the application relies on. If a known CVE exists for any of those versions, the attacker can immediately apply a documented exploit without needing to discover the vulnerability independently. **Internal file system paths** reveal the server's directory layout, which can be leveraged in path traversal or file inclusion attacks. **Logic flow information** — such as which conditional branch caused an error — helps an attacker understand the internal processing rules of the application and craft inputs designed to trigger specific weaknesses.

To eliminate this risk, this API implements a `GlobalExceptionMapper` that intercepts all `Throwable` types. The complete stack trace is written only to the server-side log via `java.util.logging.Logger`, where it remains accessible to administrators for debugging. The client receives only a safe, generic `500 Internal Server Error` JSON response containing no technical details, ensuring that no internal implementation information is ever exposed externally.

---

### Part 5.5

**Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger statements inside every resource method?**

Logging is a cross-cutting concern — it is a behaviour that applies uniformly across every endpoint in the API, independent of any specific business logic. Implementing it through a JAX-RS filter that implements both `ContainerRequestFilter` and `ContainerResponseFilter` means the logging behaviour is defined once in a single dedicated class and applied automatically to every HTTP request and response without any modification to the resource methods.

If logging were instead handled by inserting `Logger.info()` statements manually inside each resource method, the same code would be repeated across every endpoint. This not only increases the size and redundancy of the codebase but also introduces the risk of inconsistency — some methods may log differently or not at all if a developer forgets to add the statement. If the logging format ever needs to change, every resource method across the entire project must be individually updated.

There is also a functional limitation to in-method logging. A resource method is only executed when a request successfully reaches it. Requests rejected earlier by the framework — for example, due to a content-type mismatch returning a `415` response — never invoke the resource method, so any logging inside that method is never triggered. A filter operates at the framework level, prior to method dispatch, and captures every request and response without exception. This produces a complete and accurate audit trail of all API traffic, which method-level logging alone cannot guarantee.

---

*Module: 5COSC022W Client-Server Architectures | University of Westminster | 2025/26*

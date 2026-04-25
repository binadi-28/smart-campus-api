package com.example.smartcampus.resource;

import com.example.smartcampus.exception.LinkedResourceNotFoundException;
import com.example.smartcampus.exception.SensorUnavailableException;
import com.example.smartcampus.model.Sensor;
import com.example.smartcampus.model.SensorReading;
import com.example.smartcampus.repository.InMemoryStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
    private final InMemoryStore store = InMemoryStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadings() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Sensor with id " + sensorId + " was not found.");
        }
        return store.getReadingsForSensor(sensorId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Sensor with id " + sensorId + " was not found.");
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor is in MAINTENANCE and cannot accept readings.");
        }

        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        store.addReading(sensorId, reading);
        sensor.setCurrentValue(reading.getValue());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("readingId", reading.getId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}

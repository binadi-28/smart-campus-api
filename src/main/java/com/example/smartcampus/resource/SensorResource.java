package com.example.smartcampus.resource;

import com.example.smartcampus.exception.LinkedResourceNotFoundException;
import com.example.smartcampus.model.Sensor;
import com.example.smartcampus.repository.InMemoryStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {
    private final InMemoryStore store = InMemoryStore.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException("Cannot create sensor. Referenced roomId does not exist.");
        }

        store.addSensor(sensor);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Sensor created successfully.");
        response.put("sensorId", sensor.getId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = store.getAllSensors();
        if (type == null || type.trim().isEmpty()) {
            return sensors;
        }

        List<Sensor> filtered = new ArrayList<>();
        for (Sensor sensor : sensors) {
            if (type.equalsIgnoreCase(sensor.getType())) {
                filtered.add(sensor);
            }
        }
        return filtered;
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}

package com.example.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> discover() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Smart Campus API");
        response.put("version", "v1");
        response.put("contact", "facilities@westminster.ac.uk");

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        response.put("links", links);
        return response;
    }
}

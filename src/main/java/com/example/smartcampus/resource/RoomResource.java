package com.example.smartcampus.resource;

import com.example.smartcampus.exception.LinkedResourceNotFoundException;
import com.example.smartcampus.exception.RoomNotEmptyException;
import com.example.smartcampus.model.Room;
import com.example.smartcampus.repository.InMemoryStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {
    private final InMemoryStore store = InMemoryStore.getInstance();

    @GET
    public List<Room> getAllRooms() {
        return store.getAllRooms();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        store.addRoom(room);
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("roomId", room.getId());
        return Response.created(location).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public Room getRoomById(@PathParam("id") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room with id " + roomId + " was not found.");
        }
        return room;
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room with id " + roomId + " was not found.");
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room cannot be deleted because sensors are still assigned.");
        }

        store.removeRoom(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Room deleted successfully.");
        response.put("roomId", roomId);
        return Response.ok(response).build();
    }
}

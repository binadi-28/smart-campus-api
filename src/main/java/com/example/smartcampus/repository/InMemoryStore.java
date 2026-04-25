package com.example.smartcampus.repository;

import com.example.smartcampus.model.Room;
import com.example.smartcampus.model.Sensor;
import com.example.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStore {
    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Sensor> sensors = new HashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    private InMemoryStore() {
        Room demoRoom = new Room("LIB-301", "Library Quiet Study", 120);
        rooms.put(demoRoom.getId(), demoRoom);

        Sensor demoSensor = new Sensor("CO2-001", "CO2", "ACTIVE", 0.0, "LIB-301");
        sensors.put(demoSensor.getId(), demoSensor);
        demoRoom.getSensorIds().add(demoSensor.getId());

        sensorReadings.put(demoSensor.getId(), new ArrayList<SensorReading>());
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    public synchronized List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public synchronized Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public synchronized void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public synchronized Room removeRoom(String roomId) {
        return rooms.remove(roomId);
    }

    public synchronized List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public synchronized Sensor getSensor(String sensorId) {
        return sensors.get(sensorId);
    }

    public synchronized void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }
        sensorReadings.put(sensor.getId(), new ArrayList<SensorReading>());
    }

    public synchronized List<SensorReading> getReadingsForSensor(String sensorId) {
        List<SensorReading> readings = sensorReadings.get(sensorId);
        if (readings == null) {
            readings = new ArrayList<>();
            sensorReadings.put(sensorId, readings);
        }
        return new ArrayList<>(readings);
    }

    public synchronized void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = sensorReadings.get(sensorId);
        if (readings == null) {
            readings = new ArrayList<>();
            sensorReadings.put(sensorId, readings);
        }
        readings.add(reading);
    }
}

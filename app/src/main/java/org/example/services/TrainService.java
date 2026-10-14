package org.example.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entities.Train;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {

    private final List<Train> trainList;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TRAIN_DB_PATH = "src/main/java/org/example/localDb/trains.json";

    public TrainService() throws IOException{
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        File trains = new File(TRAIN_DB_PATH);

        System.out.println("Loading from: " + trains.getAbsolutePath());
        System.out.println("File exists: " + trains.exists());

        trainList = objectMapper.readValue(trains, new TypeReference<List<Train>>() {});

        System.out.println("Total trains loaded: " + trainList.size());

        for(Train train : trainList){
            System.out.println("Stations: " + train.getStations());
        }
    }

    public List<Train> searchTrains(String source, String destination) {
        return trainList.stream().filter(train -> validTrain(train, source, destination)).collect(Collectors.toList());
    }

    public Optional<Train> getTrainById(String trainId){
        return trainList.stream()
                        .filter(train -> train.getTrainId().equalsIgnoreCase(trainId))
                        .findFirst();
    }

    public void addTrain(Train newTrain) throws IOException{
        // Check if a train with the same trainId already exists
        Optional<Train> existingTrain = trainList.stream()
                .filter(train -> train.getTrainId().equalsIgnoreCase(newTrain.getTrainId()))
                .findFirst();

        if (existingTrain.isPresent()) {
            // If a train with the same trainId exists, update it instead of adding a new one
            updateTrain(newTrain);
        } else {
            // Otherwise, add the new train to the list
            trainList.add(newTrain);
            saveTrainListToFile();
        }
    }

    public void updateTrain(Train updatedTrain) throws IOException{
        // Find the index of the train with the same trainId
        OptionalInt index = IntStream.range(0, trainList.size())
                .filter(i -> trainList.get(i).getTrainId().equalsIgnoreCase(updatedTrain.getTrainId()))
                .findFirst();

        if (index.isPresent()) {
            // If found, replace the existing train with the updated one
            trainList.set(index.getAsInt(), updatedTrain);
            saveTrainListToFile();
        } else {
            // If not found, treat it as adding a new train
            addTrain(updatedTrain);
        }
    }

    private void saveTrainListToFile() throws IOException{
        objectMapper.writeValue(new File(TRAIN_DB_PATH), trainList);
    }

    
private boolean validTrain(Train train, String source, String destination) {
    List<String> stationList = train.getStations();

    if (stationList == null || source == null || destination == null) {
        return false;
    }

    int sourceIndex = -1;
    int destinationIndex = -1;

    for (int i = 0; i < stationList.size(); i++) {
        String station = stationList.get(i);

        if (station == null) {
            continue;
        }

        if (station.equalsIgnoreCase(source.trim())) {
            sourceIndex = i;
        }

        if (station.equalsIgnoreCase(destination.trim())) {
            destinationIndex = i;
        }
    }

    return sourceIndex != -1
            && destinationIndex != -1
            && sourceIndex < destinationIndex;
}


    public boolean bookTickets(Train train, int row, int seat) {
        // getting the seats from the train
        List<List<Integer>> seats = train.getSeats();
        try{
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);
                    addTrain(train);
                    return true;
                }
            }
            return false;
        }catch (Exception e){
            System.out.println("Error in bookTickets: " + e.getMessage());
            return false;
        }
    }

}

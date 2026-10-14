package org.example.services;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.example.util.UserServiceUtil;
import org.example.entities.User;
import org.example.entities.Ticket;
import org.example.entities.Train;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class UserBookingService {

    private User user;
    private List<User> userList;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_FILE_PATH = "src/main/java/org/example/localDb/users.json";

     public UserBookingService() throws IOException{
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadUsers();
    }
    private void loadUsers() throws IOException{
        userList = objectMapper.readValue(new File(USER_FILE_PATH), new TypeReference<List<User>>() {});
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    private boolean updateSeat(Train train, int row, int seat, int newValue) {
        List<Integer> rowSeats = train.getSeats().get(row);
        int oldValue = rowSeats.get(seat);
        rowSeats.set(seat, newValue);                 // change in memory
        try {
            new TrainService().addTrain(train);       // save to trains.json
            return true;
        } catch (IOException ex) {
            rowSeats.set(seat, oldValue);             // save failed: put it back
            System.out.println("Could not save the train data: " + ex.getMessage());
            return false;
        }
    }
    public boolean signUp(User user) throws IOException{
        try{
            Optional<User> foundUser = userList.stream().filter(user1 -> {
                return user1.getName().equals(user.getName());
            }).findFirst();

            if (foundUser.isPresent()) {
                // If a user with the same username exists,this will print an error message
                System.out.println("Username already taken!");
                return false;
            }

            userList.add(user);
            saveUserListToFile();
        }catch (Exception ex){
            System.out.println("saving user list to file failed " + ex.getMessage());
            return false;
        }
        return true;
    }

    private void saveUserListToFile() throws IOException{
        File usersFile = new File(USER_FILE_PATH);
        objectMapper.writeValue(usersFile, userList);
    }

    public void fetchBookings(){
        if (user == null) {
            System.out.println("Please login first.");
            return;
        }
        System.out.println("Fetching your bookings");
        user.printTickets();
    }

    public Optional<User> getUserByUsername(String username){
        return userList.stream().filter(user -> user.getName().equals(username)).findFirst();
    }

    public void setUser(User user){
        this.user = user;
    }

    private boolean releaseSeat(Ticket ticket) {
        Integer row = ticket.getRow();
        Integer seat = ticket.getSeat();
        if (row == null || seat == null || ticket.getTrain() == null) {
            System.out.println("This ticket has no seat number saved, so no seat was freed.");
            return true;
        }
        try{
            TrainService trainService = new TrainService();
            Optional<Train> current = trainService.getTrainById(ticket.getTrain().getTrainId());
            if (current.isEmpty()) {
                System.out.println("Train not found, so no seat was freed.");
                return true;
            }
            Train train = current.get();
            List<List<Integer>> seats = train.getSeats();
            if (row < 0 || row >= seats.size() || seat < 0 || seat >= seats.get(row).size()) {
                System.out.println("Seat position is invalid, so no seat was freed.");
                return true;
            }
            return updateSeat(train, row, seat, 0);
        }catch (IOException ex) {
            System.out.println("Could not read the train data: " + ex.getMessage());
            return false;
        }
    }

    public boolean cancelBooking(String ticketId) {
        if (user == null) {
            System.out.println("Please login first.");
            return false;
        }
        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return false;
        }

        Optional<Ticket> found = user.getTicketsBooked().stream()
                                                        .filter(t -> t.getTicketId().equals(ticketId))
                                                        .findFirst();
        if(found.isEmpty()){
            System.out.println("No ticket found with ID " + ticketId);
            return false;
        }
        Ticket ticket = found.get();

        user.getTicketsBooked().remove(ticket);
        try {
            saveUserListToFile();
        } catch (IOException ex) {
            user.getTicketsBooked().add(ticket);
            System.out.println("Could not save the cancellation: " + ex.getMessage());
            return false;
        }
        
        if (!releaseSeat(ticket)) {
            user.getTicketsBooked().add(ticket);
            try {
                saveUserListToFile();
            } catch (IOException ex) {
                System.out.println("Warning: ticket " + ticketId + " could not be restored on disk.");
            }
            System.out.println("Could not free the seat, so the ticket was kept. Try again.");
            return false;
        }

        System.out.println("Ticket with ID " + ticketId + " has been cancelled.");
        return true;
    }

    

    public List<Train> getTrains (String source, String destination) throws IOException {
        try{
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source,destination);
        }catch (Exception ex){
            System.out.println("There is something wrong!");
            // return empty list if there is an exception
            return Collections.emptyList();
        }
    }

    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }

    public boolean bookTrainSeat(Train train, int row, int seat, String source, String destination, String dateOfTravel) {
        if(user == null){
            System.out.println("Please login first.");
            return false;
        }
        List<List<Integer>> seats = train.getSeats();
        if(row < 0 || row >= seats.size() || seat < 0 || seat >= seats.get(row).size()){
            return false;
        }
        if(seats.get(row).get(seat) != 0){
            return false;
        }

        Ticket ticket = new Ticket();
        ticket.setSource(source);
        ticket.setDestination(destination);
        ticket.setDateOfTravel(dateOfTravel);
        ticket.setTrain(train);
        ticket.setUserId(user.getUserId());
        ticket.setRow(row);
        ticket.setSeat(seat);
        ticket.setTicketId(UserServiceUtil.generateTicketId());

        if (!updateSeat(train, row, seat, 1)) {
            return false;
        }

        user.getTicketsBooked().add(ticket);
        try{
            saveUserListToFile();
        } catch (IOException ex) {
            user.getTicketsBooked().remove(ticket);
            updateSeat(train, row, seat, 0);
            System.out.println("Could not save your ticket: " + ex.getMessage());
            return false;
        }

        System.out.println("Seat booked successfully!");
        System.out.println(ticket.getTicketInfo());
        return true;
    }
    
}

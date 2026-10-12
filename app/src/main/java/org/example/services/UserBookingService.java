package org.example.services;

import java.io.File;
import java.util.*;

import org.example.entities.User;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
;

public class UserBookingService {

    private User user;

    private List<User> userList;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String USERS_PATH = "../localDb/users.json";

    public UserBookingService(User user){
        this.user = user;

        File users = new File(USERS_PATH);

        userList = objectMapper.readValue(users, new TypeReference<List<User>>() {});
    }

}

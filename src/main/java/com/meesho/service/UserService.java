package com.meesho.service;

import com.meesho.enums.GenderType;
import com.meesho.models.User;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class UserService {
    private final ConcurrentHashMap<Long, User> userMap = new ConcurrentHashMap<>();

    public UserService(final List<User> users) {
        users.forEach(user -> userMap.put(user.getPhoneNumber(), user));
    }

    public User getUserByPhoneNumber(final Long phoneNumber) {
        return userMap.get(phoneNumber);
    }

    public void addUser(final User user) {
        userMap.put(user.getPhoneNumber(), user);
    }

    public void removeUser(final Long phoneNumber) {
        userMap.remove(phoneNumber);
    }

    public void registerUser(String name, GenderType genderType, Long phoneNumber, int pincode) {
        addUser(new User(name, genderType, phoneNumber, pincode));
    }
}

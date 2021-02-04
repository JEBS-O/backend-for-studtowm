package com.studmisto.controllers;

import com.studmisto.entities.Room;
import com.studmisto.entities.User;
import com.studmisto.entities.enums.*;
import com.studmisto.services.RoomService;
import com.studmisto.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoomService roomService;

    @GetMapping("/principal")
    public Map<String, Object> getPrincipalUser(@AuthenticationPrincipal User user) {
        return userService.getBasicUserInfo(user);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable("id") User user) {
        return userService.getBasicUserInfo(user);
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllUsers() {
        return userService.getAll();
    }

    @PostMapping("/addStaff")
    public Map<Object, Object> addStaff(@RequestParam("firstName") String firstName,
                                  @RequestParam("secondName") String secondName,
                                  @RequestParam("email") String email,
                                  @RequestParam("gender") String gender,
                                  @RequestParam("position") String position,
                                  @RequestParam("status") String status,
                                  @RequestParam("password") String password) {
        Map<Object, Object> answer = new HashMap<>();
        answer.put("firstName", firstName);
        answer.put("secondName", secondName);
        answer.put("email", email);
        answer.put("gender", gender);
        answer.put("position", position);
        answer.put("status", status);
        try {
            User user = new User();
            user.setFirstName(firstName);
            user.setSecondName(secondName);
            user.setEmail(email);
            user.setPosition(position);
            user.setGender(Gender.getGender(gender));
            user.setRole(Role.ADMIN);
            user.setStatus(Status.getStatus(status));
            user.setPassword(new BCryptPasswordEncoder(12).encode(password));
            userService.save(user);
            answer.put("message", "Користувач доданий");
        } catch(IllegalArgumentException e) {
            answer.put("errorMessage", e.getMessage());
        }
        return answer;
    }

    @PostMapping("/addExistInmate")
    public Map<String, Object> addExistInmate(@RequestParam("firstName") String firstName,
                                  @RequestParam("secondName") String secondName,
                                  @RequestParam("email") String email,
                                  @RequestParam("gender") String gender,
                                  @RequestParam("groupName") String groupName,
                                  @RequestParam("institute") String institute,
                                  @RequestParam("roomNumber") Integer roomNumber,
                                  @RequestParam("dorm") String dorm,
                                  @RequestParam("password") String password,
                                  @RequestParam("position") String position,
                                  @RequestParam("status") String status,
                                  @RequestParam("balance") Double balance,
                                  @RequestParam("tariff") String tariff) {
        Map<String, Object> answer = new HashMap<>();
        answer.put("firstName", firstName);
        answer.put("secondName", secondName);
        answer.put("email", email);
        answer.put("gender", gender);
        answer.put("groupName", groupName);
        answer.put("institute", institute);
        answer.put("roomNumber", roomNumber);
        answer.put("dorm", dorm);
        answer.put("position", position);
        answer.put("status", status);
        answer.put("balance", balance);
        answer.put("tariff", tariff);

        User user = new User();
        try {
            if(roomService.checkRoomForUser(Gender.getGender(gender), roomService.getRoom(roomNumber, Dorm.getDorm(dorm)))) {
                user.setFirstName(firstName);
                user.setSecondName(secondName);
                user.setEmail(email);
                user.setGender(Gender.getGender(gender));
                user.setGroupName(groupName);
                user.setInstitute(Institute.getInstitute(institute));
                user.setStatus(Status.getStatus(status));
                user.setPosition(position);
                user.setRoom(roomService.giveRoomForUser(Gender.getGender(gender), roomService.getRoom(roomNumber, Dorm.getDorm(dorm))));
                user.setBalance(balance);
                user.setTariff(Tariff.getTariff(tariff));
                user.setRole(Role.USER);
                user.setPassword(new BCryptPasswordEncoder(12).encode(password));
                userService.save(user);
                answer.put("message", "Користувач доданий");
            } else {
                answer.put("errorMessage", "Користувач не може бути заселений у дану кімнату");
            }
        } catch(NullPointerException | IllegalArgumentException e) {
            answer.put("errorMessage", e.getMessage());
        } finally {
            if(user.getRoom() != null) {
                roomService.takeRoomFromUser(user, user.getRoom());
            }
        }
        return answer;
    }

    @PutMapping("/principal")
    public Map<String, String> updateUser(@AuthenticationPrincipal User user,
                                          @RequestParam(name = "password1", required = false) String password1,
                                          @RequestParam(name = "password2", required = false) String password2,
                                          @RequestParam(name = "email", required = false) String email,
                                          @RequestParam(name = "userpic", required = false) String userpic) {
        StringBuilder message = new StringBuilder();
        if(!password1.equals("")) {
            if(password1.equals(password2)) {
                user.setPassword(new BCryptPasswordEncoder(12).encode(password1));
                message.append("Пароль змінено. ");
            } else {
                message.append("Паролі не збігаються. ");
            }
        }
        if(!email.equals(user.getEmail()) && userService.loadUserByUsername(email) == null) {
            try {
                user.setEmail(email);
            } catch(Exception e) {
                message = new StringBuilder();
                message.append("Вказаний некоректний email. ");
            }
        } else {
            message.append("Користувач з таким адресом електронної пошти існує. ");
        }
        if(!userpic.equals(user.getUserpic()) && !userpic.equals("")) {
            user.setUserpic(userpic);
            message.append("Посилання на аватар змінено. ");
        }
        if(!message.toString().equals("")) {
            userService.save(user);
        }
        return Map.of("message", message.toString());
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable("id") User user,
                                          @RequestParam(value = "institute", required = false, defaultValue = "") String institute,
                                          @RequestParam(value = "groupName", required = false, defaultValue = "") String groupName,
                                          @RequestParam(value = "tariff", required = false, defaultValue = "") String tariff,
                                          @RequestParam(value = "dorm", required = false, defaultValue = "") String dorm,
                                          @RequestParam(value = "roomNumber", required = false, defaultValue = "") Integer roomNumber,
                                          @RequestParam(value = "status", required = false, defaultValue = "") String status,
                                          @RequestParam(value = "position", required = false, defaultValue = "") String position) {
        StringBuilder message = new StringBuilder();
        try {
            if(user.getStatus().getName().equals(Status.INMATE.getName()) || user.getStatus().getName().equals(Status.WAITING_FOR_ORDER.getName())) {
                if(!user.getInstitute().getName().equals(institute) && !institute.equals("")) {
                    user.setInstitute(Institute.getInstitute(institute));
                    message.append("Інститут змінено. ");
                }
                if(!user.getGroupName().equals(groupName) && !groupName.equals("")) {
                    user.setGroupName(groupName);
                    message.append("Групу змінено. ");
                }
                if(!user.getTariff().getDescription().equals(tariff) && !tariff.equals("")) {
                    user.setTariff(Tariff.getTariff(tariff));
                    message.append("Тариф змінено. ");
                }
                if(((user.getRoom().getRoomNumber().intValue() != roomNumber.intValue()) || (!user.getRoom().getDorm().getAddress().equals(dorm))) && (!dorm.equals("")) && (roomNumber == 0)) {
                    Room room = roomService.getRoom(roomNumber, Dorm.getDorm(dorm));
                    if(roomService.checkRoomForUser(user.getGender(), room)) {
                        roomService.takeRoomFromUser(user, user.getRoom());
                        roomService.giveRoomForUser(user.getGender(), room);
                        user.setRoom(room);
                        message.append("Кімнату змінено. ");
                    } else {
                        message.append("В цю кімнату не можна поселити даного мешканця. Виберіть, будь ласка, іншу. ");
                    }
                }
                if(!user.getStatus().getName().equals(status) && !status.equals("")) {
                    if(status.equals(Status.WAITING_FOR_ORDER.getName()) || status.equals(Status.INMATE.getName())) {
                        user.setStatus(Status.getStatus(status));
                        message.append("Статус мешканця змінено на ");
                        message.append(status);
                    } else {
                        message.append("Статус мешканця не може бути змінений на ");
                        message.append(status);
                    }
                }
            }
            if(!status.equals("") && user.getStatus() != Status.getStatus(status) && (user.getStatus() == Status.STAFF_3 || user.getStatus() == Status.STAFF_5 || user.getStatus() == Status.STAFF_6 || user.getStatus() == Status.STAFF_7 || user.getStatus() == Status.ADMINISTRATION)) {
                Status status1 = Status.getStatus(status);
                if(status1 == Status.STAFF_3 || user.getStatus() == Status.STAFF_5 || user.getStatus() == Status.STAFF_6 || user.getStatus() == Status.STAFF_7 || user.getStatus() == Status.ADMINISTRATION) {
                    user.setStatus(status1);
                    message.append("Статус особи адміністрації/персоналу студмістечка змінений на ");
                    message.append(status);
                } else {
                    message.append("Статус особи адміністрації/персоналу студмістечка не може бути змінений на ");
                    message.append(status);
                }
            }
            if(!user.getPosition().equals(position) && !position.equals("")) {
                user.setPosition(position);
                message.append("Позицію користувача змінено. ");
            }
            if(!message.toString().equals("")) {
                userService.save(user);
            } else {
                message.append("Нічого не змінено. ");
            }
            return Map.of("message", message.toString());
        } catch(IllegalArgumentException | NullPointerException e) {
            message = new StringBuilder();
            message.append(e);
            return Map.of("errorMessage", message.toString());
        }
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable("id") User user) {
        roomService.takeRoomFromUser(user, user.getRoom());
        userService.delete(user);
    }
}
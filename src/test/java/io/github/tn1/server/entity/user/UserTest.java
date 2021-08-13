package io.github.tn1.server.entity.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Builder를 활용하여 User객체를 생성하는 메소드")
    void createByBuilder() {
        //given
        String email = "test@gmail.com";
        String accountNumber = "47------------KB";
        Role role = Role.ROLE_USER;
        String deviceToken = "test token";
        boolean alarm = true;
        LocalDate blackDate = LocalDate.of(2021, 8, 13);

        // when
        User user = User.builder()
                .email(email)
                .role(role)
                .gcn("2114")
                .accountNumber(accountNumber)
                .name("이**")
                .roomNumber("318")
                .deviceToken(deviceToken)
                .alarm(alarm)
                .blackDate(blackDate)
                .build();

        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(email, user.getEmail()),
                () -> Assertions.assertEquals(accountNumber, user.getAccountNumber()),
                () -> Assertions.assertEquals(role, user.getRole()),
                () -> Assertions.assertEquals(deviceToken, user.getDeviceToken()),
                () -> Assertions.assertEquals(alarm, user.isAlarm()),
                () -> Assertions.assertEquals(blackDate, user.getBlackDate())
        );
    }

}
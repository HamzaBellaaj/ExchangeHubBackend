package com.exchangeHub.Backend.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CvProfileDto {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String profileTitle;
    private String summary;
}

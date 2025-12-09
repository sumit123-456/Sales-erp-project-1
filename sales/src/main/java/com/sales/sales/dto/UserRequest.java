package com.sales.sales.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserRequest {

    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String role;
    private Integer callTarget;
    private Integer monthlyTarget;
    private String teamAllocation;
}

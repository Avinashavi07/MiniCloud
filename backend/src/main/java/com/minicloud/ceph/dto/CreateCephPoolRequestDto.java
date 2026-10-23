package com.minicloud.ceph.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateCephPoolRequestDto {

    @NotBlank
    @Pattern(regexp = "^mc-[A-Za-z0-9][A-Za-z0-9_.-]{0,60}$", message = "must be mc-*")
    private String name;

    @Min(1)
    @Max(128)
    private int pgNum = 8;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPgNum() {
        return pgNum;
    }

    public void setPgNum(int pgNum) {
        this.pgNum = pgNum;
    }
}

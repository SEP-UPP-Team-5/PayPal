package com.PayPal.model;


import com.PayPal.model.enums.IntervalUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Frequency {
    private IntervalUnit interval_unit; //MONTH
    private Integer interval_count;
}
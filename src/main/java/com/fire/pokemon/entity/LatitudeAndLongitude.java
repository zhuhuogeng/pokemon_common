package com.fire.pokemon.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhuhuogeng
 * @date 16/7/25.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatitudeAndLongitude {

    /**
     * 最小纬度
     */
    private double minLatitude;

    /**
     * 最大纬度
     */
    private double maxLatitude;

    /**
     * 最小精度
     */
    private double minLongitude;

    /**
     * 最大精度
     */
    private double maxLongitude;

}

package com.fire.pokemon.entity;

import lombok.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author zhuhuogeng
 * @date 16/7/24.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pokemon {
    /**
     * id
     */
    private long id;

    /**
     * 精灵Id
     */
    private String pokemonId;

    /**
     * 精灵名称
     */
    private int pokemonName;

    /**
     * 经度
     */
    private double latitude;

    /**
     * 纬度
     */
    private double longitude;

    /**
     * 消失时间戳(unix时间)
     */
    private long expiration_time;

    /**
     * 消失时间(HH:mm:ss)
     */
    private String expirationTimeStr;

    /**
     * 是否有效,true,false
     */
    private String is_alive;

    /**
     * 不知道这个是什么...
     */
    private String uid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Pokemon pokemon = (Pokemon) o;

        return new EqualsBuilder()
                .append(getLatitude(), pokemon.getLatitude())
                .append(getLongitude(), pokemon.getLongitude())
                .append(getPokemonId(), pokemon.getPokemonId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getPokemonId())
                .append(getLatitude())
                .append(getLongitude())
                .toHashCode();
    }
}
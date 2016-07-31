package com.fire.pokemon.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fire.pokemon.entity.Pokemon;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @author zhuhuogeng
 * @date 16/7/30.
 */
@Slf4j
public class AnalysisPokemon {

    /**
     * 解析精灵
     *
     * @param pokemonStr
     * @return
     */
    public static Set<Pokemon> analysisPokemon(String pokemonStr) {
        try {
            JSONObject pokemonObject = (JSONObject) JSONObject.parse(pokemonStr);
            String status = (String) pokemonObject.get("status");
            if (!StringUtils.equals("success", status)) {
                return Sets.newHashSet();
            }
            JSONArray pokemonJson = (JSONArray) pokemonObject.get("pokemon");
            if (pokemonJson.size() < 1) {
                return Sets.newHashSet();
            }
            Set<Pokemon> pokemonSet = pokemonJson.stream().map(p -> {
                Pokemon pokemon = JSON.parseObject(p.toString(), Pokemon.class, Feature.SortFeidFastMatch);
                long expirationTimeInMillis = pokemon.getExpiration_time() * 1000;
                String expirationTimeStr = analysisExpirationTime(expirationTimeInMillis);
                pokemon.setExpirationTimeStr(expirationTimeStr);
                return pokemon;
            }).collect(toSet());
            pokemonSet.removeIf(p -> StringUtils.equals(p.getIs_alive(), "false"));
            return pokemonSet;
        } catch (Exception e) {
            log.error("error");
            return Sets.newHashSet();
        }
    }

    /**
     * 解析消失时间
     *
     * @param expirationTimeInMillis
     * @return
     */
    private static String analysisExpirationTime(long expirationTimeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expirationTimeInMillis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return hour + ":" + minute + ":" + second;
    }

}

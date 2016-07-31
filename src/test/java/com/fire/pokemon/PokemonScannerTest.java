package com.fire.pokemon;

import POGOProtos.Map.Fort.FortDataOuterClass.FortData;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass.NearbyPokemon;
import com.fire.pokemon.data.RarePokemonIdAndChName;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.Collection;

/**
 * @author zhuhuogeng
 * @date 16/7/24.
 */
@Slf4j
public class PokemonScannerTest {

    private static final String LOGIN = "327872612";
    private static final String PASSWORD = "bmzy10710ZHG";
//    private static double LATITUDE = -34.916767;
//    private static double LONGITUDE = 138.595291;
    private static double LATITUDE = 37.831683;
    private static double LONGITUDE = -122.270536;


    @Test
    public void pokemonScanner() {
        OkHttpClient httpClient = new OkHttpClient();
        int maxErrorCount = 5;
        int errorCount = 0;
        while (errorCount < maxErrorCount) {
            try {
                PokemonGo go = new PokemonGo(new PtcCredentialProvider(httpClient, LOGIN, PASSWORD), httpClient);
                go.setLatitude(LATITUDE);
                go.setLongitude(LONGITUDE);
                Map map = new Map(go);
                MapObjects mapObjects = map.getMapObjects();

                final Collection<MapPokemonOuterClass.MapPokemon> collectionPokemon = mapObjects.getCatchablePokemons();
                final Collection<FortData> collectionGyms = mapObjects.getGyms();
                final Collection<Pokestop> collectionPokeStops = mapObjects.getPokestops();

                for (MapPokemonOuterClass.MapPokemon pokemon : collectionPokemon) {
                    log.info("【{}】消失时间:{},坐标:{},{}", RarePokemonIdAndChName.map.get(pokemon.getPokemonId()),
                            pokemon.getExpirationTimestampMs(), pokemon.getLatitude(), pokemon.getLongitude());
                }
                for (FortData gym : collectionGyms) {

                }
                for (Pokestop pokestop : collectionPokeStops) {
                }
                break;
            } catch (LoginFailedException e) {
                log.error("登录出错:{}", e.getMessage());
                errorCount++;
            } catch (RemoteServerException e) {
                log.error("远程调用服务出错:{}", e.getMessage());
                errorCount++;
            }
        }
    }
}

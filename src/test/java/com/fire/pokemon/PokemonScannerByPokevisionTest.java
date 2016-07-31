package com.fire.pokemon;

import com.fire.pokemon.data.PokemonIdAndName;
import com.fire.pokemon.data.RarePokemonIdAndChName;
import com.fire.pokemon.entity.LatitudeAndLongitude;
import com.fire.pokemon.entity.Pokemon;
import com.fire.pokemon.service.AnalysisPokemon;
import com.fire.util.SendRequestUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus.CATCH_SUCCESS;

/**
 * @author zhuhuogeng
 * @date 16/7/24.
 */
@Slf4j
public class PokemonScannerByPokevisionTest {

    private static final String host = "https://pokevision.com/map/data/";
    private static List<LatitudeAndLongitude> latitudeAndLongitudeList = Lists.newArrayList();

    private static Map<String, String> needPokemonIdNameMap = Maps.newHashMap();
    private static OkHttpClient http = new OkHttpClient();
    private static PokemonGo go = null;
    private static int threadNum = 0;

    private static Set<Pokemon> catchPokemonSet = Sets.newConcurrentHashSet();

    public static void main(String[] args) {
        Set<Pokemon> existPokemonSet = Sets.newHashSet();
        initPokemonIdNameMap();
        initLatitudeAndLongitudeList();
        login();
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        executorService.submit(() -> {
            Thread.currentThread().setName("cache");
            catchPokemon();
        });
        while (true) {
            latitudeAndLongitudeList.forEach(l -> {
                for (double latitude=l.getMinLatitude(); latitude<l.getMaxLatitude(); latitude+=0.02) {
                    for (double longitude=l.getMinLongitude(); longitude<l.getMaxLongitude(); longitude+=0.02) {
                        double finalLatitude = latitude;
                        double finalLongitude = longitude;
                        executorService.submit(() -> {
                            Thread.currentThread().setName(finalLatitude + "," + finalLongitude);
                            getPokemon(existPokemonSet, httpClient, finalLatitude, finalLongitude);
                        });
                        threadNum++;
                        while (threadNum > 40) {
                            try {
                                Thread.currentThread().sleep(5000);
                            } catch (InterruptedException e) {
                                log.error("扫描出错:{}", e.getMessage());
                            }
                        }
                    }
                }
            });
        }
    }

    private static void getPokemon(Set<Pokemon> existPokemonSet, CloseableHttpClient httpClient, double latitude, double longitude) {
        String location = latitude + "/" + longitude;
        String result = SendRequestUtil.sendGetRequest(httpClient, host + location);
        Set<Pokemon> pokemonSet = AnalysisPokemon.analysisPokemon(result);
//        pokemonSet.removeIf(p -> !RarePokemonIdAndChName.map.containsKey(p.getPokemonId()));
        pokemonSet.removeIf(p -> !needPokemonIdNameMap.containsKey(p.getPokemonId()));
        pokemonSet.removeIf(p -> existPokemonSet.contains(p));
        pokemonSet.removeIf(p -> p.getExpiration_time() < System.currentTimeMillis() / 1000);
        existPokemonSet.addAll(pokemonSet);
        pokemonSet.forEach(p -> log.info("【{}】,消失时间:{},坐标:{},{}", needPokemonIdNameMap
                .get(p.getPokemonId()), p.getExpirationTimeStr(), p.getLatitude(), p.getLongitude()));

        catchPokemonSet.addAll(pokemonSet);
        threadNum--;
    }

    private static Object catchPokemon() {
        while (true) {
            try {
                if (catchPokemonSet.size() < 1) {
                    Thread.currentThread().sleep(5000);
                } else {
                    catchPokemonSet.forEach(pokemon -> {
                        BigDecimal bg1 = new BigDecimal(pokemon.getLatitude());
                        BigDecimal bg2 = new BigDecimal(pokemon.getLongitude());
                        catchPokemon(bg1.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue(),
                                bg2.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
                        catchPokemonSet.remove(pokemon);
                    });
                }
            } catch (Exception e) {
                log.error("抓捕出错:{}", e.getMessage());
            }
        }
    }

    private static void login() {
        while (go == null) {
            try {
                if (go == null) {
                    // ptc登录
                    log.info("登录ptc账号");
                    go = new PokemonGo(new PtcCredentialProvider(http, "goodbyeFire", "bmzy10710ZHG"), http);
                    // google登录
//                    GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(http);
//                    System.out.println("打开这个地址进行授权:" + provider.LOGIN_URL);
//                    System.out.println("输入你获得的授权码:");
//                    Scanner sc = new Scanner(System.in);
//                    String access = sc.nextLine();
//                    provider.login(access);
//                    log.info("google登录成功,你本次登录的token是:{}", provider.getRefreshToken());
//                    go = new PokemonGo(provider, http);
                }
            } catch (Exception e) {
                log.error("登录出错:{}", e.getMessage());
            }
        }
    }

    private static void catchPokemon(double latitude, double longitude) {
        try {
            go.setLocation(latitude, longitude, 0); // 飞过去
            List<CatchablePokemon> catchablePokemon = go.getMap().getCatchablePokemon();
            log.info("飞到[{},{}],发现{}只精灵", latitude, longitude, catchablePokemon.size());
            for (CatchablePokemon cp : catchablePokemon) {
                if (!needPokemonIdNameMap.keySet().contains(cp.getPokemonId().getNumber() + "")) {
                    continue;
                }
                try {
                    EncounterResult encResult = cp.encounterPokemon();
                    if (encResult.wasSuccessful()) {
                        log.info("点击{}", PokemonIdAndName.map.get(cp.getPokemonId().getNumber() + ""));
                        go.setLocation(37.788412, -122.407414, 0); // 回家
                        go.getMap().getCatchablePokemon();
                        CatchResult result = cp.catchPokemonWithRazzBerry();
                        log.info("抓捕{}{}", PokemonIdAndName.map.get(cp.getPokemonId().getNumber() + ""),
                                result.getStatus().equals(CATCH_SUCCESS) ? "成功" : "失败");
                    }
                } catch (Exception e) {
                    log.error("抓捕出错了:{}", e.getMessage());
                }

            }
            go.setLocation(37.788412, -122.407414, 0);
            go.getMap().getCatchablePokemon();
        } catch (LoginFailedException | RemoteServerException e) {
            log.error("{}", e);
            go.setLocation(37.788412, -122.407414, 0);
            try {
                go.getMap().getCatchablePokemon();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

    }


    /**
     * 初始化需要扫描的精灵的id和名称
     */
    private static void initPokemonIdNameMap() {
        needPokemonIdNameMap.put("3", "妙蛙花");
        needPokemonIdNameMap.put("6", "喷火龙");
        needPokemonIdNameMap.put("9", "水箭龟");
        needPokemonIdNameMap.put("26", "雷丘");
        needPokemonIdNameMap.put("38", "九尾");
        needPokemonIdNameMap.put("65", "胡迪");
        needPokemonIdNameMap.put("94", "耿鬼");
        needPokemonIdNameMap.put("130", "暴鲤龙");
        needPokemonIdNameMap.put("131", "乘龙");
        needPokemonIdNameMap.put("134", "水伊布");
        needPokemonIdNameMap.put("135", "雷伊布");
        needPokemonIdNameMap.put("136", "火伊布");
        needPokemonIdNameMap.put("142", "化石翼龙");
        needPokemonIdNameMap.put("143", "卡比兽");
        needPokemonIdNameMap.put("149", "快龙");
    }

    /**
     * 初始化扫描的坐标
     */
    private static void initLatitudeAndLongitudeList() {
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(-33.95247360616282, -33.77343983379775, 150.9143829345703, 151.25890731811523));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(51.40749369152795, 51.56255861691012, -0.30782958984375, 0.14556884765625));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(35.63776694133777, 35.88682489453265, 139.45838928222656, 139.932861328125));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(33.84703735381736, 34.07086232376631, -118.5150146484375, -118.14010620117188));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(-34.936185628294744, -34.906275829530244, 138.58858108520508, 138.6390495300293));
    }
}

package com.fire.pokemon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fire.pokemon.entity.LatitudeAndLongitude;
import com.fire.pokemon.entity.Pokemon;
import com.fire.util.SendRequestUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toSet;

/**
 * @author zhuhuogeng
 * @date 16/7/24.
 */
@Slf4j
public class PokemonScannerTest {

    private static final String host = "https://pokevision.com/map/data/";
    private static Map<String, String> pokemonIdNameMap = Maps.newHashMap();
    private static Map<String, String> rarePokemonIdNameMap = Maps.newHashMap();
    private static List<LatitudeAndLongitude> latitudeAndLongitudeList = Lists.newArrayList();

    @Test
    public void getLatitudeAndLongitude() {
        Set<Pokemon> existPokemonSet = Sets.newHashSet();
        initPokemonIdNameMap();
        initLatitudeAndLongitudeList();
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        latitudeAndLongitudeList.forEach(l -> {
            for (double latitude=l.getMinLatitude(); latitude<l.getMaxLatitude(); latitude+=0.02) {
                for (double longitude=l.getMinLongitude(); longitude<l.getMaxLongitude(); longitude+=0.02) {
                    double finalLatitude = latitude;
                    double finalLongitude = longitude;
                    executorService.submit(() -> {
                        String location = finalLatitude +"/"+ finalLongitude;
                        String result = SendRequestUtil.sendGetRequest(httpClient, host + location);
                        Set<Pokemon> pokemonSet = analysisPokemon(result);
                        pokemonSet.removeIf(p -> !rarePokemonIdNameMap.containsKey(p.getPokemonId()));
                        pokemonSet.removeIf(p -> existPokemonSet.contains(p));
                        existPokemonSet.addAll(pokemonSet);
                        pokemonSet.forEach(p -> log.info("名称:{},消失时间:{},坐标:{},{}",
                                rarePokemonIdNameMap.get(p.getPokemonId()), p.getExpirationTimeStr(), p.getLatitude(), p.getLongitude()));
                    });
                }
            }
        });
        while (!executorService.isShutdown());
    }

    /**
     * 解析精灵
     *
     * @param pokemonStr
     * @return
     */
    private Set<Pokemon> analysisPokemon(String pokemonStr) {
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
//            log.error("{}\n{}", e, pokemonStr);
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
    private String analysisExpirationTime(long expirationTimeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expirationTimeInMillis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return hour + ":" + minute + ":" + second;
    }

    /**
     * 初始化需要扫描的精灵的id和名称
     */
    private void initPokemonIdNameMap() {
        String str = "{\"1\":\"Bulbasaur\",\"2\":\"Ivysaur\",\"3\":\"Venusaur\",\"4\":\"Charmander\",\"5\":\"Charmeleon\",\"6\":\"Charizard\",\"7\":\"Squirtle\",\"8\":\"Wartortle\",\"9\":\"Blastoise\",\"10\":\"Caterpie\",\"11\":\"Metapod\",\"12\":\"Butterfree\",\"13\":\"Weedle\",\"14\":\"Kakuna\",\"15\":\"Beedrill\",\"16\":\"Pidgey\",\"17\":\"Pidgeotto\",\"18\":\"Pidgeot\",\"19\":\"Rattata\",\"20\":\"Raticate\",\"21\":\"Spearow\",\"22\":\"Fearow\",\"23\":\"Ekans\",\"24\":\"Arbok\",\"25\":\"Pikachu\",\"26\":\"Raichu\",\"27\":\"Sandshrew\",\"28\":\"Sandslash\",\"29\":\"Nidoran\\u2640\",\"30\":\"Nidorina\",\"31\":\"Nidoqueen\",\"32\":\"Nidoran\\u2642\",\"33\":\"Nidorino\",\"34\":\"Nidoking\",\"35\":\"Clefairy\",\"36\":\"Clefable\",\"37\":\"Vulpix\",\"38\":\"Ninetales\",\"39\":\"Jigglypuff\",\"40\":\"Wigglytuff\",\"41\":\"Zubat\",\"42\":\"Golbat\",\"43\":\"Oddish\",\"44\":\"Gloom\",\"45\":\"Vileplume\",\"46\":\"Paras\",\"47\":\"Parasect\",\"48\":\"Venonat\",\"49\":\"Venomoth\",\"50\":\"Diglett\",\"51\":\"Dugtrio\",\"52\":\"Meowth\",\"53\":\"Persian\",\"54\":\"Psyduck\",\"55\":\"Golduck\",\"56\":\"Mankey\",\"57\":\"Primeape\",\"58\":\"Growlithe\",\"59\":\"Arcanine\",\"60\":\"Poliwag\",\"61\":\"Poliwhirl\",\"62\":\"Poliwrath\",\"63\":\"Abra\",\"64\":\"Kadabra\",\"65\":\"Alakazam\",\"66\":\"Machop\",\"67\":\"Machoke\",\"68\":\"Machamp\",\"69\":\"Bellsprout\",\"70\":\"Weepinbell\",\"71\":\"Victreebel\",\"72\":\"Tentacool\",\"73\":\"Tentacruel\",\"74\":\"Geodude\",\"75\":\"Graveler\",\"76\":\"Golem\",\"77\":\"Ponyta\",\"78\":\"Rapidash\",\"79\":\"Slowpoke\",\"80\":\"Slowbro\",\"81\":\"Magnemite\",\"82\":\"Magneton\",\"83\":\"Farfetch'd\",\"84\":\"Doduo\",\"85\":\"Dodrio\",\"86\":\"Seel\",\"87\":\"Dewgong\",\"88\":\"Grimer\",\"89\":\"Muk\",\"90\":\"Shellder\",\"91\":\"Cloyster\",\"92\":\"Gastly\",\"93\":\"Haunter\",\"94\":\"Gengar\",\"95\":\"Onix\",\"96\":\"Drowzee\",\"97\":\"Hypno\",\"98\":\"Krabby\",\"99\":\"Kingler\",\"100\":\"Voltorb\",\"101\":\"Electrode\",\"102\":\"Exeggcute\",\"103\":\"Exeggutor\",\"104\":\"Cubone\",\"105\":\"Marowak\",\"106\":\"Hitmonlee\",\"107\":\"Hitmonchan\",\"108\":\"Lickitung\",\"109\":\"Koffing\",\"110\":\"Weezing\",\"111\":\"Rhyhorn\",\"112\":\"Rhydon\",\"113\":\"Chansey\",\"114\":\"Tangela\",\"115\":\"Kangaskhan\",\"116\":\"Horsea\",\"117\":\"Seadra\",\"118\":\"Goldeen\",\"119\":\"Seaking\",\"120\":\"Staryu\",\"121\":\"Starmie\",\"122\":\"Mr. Mime\",\"123\":\"Scyther\",\"124\":\"Jynx\",\"125\":\"Electabuzz\",\"126\":\"Magmar\",\"127\":\"Pinsir\",\"128\":\"Tauros\",\"129\":\"Magikarp\",\"130\":\"Gyarados\",\"131\":\"Lapras\",\"132\":\"Ditto\",\"133\":\"Eevee\",\"134\":\"Vaporeon\",\"135\":\"Jolteon\",\"136\":\"Flareon\",\"137\":\"Porygon\",\"138\":\"Omanyte\",\"139\":\"Omastar\",\"140\":\"Kabuto\",\"141\":\"Kabutops\",\"142\":\"Aerodactyl\",\"143\":\"Snorlax\",\"144\":\"Articuno\",\"145\":\"Zapdos\",\"146\":\"Moltres\",\"147\":\"Dratini\",\"148\":\"Dragonair\",\"149\":\"Dragonite\",\"150\":\"Mewtwo\",\"151\":\"Mew\",\"152\":\"Chikorita\",\"153\":\"Bayleef\",\"154\":\"Meganium\",\"155\":\"Cyndaquil\",\"156\":\"Quilava\",\"157\":\"Typhlosion\",\"158\":\"Totodile\",\"159\":\"Croconaw\",\"160\":\"Feraligatr\",\"161\":\"Sentret\",\"162\":\"Furret\",\"163\":\"Hoothoot\",\"164\":\"Noctowl\",\"165\":\"Ledyba\",\"166\":\"Ledian\",\"167\":\"Spinarak\",\"168\":\"Ariados\",\"169\":\"Crobat\",\"170\":\"Chinchou\",\"171\":\"Lanturn\",\"172\":\"Pichu\",\"173\":\"Cleffa\",\"174\":\"Igglybuff\",\"175\":\"Togepi\",\"176\":\"Togetic\",\"177\":\"Natu\",\"178\":\"Xatu\",\"179\":\"Mareep\",\"180\":\"Flaaffy\",\"181\":\"Ampharos\",\"182\":\"Bellossom\",\"183\":\"Marill\",\"184\":\"Azumarill\",\"185\":\"Sudowoodo\",\"186\":\"Politoed\",\"187\":\"Hoppip\",\"188\":\"Skiploom\",\"189\":\"Jumpluff\",\"190\":\"Aipom\",\"191\":\"Sunkern\",\"192\":\"Sunflora\",\"193\":\"Yanma\",\"194\":\"Wooper\",\"195\":\"Quagsire\",\"196\":\"Espeon\",\"197\":\"Umbreon\",\"198\":\"Murkrow\",\"199\":\"Slowking\",\"200\":\"Misdreavus\",\"201\":\"Unown\",\"202\":\"Wobbuffet\",\"203\":\"Girafarig\",\"204\":\"Pineco\",\"205\":\"Forretress\",\"206\":\"Dunsparce\",\"207\":\"Gligar\",\"208\":\"Steelix\",\"209\":\"Snubbull\",\"210\":\"Granbull\",\"211\":\"Qwilfish\",\"212\":\"Scizor\",\"213\":\"Shuckle\",\"214\":\"Heracross\",\"215\":\"Sneasel\",\"216\":\"Teddiursa\",\"217\":\"Ursaring\",\"218\":\"Slugma\",\"219\":\"Magcargo\",\"220\":\"Swinub\",\"221\":\"Piloswine\",\"222\":\"Corsola\",\"223\":\"Remoraid\",\"224\":\"Octillery\",\"225\":\"Delibird\",\"226\":\"Mantine\",\"227\":\"Skarmory\",\"228\":\"Houndour\",\"229\":\"Houndoom\",\"230\":\"Kingdra\",\"231\":\"Phanpy\",\"232\":\"Donphan\",\"233\":\"Porygon2\",\"234\":\"Stantler\",\"235\":\"Smeargle\",\"236\":\"Tyrogue\",\"237\":\"Hitmontop\",\"238\":\"Smoochum\",\"239\":\"Elekid\",\"240\":\"Magby\",\"241\":\"Miltank\",\"242\":\"Blissey\",\"243\":\"Raikou\",\"244\":\"Entei\",\"245\":\"Suicune\",\"246\":\"Larvitar\",\"247\":\"Pupitar\",\"248\":\"Tyranitar\",\"249\":\"Lugia\",\"250\":\"Ho-Oh\",\"251\":\"Celebi\",\"252\":\"Treecko\",\"253\":\"Grovyle\",\"254\":\"Sceptile\",\"255\":\"Torchic\",\"256\":\"Combusken\",\"257\":\"Blaziken\",\"258\":\"Mudkip\",\"259\":\"Marshtomp\",\"260\":\"Swampert\",\"261\":\"Poochyena\",\"262\":\"Mightyena\",\"263\":\"Zigzagoon\",\"264\":\"Linoone\",\"265\":\"Wurmple\",\"266\":\"Silcoon\",\"267\":\"Beautifly\",\"268\":\"Cascoon\",\"269\":\"Dustox\",\"270\":\"Lotad\",\"271\":\"Lombre\",\"272\":\"Ludicolo\",\"273\":\"Seedot\",\"274\":\"Nuzleaf\",\"275\":\"Shiftry\",\"276\":\"Taillow\",\"277\":\"Swellow\",\"278\":\"Wingull\",\"279\":\"Pelipper\",\"280\":\"Ralts\",\"281\":\"Kirlia\",\"282\":\"Gardevoir\",\"283\":\"Surskit\",\"284\":\"Masquerain\",\"285\":\"Shroomish\",\"286\":\"Breloom\",\"287\":\"Slakoth\",\"288\":\"Vigoroth\",\"289\":\"Slaking\",\"290\":\"Nincada\",\"291\":\"Ninjask\",\"292\":\"Shedinja\",\"293\":\"Whismur\",\"294\":\"Loudred\",\"295\":\"Exploud\",\"296\":\"Makuhita\",\"297\":\"Hariyama\",\"298\":\"Azurill\",\"299\":\"Nosepass\",\"300\":\"Skitty\",\"301\":\"Delcatty\",\"302\":\"Sableye\",\"303\":\"Mawile\",\"304\":\"Aron\",\"305\":\"Lairon\",\"306\":\"Aggron\",\"307\":\"Meditite\",\"308\":\"Medicham\",\"309\":\"Electrike\",\"310\":\"Manectric\",\"311\":\"Plusle\",\"312\":\"Minun\",\"313\":\"Volbeat\",\"314\":\"Illumise\",\"315\":\"Roselia\",\"316\":\"Gulpin\",\"317\":\"Swalot\",\"318\":\"Carvanha\",\"319\":\"Sharpedo\",\"320\":\"Wailmer\",\"321\":\"Wailord\",\"322\":\"Numel\",\"323\":\"Camerupt\",\"324\":\"Torkoal\",\"325\":\"Spoink\",\"326\":\"Grumpig\",\"327\":\"Spinda\",\"328\":\"Trapinch\",\"329\":\"Vibrava\",\"330\":\"Flygon\",\"331\":\"Cacnea\",\"332\":\"Cacturne\",\"333\":\"Swablu\",\"334\":\"Altaria\",\"335\":\"Zangoose\",\"336\":\"Seviper\",\"337\":\"Lunatone\",\"338\":\"Solrock\",\"339\":\"Barboach\",\"340\":\"Whiscash\",\"341\":\"Corphish\",\"342\":\"Crawdaunt\",\"343\":\"Baltoy\",\"344\":\"Claydol\",\"345\":\"Lileep\",\"346\":\"Cradily\",\"347\":\"Anorith\",\"348\":\"Armaldo\",\"349\":\"Feebas\",\"350\":\"Milotic\",\"351\":\"Castform\",\"352\":\"Kecleon\",\"353\":\"Shuppet\",\"354\":\"Banette\",\"355\":\"Duskull\",\"356\":\"Dusclops\",\"357\":\"Tropius\",\"358\":\"Chimecho\",\"359\":\"Absol\",\"360\":\"Wynaut\",\"361\":\"Snorunt\",\"362\":\"Glalie\",\"363\":\"Spheal\",\"364\":\"Sealeo\",\"365\":\"Walrein\",\"366\":\"Clamperl\",\"367\":\"Huntail\",\"368\":\"Gorebyss\",\"369\":\"Relicanth\",\"370\":\"Luvdisc\",\"371\":\"Bagon\",\"372\":\"Shelgon\",\"373\":\"Salamence\",\"374\":\"Beldum\",\"375\":\"Metang\",\"376\":\"Metagross\",\"377\":\"Regirock\",\"378\":\"Regice\",\"379\":\"Registeel\",\"380\":\"Latias\",\"381\":\"Latios\",\"382\":\"Kyogre\",\"383\":\"Groudon\",\"384\":\"Rayquaza\",\"385\":\"Jirachi\",\"386\":\"Deoxys\",\"387\":\"Turtwig\",\"388\":\"Grotle\",\"389\":\"Torterra\",\"390\":\"Chimchar\",\"391\":\"Monferno\",\"392\":\"Infernape\",\"393\":\"Piplup\",\"394\":\"Prinplup\",\"395\":\"Empoleon\",\"396\":\"Starly\",\"397\":\"Staravia\",\"398\":\"Staraptor\",\"399\":\"Bidoof\",\"400\":\"Bibarel\",\"401\":\"Kricketot\",\"402\":\"Kricketune\",\"403\":\"Shinx\",\"404\":\"Luxio\",\"405\":\"Luxray\",\"406\":\"Budew\",\"407\":\"Roserade\",\"408\":\"Cranidos\",\"409\":\"Rampardos\",\"410\":\"Shieldon\",\"411\":\"Bastiodon\",\"412\":\"Burmy\",\"413\":\"Wormadam\",\"414\":\"Mothim\",\"415\":\"Combee\",\"416\":\"Vespiquen\",\"417\":\"Pachirisu\",\"418\":\"Buizel\",\"419\":\"Floatzel\",\"420\":\"Cherubi\",\"421\":\"Cherrim\",\"422\":\"Shellos\",\"423\":\"Gastrodon\",\"424\":\"Ambipom\",\"425\":\"Drifloon\",\"426\":\"Drifblim\",\"427\":\"Buneary\",\"428\":\"Lopunny\",\"429\":\"Mismagius\",\"430\":\"Honchkrow\",\"431\":\"Glameow\",\"432\":\"Purugly\",\"433\":\"Chingling\",\"434\":\"Stunky\",\"435\":\"Skuntank\",\"436\":\"Bronzor\",\"437\":\"Bronzong\",\"438\":\"Bonsly\",\"439\":\"Mime Jr.\",\"440\":\"Happiny\",\"441\":\"Chatot\",\"442\":\"Spiritomb\",\"443\":\"Gible\",\"444\":\"Gabite\",\"445\":\"Garchomp\",\"446\":\"Munchlax\",\"447\":\"Riolu\",\"448\":\"Lucario\",\"449\":\"Hippopotas\",\"450\":\"Hippowdon\",\"451\":\"Skorupi\",\"452\":\"Drapion\",\"453\":\"Croagunk\",\"454\":\"Toxicroak\",\"455\":\"Carnivine\",\"456\":\"Finneon\",\"457\":\"Lumineon\",\"458\":\"Mantyke\",\"459\":\"Snover\",\"460\":\"Abomasnow\",\"461\":\"Weavile\",\"462\":\"Magnezone\",\"463\":\"Lickilicky\",\"464\":\"Rhyperior\",\"465\":\"Tangrowth\",\"466\":\"Electivire\",\"467\":\"Magmortar\",\"468\":\"Togekiss\",\"469\":\"Yanmega\",\"470\":\"Leafeon\",\"471\":\"Glaceon\",\"472\":\"Gliscor\",\"473\":\"Mamoswine\",\"474\":\"Porygon-Z\",\"475\":\"Gallade\",\"476\":\"Probopass\",\"477\":\"Dusknoir\",\"478\":\"Froslass\",\"479\":\"Rotom\",\"480\":\"Uxie\",\"481\":\"Mesprit\",\"482\":\"Azelf\",\"483\":\"Dialga\",\"484\":\"Palkia\",\"485\":\"Heatran\",\"486\":\"Regigigas\",\"487\":\"Giratina\",\"488\":\"Cresselia\",\"489\":\"Phione\",\"490\":\"Manaphy\",\"491\":\"Darkrai\",\"492\":\"Shaymin\",\"493\":\"Arceus\",\"494\":\"Victini\",\"495\":\"Snivy\",\"496\":\"Servine\",\"497\":\"Serperior\",\"498\":\"Tepig\",\"499\":\"Pignite\",\"500\":\"Emboar\",\"501\":\"Oshawott\",\"502\":\"Dewott\",\"503\":\"Samurott\",\"504\":\"Patrat\",\"505\":\"Watchog\",\"506\":\"Lillipup\",\"507\":\"Herdier\",\"508\":\"Stoutland\",\"509\":\"Purrloin\",\"510\":\"Liepard\",\"511\":\"Pansage\",\"512\":\"Simisage\",\"513\":\"Pansear\",\"514\":\"Simisear\",\"515\":\"Panpour\",\"516\":\"Simipour\",\"517\":\"Munna\",\"518\":\"Musharna\",\"519\":\"Pidove\",\"520\":\"Tranquill\",\"521\":\"Unfezant\",\"522\":\"Blitzle\",\"523\":\"Zebstrika\",\"524\":\"Roggenrola\",\"525\":\"Boldore\",\"526\":\"Gigalith\",\"527\":\"Woobat\",\"528\":\"Swoobat\",\"529\":\"Drilbur\",\"530\":\"Excadrill\",\"531\":\"Audino\",\"532\":\"Timburr\",\"533\":\"Gurdurr\",\"534\":\"Conkeldurr\",\"535\":\"Tympole\",\"536\":\"Palpitoad\",\"537\":\"Seismitoad\",\"538\":\"Throh\",\"539\":\"Sawk\",\"540\":\"Sewaddle\",\"541\":\"Swadloon\",\"542\":\"Leavanny\",\"543\":\"Venipede\",\"544\":\"Whirlipede\",\"545\":\"Scolipede\",\"546\":\"Cottonee\",\"547\":\"Whimsicott\",\"548\":\"Petilil\",\"549\":\"Lilligant\",\"550\":\"Basculin\",\"551\":\"Sandile\",\"552\":\"Krokorok\",\"553\":\"Krookodile\",\"554\":\"Darumaka\",\"555\":\"Darmanitan\",\"556\":\"Maractus\",\"557\":\"Dwebble\",\"558\":\"Crustle\",\"559\":\"Scraggy\",\"560\":\"Scrafty\",\"561\":\"Sigilyph\",\"562\":\"Yamask\",\"563\":\"Cofagrigus\",\"564\":\"Tirtouga\",\"565\":\"Carracosta\",\"566\":\"Archen\",\"567\":\"Archeops\",\"568\":\"Trubbish\",\"569\":\"Garbodor\",\"570\":\"Zorua\",\"571\":\"Zoroark\",\"572\":\"Minccino\",\"573\":\"Cinccino\",\"574\":\"Gothita\",\"575\":\"Gothorita\",\"576\":\"Gothitelle\",\"577\":\"Solosis\",\"578\":\"Duosion\",\"579\":\"Reuniclus\",\"580\":\"Ducklett\",\"581\":\"Swanna\",\"582\":\"Vanillite\",\"583\":\"Vanillish\",\"584\":\"Vanilluxe\",\"585\":\"Deerling\",\"586\":\"Sawsbuck\",\"587\":\"Emolga\",\"588\":\"Karrablast\",\"589\":\"Escavalier\",\"590\":\"Foongus\",\"591\":\"Amoonguss\",\"592\":\"Frillish\",\"593\":\"Jellicent\",\"594\":\"Alomomola\",\"595\":\"Joltik\",\"596\":\"Galvantula\",\"597\":\"Ferroseed\",\"598\":\"Ferrothorn\",\"599\":\"Klink\",\"600\":\"Klang\",\"601\":\"Klinklang\",\"602\":\"Tynamo\",\"603\":\"Eelektrik\",\"604\":\"Eelektross\",\"605\":\"Elgyem\",\"606\":\"Beheeyem\",\"607\":\"Litwick\",\"608\":\"Lampent\",\"609\":\"Chandelure\",\"610\":\"Axew\",\"611\":\"Fraxure\",\"612\":\"Haxorus\",\"613\":\"Cubchoo\",\"614\":\"Beartic\",\"615\":\"Cryogonal\",\"616\":\"Shelmet\",\"617\":\"Accelgor\",\"618\":\"Stunfisk\",\"619\":\"Mienfoo\",\"620\":\"Mienshao\",\"621\":\"Druddigon\",\"622\":\"Golett\",\"623\":\"Golurk\",\"624\":\"Pawniard\",\"625\":\"Bisharp\",\"626\":\"Bouffalant\",\"627\":\"Rufflet\",\"628\":\"Braviary\",\"629\":\"Vullaby\",\"630\":\"Mandibuzz\",\"631\":\"Heatmor\",\"632\":\"Durant\",\"633\":\"Deino\",\"634\":\"Zweilous\",\"635\":\"Hydreigon\",\"636\":\"Larvesta\",\"637\":\"Volcarona\",\"638\":\"Cobalion\",\"639\":\"Terrakion\",\"640\":\"Virizion\",\"641\":\"Tornadus\",\"642\":\"Thundurus\",\"643\":\"Reshiram\",\"644\":\"Zekrom\",\"645\":\"Landorus\",\"646\":\"Kyurem\",\"647\":\"Keldeo\",\"648\":\"Meloetta\",\"649\":\"Genesect\",\"650\":\"Chespin\",\"651\":\"Quilladin\",\"652\":\"Chesnaught\",\"653\":\"Fennekin\",\"654\":\"Braixen\",\"655\":\"Delphox\",\"656\":\"Froakie\",\"657\":\"Frogadier\",\"658\":\"Greninja\",\"659\":\"Bunnelby\",\"660\":\"Diggersby\",\"661\":\"Fletchling\",\"662\":\"Fletchinder\",\"663\":\"Talonflame\",\"664\":\"Scatterbug\",\"665\":\"Spewpa\",\"666\":\"Vivillon\",\"667\":\"Litleo\",\"668\":\"Pyroar\",\"669\":\"Flab\\u00e9b\\u00e9\",\"670\":\"Floette\",\"671\":\"Florges\",\"672\":\"Skiddo\",\"673\":\"Gogoat\",\"674\":\"Pancham\",\"675\":\"Pangoro\",\"676\":\"Furfrou\",\"677\":\"Espurr\",\"678\":\"Meowstic\",\"679\":\"Honedge\",\"680\":\"Doublade\",\"681\":\"Aegislash\",\"682\":\"Spritzee\",\"683\":\"Aromatisse\",\"684\":\"Swirlix\",\"685\":\"Slurpuff\",\"686\":\"Inkay\",\"687\":\"Malamar\",\"688\":\"Binacle\",\"689\":\"Barbaracle\",\"690\":\"Skrelp\",\"691\":\"Dragalge\",\"692\":\"Clauncher\",\"693\":\"Clawitzer\",\"694\":\"Helioptile\",\"695\":\"Heliolisk\",\"696\":\"Tyrunt\",\"697\":\"Tyrantrum\",\"698\":\"Amaura\",\"699\":\"Aurorus\",\"700\":\"Sylveon\",\"701\":\"Hawlucha\",\"702\":\"Dedenne\",\"703\":\"Carbink\",\"704\":\"Goomy\",\"705\":\"Sliggoo\",\"706\":\"Goodra\",\"707\":\"Klefki\",\"708\":\"Phantump\",\"709\":\"Trevenant\",\"710\":\"Pumpkaboo\",\"711\":\"Gourgeist\",\"712\":\"Bergmite\",\"713\":\"Avalugg\",\"714\":\"Noibat\",\"715\":\"Noivern\",\"716\":\"Xerneas\",\"717\":\"Yveltal\",\"718\":\"Zygarde\",\"719\":\"Diancie\",\"720\":\"Hoopa\",\"721\":\"Volcanion\"}";
//        pokemonIdNameMap = (Map<String, String>) JSONArray.parse(str);

        rarePokemonIdNameMap.put("3", "妙蛙花");
        rarePokemonIdNameMap.put("6", "喷火龙");
        rarePokemonIdNameMap.put("9", "水箭龟");
        rarePokemonIdNameMap.put("12", "巴大蝶");
        rarePokemonIdNameMap.put("15", "大针蜂");
        rarePokemonIdNameMap.put("26", "雷丘");
        rarePokemonIdNameMap.put("28", "穿山王");
        rarePokemonIdNameMap.put("31", "尼多后");
        rarePokemonIdNameMap.put("34", "尼多王");
        rarePokemonIdNameMap.put("36", "皮可西");
        rarePokemonIdNameMap.put("38", "九尾");
        rarePokemonIdNameMap.put("40", "胖可丁");
        rarePokemonIdNameMap.put("45", "霸王花");
        rarePokemonIdNameMap.put("51", "三地鼠");
        rarePokemonIdNameMap.put("57", "火爆猴");
        rarePokemonIdNameMap.put("59", "风速狗");
        rarePokemonIdNameMap.put("65", "胡迪");
        rarePokemonIdNameMap.put("68", "怪力");
        rarePokemonIdNameMap.put("71", "大食花");
        rarePokemonIdNameMap.put("76", "隆隆岩");
        rarePokemonIdNameMap.put("78", "烈焰马");
        rarePokemonIdNameMap.put("82", "三磁怪");
        rarePokemonIdNameMap.put("83", "大葱鸭");
        rarePokemonIdNameMap.put("87", "白海狮");
        rarePokemonIdNameMap.put("89", "臭臭泥");
        rarePokemonIdNameMap.put("91", "刺甲贝");
        rarePokemonIdNameMap.put("94", "耿鬼");
//        rarePokemonIdNameMap.put("97", "素利拍");
        rarePokemonIdNameMap.put("103", "椰蛋树");
        rarePokemonIdNameMap.put("105", "嘎啦嘎啦");
        rarePokemonIdNameMap.put("110", "双弹瓦斯");
        rarePokemonIdNameMap.put("112", "铁甲暴龙");
        rarePokemonIdNameMap.put("113", "吉利蛋");
        rarePokemonIdNameMap.put("115", "袋龙");
        rarePokemonIdNameMap.put("122", "魔偶");
        rarePokemonIdNameMap.put("130", "暴鲤龙");
        rarePokemonIdNameMap.put("131", "乘龙");
        rarePokemonIdNameMap.put("132", "百变怪");
        rarePokemonIdNameMap.put("134", "水伊布");
        rarePokemonIdNameMap.put("135", "雷精灵");
        rarePokemonIdNameMap.put("136", "火伊布");
        rarePokemonIdNameMap.put("137", "3D龙");
        rarePokemonIdNameMap.put("139", "多刺菊石兽");
        rarePokemonIdNameMap.put("141", "镰刀怪");
        rarePokemonIdNameMap.put("142", "化石翼龙");
        rarePokemonIdNameMap.put("143", "卡比兽");
        rarePokemonIdNameMap.put("144", "急冻鸟");
        rarePokemonIdNameMap.put("145", "闪电鸟");
        rarePokemonIdNameMap.put("146", "火焰鸟");
        rarePokemonIdNameMap.put("149", "快龙");
    }

    /**
     * 初始化扫描的坐标
     */
    private void initLatitudeAndLongitudeList() {
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(-33.95247360616282, -33.77343983379775, 150.9143829345703, 151.25890731811523));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(51.40749369152795, 51.56255861691012, -0.30782958984375, 0.14556884765625));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(35.63776694133777, 35.88682489453265, 139.45838928222656, 139.932861328125));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(33.84703735381736, 34.07086232376631, -118.5150146484375, -118.14010620117188));
        latitudeAndLongitudeList.add(new LatitudeAndLongitude(-34.936185628294744, -34.906275829530244, 138.58858108520508, 138.6390495300293));
    }
}

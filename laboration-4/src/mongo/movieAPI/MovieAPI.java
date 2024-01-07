package mongo.movieAPI;

/**
 * LABORATION 4: Gabriel Larsson
 * Samarbetat med:
 * Sandra Johannesson
 * Claudia Cheh
 */

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.text.WordUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;



public class MovieAPI {

    public static void main(String[] args) throws IOException {
        String connString;
        Logger logger = LoggerFactory.getLogger(MovieAPI.class);
        InputStream input = new FileInputStream("connection.properties");
            Properties prop = new Properties();
            prop.load(input);
            connString = prop.getProperty("db.connection_string");
            logger.info(connString);
            ConnectionString connectionString = new ConnectionString(connString);
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
            logger.info(prop.getProperty("db.name"));

            
            /** 
             * GET /test
             * Detta är en test-endpoint.
             * Även ifall get fungerade korrekt.
            */
            get("/test", (req, res) -> {
                logger.info("Test endpoint kallad (GET).");
                res.type("application/json");
                return "{\"message\": \"(get)Test endpoisadasadst\"}";
            });
            /** 
             * POST /test
             * Detta är en test-endpoint för att se ifall post fungerar korrekt.
             * 
            */
            post("/test", (req, res) -> {
                logger.info("Test endpoint kallad. (POST) ");
                res.type("application/json");
                return "{\"message\": \"(post) Test endpoisadasadst\"}";
            });
            
            
          /** 
           * GET /title/{name}
           * Detta är en endpoint för att hämta ut information om en specifik film, baserat på dess titel.
           * Vi skapar en variabel för att definiera vilken mongo collection vi vill hämta information ifrån, i detta fall är det movies.
           * En ny variabel "filter" skapas, detta gör vi för att se till att första bokstaven är stor i filmnamnet, detta då mongoDB är case-sensitive
           * Använder logger för att se ifall sökningen blev korrekt.
           * 
           * Definierar myDoc med ett sökfilter som söker efter vilken titel vi skrev in, tar sedan bort fälten:
           * 	#id,
           *	#poster,
           *    #cast,
           * 	#fullplot,
           * 
           * Sedan returneras det första resultatet ifall något hittas.
           * Att ett resultat finns kontrollerar vi genom att testa ifall myDoc som vi definierade tidigare inte är lika med null, alltså ett dokument finns.
           * Om detta är sant så loggar vi att ett resultat kommer att returneras och sedan returneras det. Statuskoden sätts även till 200.
           * 
           * Om inget resultat hittades så loggar vi ett felmedellande; ingen film hittades. Och returnerar statuskoden 404
           * Ett medelande returneras i JSON format att inget resultat för den titeln hittades.
           */
            get("/title/:name", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = req.params("name").toLowerCase();
                filter = WordUtils.capitalizeFully(filter);
                logger.info("Filtrerar filmer efter: " + filter);

                Document movie = collection.find(Filters.eq("title", filter))
                        .projection(new Document("_id", 0)
                                .append("poster", 0)
                                .append("cast", 0)
                                .append("fullplot", 0))
                        .first();

                if (movie != null) { 
                    logger.info("Returenrar resultatet i JSON format.");
                    res.status(200);
                    return movie.toJson();
                    
                    
                } else {
                    logger.error("Ingen film hittades...");
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"Inga resultat för: " + "'" + filter + "'" + " hittades. Testa att söka på en ny filmtitel.\"}";
                }
            });
            
            /** 
             * GET /fullplot/{title}
             * Den här endpointen tar en filmtitel som input, och returnerar den filmens fullplot
               fält från mongoDB databasen.
             * Vi definierar dokumentet att hämta från movies collectionen
             * Formaterar input så att första bokstäverna är stora och andra är små
             * Loggar att filmen söks på, för att vara säkra på att funktionen kallades på, och
               att sökningen sker på ett korrekt sätt.
             * Vi skapar ett filter för att söka med input titeln. Och returnerar ENDAST fullplot fältet.
             * .first() returnerar det första resultatet som hittades.
             * IF (film hittades) :
             *  - Om movies dokumentet inte är null, och har fullplot fältet så hittades en film
             *  - Statuskoden sätts till 200, och vi returnerar resultatet i JSON format.
             * 
             * ELSE (film hittades inte) :
             *  - Loggar ett felmeddelande att något gick fel.
             *  - Statuskoden sätts till 404 för not found.
             *  - Returnerar ett felmeddelande att ingen film med den sökta titeln hittades.
            */
            
            get("/fullplot/:title", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                String title = req.params("title").toLowerCase();
                title = WordUtils.capitalizeFully(title);
                logger.info("Söker efter filmer med titel: " + title);

                Document movie = collection.find(Filters.eq("title", title))
                                          .projection(Projections.fields(Projections.excludeId(), Projections.include("fullplot")))
                                          .first();

                if (movie != null && movie.containsKey("fullplot")) {
                    logger.info("Film hittades! Returnerar 'fullplot'.");
                    res.status(200);
                    res.type("application/json");
                    return movie.toJson();
                } else {
                    logger.error("Film hittades ej, eller hade ingen fullplot.");
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"Ingen film hittades för: " + title + "\"}";
                }
            });
            
            
            /** 
             * GET /cast/{title}
             * Den här endpointen tar en filmtitel som input, och returnerar cast från den filmen.
             * Vi definierar dokumentet att hämta från movies collectionen
             * Formaterar input så att första bokstäverna är stora och andra är små
             * Loggar att filmen söks på, för att vara säkra på att funktionen kallades på, och
               att sökningen sker på ett korrekt sätt. 
             * Filtrerar efter titel på samma sätt, men den här gången vill vi endast 
               returnera filmtiteln och dess cast.
             * IF (film hittades) :
             *  - Om movies dokumentet inte är null, och har cast fältet så hittades en film
             *  - Statuskoden sätts till 200, och vi returnerar resultatet i JSON format.
             * 
             * ELSE (film hittades inte) :
             *  - Loggar ett felmeddelande att något gick fel.
             *  - Statuskoden sätts till 404 för not found.
             *  - Returnerar ett felmeddelande att ingen film med den sökta titeln hittades.
            */
            get("/cast/:title", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                String title = req.params("title").toLowerCase();
                title = WordUtils.capitalizeFully(title);
                logger.info("Söker efter filmtitel: " + title);

                Document movie = collection.find(Filters.eq("title", title))
                                          .projection(Projections.fields(
                                              Projections.excludeId(), Projections.include("title", "cast")))
                                          .first();

                if (movie != null && movie.containsKey("cast")) {
                    logger.info("Film hittades! Returnerar cast.");
                    res.status(200);
                    res.type("application/json");
                    return movie.toJson();
                } else {
                    logger.error("Film hittades ej, eller hade ingen cast.");
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"Ingen film hittades för: " + title + "\"}";
                }
            });
            
            /** 
             * POST /title (args)
             * Den här endpointen lägger till en ny film i databasen.
             * Den kräver att all information som behövs skickas med post requesten som en body
             * Använder en try, catch sats för att testa för fel vid inläggning.
             * 
             * Try (lägga till en film lyckades):
             *  - Gör datan(body) vi skickar in till ett JSON Objekt
             *  - Använder inserOne() för att lägga till en ny post i databasen.
             *  - Sätter statuskoden till 202 accepterad statuskoden.
             *  - En tom sträng returneras.
             * 
             * Catch (något gick fel): 
             *  - Statuskoden blir 400 (Fel vid inläggning)
             *  - Returnerar ett felmeddelande i JSON format att filmen inte lagts till och att man ska
                - kontrollera indatan.
            */
            
            post("/title", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                try {
                    JsonObject bodyData = JsonParser.parseString(req.body()).getAsJsonObject(); 
                    collection.insertOne(Document.parse(bodyData.toString()));
                    res.status(202);
                    return "";
                    
                } catch (Exception e) {
                	logger.error("Film lades ej till, kontrollera in datan.");
                    res.status(400);
                    res.type("application/json");
                    return "{\"message\": \"Något gick fel, kontrollera indatan.\"}";
                }
            });
            
            /** 
             * GET /genre/{genres}
             * Den här endpointen tar en genre som input och returnerar MAX 10 filmer från den gengern.
             * antalFilmer räknar hur många filmresultat som hittades.
             * Formaterar input så att första bokstäverna är stora och andra är små
             * Loggar att filmen söks på, för att vara säkra på att funktionen kallades på, och
               att sökningen sker på ett korrekt sätt. 
             * Vi använder en FindIterable då genres är en array, alltså det kan finnas flera gengrar 
               på en film. Vi måste därför gå igenom alla gengrar för filmen. Med limit 10.
             * vi skapar en JsonArray som heter filmLista.
             * 
             * IF (om iteratorn har ett nästa resultat):
             *  - Vi använder hasNext för att kontrollera om det finns en nästa film.
             *  - Vi hämtar hela dokumentet och tar bort:
             * 		#id,
             *      #poster, 
             * 		#cast,
             * 		#fullplot
             *  - Konverterar dokumentet till ett JsonObject med Gson och lägg till det i filmLista.
             *  - Lägger till en radbrytning för en bättre output.
             * 
             * ELSE (Ingen film hittades):
             *  - Statuskoden sätts till 404 för not found.
             *  - Returnerar en JSON respons att ingen film hittades.
            */
            
            get("/genre/:genres", (req, res) -> {
            	int antalFilmer = 0;
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = req.params("genres").toLowerCase();
                
                filter = WordUtils.capitalizeFully(filter);
                logger.info("Söker efter genre: " + filter);
                
                FindIterable<Document> result = collection.find(Filters.eq("genres", filter)).limit(10);
                JsonArray filmLista = new JsonArray();
                
                if (result.iterator().hasNext()) {
                    for (Document doc : result) {
                    	
                        doc.remove("_id");
                        doc.remove("poster");
                        doc.remove("cast");
                        doc.remove("fullplot");
                        
                        JsonObject jsonFilm = new Gson().fromJson(doc.toJson(), JsonObject.class);
                        filmLista.add(jsonFilm);
                        filmLista.add("\n");
                        antalFilmer++;
                    }
                } else {
                    res.type("application/json");
                    res.status(404);
                    return "{\"message\": \"Ingen sådan genre hittades. Sök på en annan genre.\"}";
                }
                logger.info("Hittade: " + antalFilmer + " antal filmer.");
                JsonObject response = new JsonObject();
                response.add("movies", filmLista);
                return response;
            });
            
            /**
             * GET /actor/{actor}
             * Denna endpoint tar en skådespelares namn som input och returnerar 
               upp till 10 filmer där skådespelaren medverkar.
             * Formaterar input så att första bokstaven i varje ord är stor och resten små.
             * Loggar sökningen efter filmer med den specificerade skådespelaren för att
               säkerställa att den fungerar korrekt.
             * Vi använder en FindIterable för att hämta filmer baserat på skådespelaren.
             * En JsonArray med filmernas titlar skapas för att lagra resultaten.
             * 
             * IF (om iteratorn har ett nästa resultat):
             *   - Kontrollerar om det finns matchande filmer.
             *   - Hämtar titeln på varje film och lägger till den i movieTitles.
             *   - Sätter HTTP status till 200 för OK.
             *   - Skapar en JSON respons som innehåller filmernas titlar under nyckeln "movies".
             * 
             * ELSE (Ingen matchande film hittades):
             *   - Sätter HTTP svarstatus till 404 för not found.
             *   - Returnerar en JSON respons att skådespelaren inte hittades i någon film.
             */
            get("/actor/:actor", (req, res) -> {
            	int antal = 0;
                MongoCollection<Document> collection = database.getCollection("movies");
                String actor = req.params("actor").toLowerCase();
                actor = WordUtils.capitalizeFully(actor);
                
                logger.info("Söker efter skådespelare: " + actor);

                FindIterable<Document> result = collection.find(Filters.eq("cast", actor)).limit(10);
                
                JsonArray movieTitles = new JsonArray();

                if (result.iterator().hasNext()) {
                    for (Document doc : result) {
                        String title = doc.getString("title");
                        movieTitles.add(title);
                        antal++;
                    }
                    
                    logger.info("Hittade: " + antal + " antal filmer.");
                    res.type("application/json");
                    res.status(200);
                    JsonObject response = new JsonObject();
                    response.add("movies", movieTitles);
                    return response;
                    
                } else {
                    res.type("application/json");
                    res.status(404);
                    return "{\"message\": \"Den skådespelaren hittades inte i några filmer.\"}";
                }
            });
            
    }}

package se.kau.isgb33;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

public class Stub {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(Stub.class);

        JFrame f = new JFrame("M0vie Searcher");
        f.setSize(400, 500);
        f.setLayout(null);

        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setBounds(10, 10, 365, 400);
        area.setEditable(false); /**gör så att man inte kan redigera resulatet */

        JTextField t = new JTextField("");
        t.setBounds(10, 415, 260, 40);

        JButton b = new JButton("Search");
        b.setBounds(275, 415, 100, 40);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent q) {
                logger.info("Knapp tryck upptäckt");
                /** Starta anslutingen till mongodb */
                String connString;

                try (InputStream input = new FileInputStream("connection.properties")) {

                    Properties prop = new Properties();
                    prop.load(input);
                    connString = prop.getProperty("db.connection_string");
                    logger.info(connString);

                    ConnectionString connectionString = new ConnectionString(connString);
                    MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                            .build();
                    MongoClient mongoClient = MongoClients.create(settings);

                    /** ansluting lyckades */

                     /** använder db.name som databas namn och hämtar från collectionen movies */                   
                    MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
                    MongoCollection<Document> collection = database.getCollection("movies");
  
                    /** 
                     * Skapar en MongoCursor för den nuvarande collectionen movies
                     * Filtrerar efter arrayen Genres och hämtar alla platser i arrayen
                     * Sorterar i ordningen Z-A efter titel
                     * Returnerar en iterator för att gå igenom arrauy
                     */
                    MongoCursor<Document> cursor = collection
                    .find(Filters.all("genres", Arrays.asList(t.getText())))
                        .sort(Sorts.descending("title"))
                        .iterator();
                    
                        /** 
                         * Hämta titel och år och felhantering
                         * Skapar en counter som börjar på 0, för att veta när 10 exempel hämtats
                         * Definierar en resultat variabel
                         * Skapar en while loop som kommer att pågå sålänge det finns ett till resultat och counter är mindre än 10
                         * doc är en variabel med nästa plats i databasen
                         * name är variabeln där vi lagrar titel
                         * yearObj och yearStr behövs för att hantera året
                         */
                    int count = 0;
                    StringBuilder result = new StringBuilder();
                    while (cursor.hasNext() && count < 10) {
                        Document doc = cursor.next();
                        String name = doc.getString("title");
                        
                        Object yearObj = doc.get("year");
                        String yearStr; 
                        
                        /**  ** Felhantering **
                         * Efetsom att vissa år slutar med ett é så måste detta testas för innan resultatet kan skrivas ut
                         * Om yearObj är en Integer, sätts yearStr till det parsade värdet av den integern.
                         * Om yearObk är en String så sätts yearValue till string värdet av yearObj
                         * Om yearValue kan parsas till en int så sätts yearStr till värdet av year.
                         * Catch fångar felet NumberFormatException och felmedelanden skrivs ut i terminalen.
                         * Löser felet att parsingen inte fungerade så används det originella string värdet.
                         * Om inget av detta fungerar så blir yearStr till ""
                         * Sedan sätts resultatet till hela titel, år 
                         * count++ adderar 1 till countern
                         */
                        
                        if (yearObj instanceof Integer) {
                            yearStr = String.valueOf((Integer) yearObj);

                        } else if (yearObj instanceof String) {
                            String yearValue = (String) yearObj;

                            try {
                                int year = Integer.parseInt(yearValue);
                            yearStr = String.valueOf(year); 

                            } catch (NumberFormatException e) {
                                
                                logger.error("[ERROR] INVALID YEAR VALUE: " +yearValue);
                                logger.warn("[ALERT] Using original string value");
                                logger.info("Resolved");
                               
                            yearStr = yearValue; 
                            }
                        } 
                        
                        else {
                            yearStr = ""; 
                        }
                        
                        result.append(name).append(", ").append(yearStr).append("\n");

                        count++;
                    }
                    
                    /** 
                     * Stänger av mongo cursorn 
                     * Om count i slutet är > 0 så sätts texten i textarean till resultatet
                     * Ifall stringen var tom så skrivs ett separat felmedelande ut
                     * Annars kommer ett felmeddelande att skrivas ut
                    */
                    
                    cursor.close();
                    
                    if (count > 0) {

                        area.setText(result.toString());
                    } else {

                        if (t.getText().isEmpty()) {
                            logger.warn("[ALERT] Empty input detected.");
                            area.setText("Skriv in en genre och försök igen.");
                        } 
                        else {
                            logger.warn("[ALERT] No results found for: " + t.getText());
                            area.setText("Ingen film matchade kategorin: " + t.getText());
                        }
                    }
                    
                
                    /** Fångar standardfel */
                } catch (FileNotFoundException e) {
                    logger.info("[error] File not found!");
                    area.setText("File Not Found try again");
                    e.printStackTrace();
                } catch (IOException e) {
                    logger.error("[ERROR] IO Errpor! någon fraudar ner sig");
                    area.setText("[ERROR] IO Exception detected. Check terminal stacktrace for more information");
                    e.printStackTrace();
                }
            }
        });

        f.add(area);
        f.add(t);
        f.add(b);

        f.setVisible(true);
    }
}

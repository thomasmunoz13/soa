package flight.data;

import com.github.fakemongo.Fongo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import flight.service.Flight;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlightQueryHandlerTest {

    private Fongo client;
    private MongoDatabase db;
    private MongoCollection<Document> flights;

    @Test
    void queryHandlerTest() {
        client = new Fongo("mongo server handler test");
        db = client.getDatabase("flight");
        db.drop();
        flights = db.getCollection("flights");

        Flight parisNice = new Flight("Paris", "Nice", LocalDate.of(2017, 12, 24), 50,
                Flight.JourneyType.DIRECT, Duration.ofHours(1), Flight.Category.ECO, "Air France");
        Flight parisNice2 = new Flight("Paris", "Nice", LocalDate.of(2017, 12, 21), 45,
                Flight.JourneyType.DIRECT, Duration.ofMinutes(85), Flight.Category.ECO, "Air France");
        Flight parisNice3 = new Flight("Paris", "Nice", LocalDate.of(2017, 12, 21), 40,
                Flight.JourneyType.DIRECT, Duration.ofMinutes(80), Flight.Category.ECO, "EasyJet");
        Flight parisNewYork = new Flight("Paris", "New-York", LocalDate.of(2017, 8, 15), 156,
                Flight.JourneyType.INDIRECT, Duration.ofHours(11), Flight.Category.FIRST, "Iberia");
        flights.insertMany(Arrays.asList(parisNice.toBson(), parisNice2.toBson(), parisNice3.toBson(), parisNewYork.toBson()));

        Query query = new Query(
                and(eq("origin", "Paris"),
                        eq("destination", "Nice"),
                        eq("date", LocalDate.of(2017, 12, 21).toString()),
                        eq("journeyType", Flight.JourneyType.DIRECT.toString()),
                        eq("category", Flight.Category.ECO.toString())),
                orderBy(ascending("price")));


        FlightQueryHandler queryHandler = new FlightQueryHandler(query);
        List<Document> result = queryHandler.performOn(flights);

        assertEquals(2, result.size());
        assertEquals(parisNice3, new Flight(result.get(0)));
        assertEquals(parisNice2, new Flight(result.get(result.size()-1)));
    }
}

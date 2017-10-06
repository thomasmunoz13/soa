package hotel.data.sorter;

import hotel.data.exception.IllegalSorterValueException;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Sorts.*;

public class PriceOrderFB implements SorterBuilder<Bson> {
    @Override
    public Bson buildSorter(Document bson) throws IllegalSorterValueException {
        if (!bson.containsKey("order")) {
            return null;
        }

        String order = bson.getString("order");
        if (order.equalsIgnoreCase("ascending")) {
            return ascending("nightPrice");
        } else if (order.equalsIgnoreCase("descending")) {
            return descending("nightPrice");
        } else {
            throw new IllegalSorterValueException("incorrect 'order' value");
        }
    }
}

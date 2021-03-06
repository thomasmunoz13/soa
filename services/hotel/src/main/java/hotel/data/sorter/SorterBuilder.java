package hotel.data.sorter;

import hotel.data.exception.IllegalSorterValueException;
import org.bson.Document;

public interface SorterBuilder<T> {
    T buildSorter(Document bson) throws IllegalSorterValueException;

}

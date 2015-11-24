/**
 * Interfejs zadania.
 *
 * @author oramus
 *
 */
public interface TaskInterface {
    /**
     * Zwraca numer pierwszej kolejki, do ktorej zadanie ma trafic.
     *
     * @return numer pierwszej kolejki
     */
    public int getFirstQueue();

    /**
     * Zwraca numer ostatniej kolejki, do ktorej zadanie ma trafic
     *
     * @return numer ostatniej kolejki
     */
    public int getLastQueue();

    /**
     * Zwraca numer identyfikacyjny zadania
     *
     * @return numer identyfikacyjny zadania
     */
    public int getTaskID();

    /**
     * Zwraca informacje o tym, czy zadanie chce aby
     * kolejnosc byla przez nie zachowana.
     * @return <tt>true</tt> - kolejnosc ma byc przestrzegana, <tt>false</tt> - zadanie, dla
     * ktorego kolejnosc wykonania nie ma znaczenia.
     */
    public boolean keepOrder();

    /**
     * Metoda, ktora nalezy wykonac aby otrzymac obiekt do umieszczenia
     * w kolejnej kolejce.
     *
     * @param queue numer <em>aktualnej</em> kolejki, w ktorej zadanie sie znajduje
     * @return obiekt do umieszczenia w <em>kolejnej</em> kolejce
     */
    public TaskInterface work( int queue );
}
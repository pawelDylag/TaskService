/**
 * Interfejs systemu rownoleglej obslugi zadan
 *
 * @author oramus
 *
 */
public interface SystemInterface {
    /**
     * Metoda ustala liczbe kolejek, ktora system ma oferowac uzytkownikom.
     * @param queues liczba kolejek
     */
    public void setNumberOfQueues( int queues );

    /**
     * Metoda ustala polityke uzycia zasobow. Tablica zawiera
     * maksymalna liczbe watkow, jaka moze byc uzyta do
     * wykonywania zadan z danej kolejki.
     *
     * @param maximumThreadsPerQueue tablica limitow watkow
     */
    public void setThreadsLimit( int[] maximumThreadsPerQueue );

    /**
     * Metoda pozwalajaca na umieszczenie w systemie zadania.
     * Metoda dziala i pozwala na umieszczanie w systemie zadan nawet w czasie
     * realizacji przez System zadan z kolejek.
     *
     * @param task zadanie do zrealizowania
     */
    public void addTask( TaskInterface task );
}
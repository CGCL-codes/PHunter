package analyze;

public class Pair<K , V > {

    public K key = null;
    public V value = null;
    public double sim = -1;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public Pair() {
    }

    public boolean isEmpty() {
        return key == null || value == null;
    }

//    @Override
//    public int compareTo(Pair<K, V> p1) {
//        if (value.equals(p1.getValue())) return key.compareTo(p1.getKey());
//        else return value.compareTo(p1.getValue());
//    }

    @Override
    public boolean equals(Object obj) {
//		System.out.println(obj.getClass() + "\t" + this.getClass());
        if (obj.getClass() != this.getClass()) return false;
        @SuppressWarnings("unchecked")
        Pair<K, V> pair = (Pair<K, V>) obj;
        return this.key.equals(pair.key) && this.value.equals(pair.value);
    }

    @Override
    public int hashCode() {
        return key.hashCode() + 31 * value.hashCode();
    }

    @Override
    public String toString() {
        return key.toString() + ":" + value.toString();
    }
}

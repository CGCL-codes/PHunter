package util;

import java.util.HashSet;

public class Pair<K extends Comparable<K>, V extends Comparable<V>> implements Comparable<Pair<K, V>>{
	private K key;
	private V value;
	
	public Pair(K k, V v) {
		key = k;
		value = v;
	}

	public K getKey() {
		return key;
	}
	
	public V getValue() {
		return value;
	}
	
	public boolean invalid() {
		return (key == null || value == null);
	}
	
	@Override
	public int compareTo(Pair<K, V> p1) {
		// TODO Auto-generated method stub
		if (value.equals(p1.getValue())) return key.compareTo(p1.getKey());
		else return value.compareTo(p1.getValue());
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
//		System.out.println(obj.getClass() + "\t" + this.getClass());
		if (obj.getClass() != this.getClass()) return false;
		@SuppressWarnings("unchecked")
		Pair<K, V> pair = (Pair<K, V>) obj;
		if (this.key.equals(pair.key) && this.value.equals(pair.value)) return true;
		else return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return key.hashCode() + 31 * value.hashCode();
	}

	@Override
	public String toString() {
		return key.toString() + ":" + value.toString();
	}
	
	public void modifyKey(K nkey) {
		key = nkey;
	}
	
	public void modifyValue(V nvalue) {
		value = nvalue;
	}
	
	public static void main(String[] args) {
		Pair<String, String> p1 = new Pair<String, String>("A", "B");
		Pair<String, String> p2 = new Pair<String, String>("A", "C");
		HashSet<Pair<String,String>> pairs = new HashSet<Pair<String,String>>();
		pairs.add(p1);
		pairs.add(p2);
		
	}
}

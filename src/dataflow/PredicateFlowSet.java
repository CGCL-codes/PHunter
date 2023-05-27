package dataflow;

import soot.jimple.ConditionExpr;
import soot.toolkits.scalar.AbstractFlowSet;

import java.util.*;
import java.util.function.Consumer;

public class PredicateFlowSet extends AbstractFlowSet<ConditionExpr> {

    public final Set<ConditionExpr> predicateSet = new HashSet<>();

    @Override
    public AbstractFlowSet<ConditionExpr> clone() {
        PredicateFlowSet clone = new PredicateFlowSet();
        clone.predicateSet.addAll(this.predicateSet);
        return clone;
    }

    @Override
    public boolean isEmpty() {
        return predicateSet.isEmpty();
    }

    @Override
    public int size() {
        return predicateSet.size();
    }

    @Override
    public void add(ConditionExpr predicate) {
        predicateSet.add(predicate);
    }

    public void addSet(Set<ConditionExpr> conSet) {
        predicateSet.addAll(conSet);
    }

    public PredicateFlowSet setTo(PredicateFlowSet another) {
        predicateSet.clear();
        predicateSet.addAll(another.predicateSet);
        return this;
    }

    public PredicateFlowSet union(PredicateFlowSet another) {
        predicateSet.addAll(another.predicateSet);
        return this;
    }

    public PredicateFlowSet duplicate() {
        PredicateFlowSet res = new PredicateFlowSet();
        res.predicateSet.addAll(predicateSet);
        return res;
    }

    @Override
    public void remove(ConditionExpr predicate) {
        predicateSet.remove(predicate);
    }

    @Override
    public boolean contains(ConditionExpr predicate) {
        return predicateSet.contains(predicate);
    }

    @Override
    public Iterator<ConditionExpr> iterator() {
        return predicateSet.iterator();
    }

    @Override
    public void forEach(Consumer<? super ConditionExpr> action) {
        super.forEach(action);
    }

    @Override
    public Spliterator<ConditionExpr> spliterator() {
        return super.spliterator();
    }

    @Override
    public List<ConditionExpr> toList() {
        return new ArrayList<>(predicateSet);
    }
}

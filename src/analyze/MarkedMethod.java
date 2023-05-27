package analyze;

import java.util.List;

class MarkedMethod implements Comparable<MarkedMethod> {
    boolean isPre;
    double sim = 0;
    float preFineGrainSimilarity = 0;
    float postFineGrainSimilarity = 0;
    MethodAttr m;

    PatchState state;

    List<Integer> patchRelatedLines;

    public MarkedMethod(boolean isPre, MethodAttr m, PatchState state, List<Integer> patchRelatedLines) {
        this.isPre = isPre;
        this.m = m;
        this.state = state;
        this.patchRelatedLines = patchRelatedLines;
    }

    public MarkedMethod(boolean isPre, MethodAttr m, PatchState state) {
        this.isPre = isPre;
        this.m = m;
        this.state = state;
        this.patchRelatedLines = null;
    }

    public MarkedMethod(double sim, MethodAttr m) {
        this.sim = sim;
        this.m = m;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MarkedMethod other = (MarkedMethod) obj;
        if (this.m.signature == null) {
            return other.m.signature == null;
        } else return this.m.signature.equals(other.m.signature);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        String str = this.m.signature;
        int itr = str.length() / 32;
        if (str.length() % 32 != 0)
            itr += 1;
        for (int i = 0; i < itr; i++) {
            if (i != itr - 1)
                hash += str.substring(32 * i, 32 * i + 32).hashCode();
            else hash += str.substring(32 * i).hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return this.m.signature;
    }

    @Override
    public int compareTo(MarkedMethod o) {
        return Double.compare(sim, o.sim);
    }
}

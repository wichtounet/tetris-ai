package code;

import java.util.BitSet;

public class State {
    BitSet bs;
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final State other = (State) obj;
        if (this.bs != other.bs && (this.bs == null || !this.bs.equals(other.bs))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.bs != null ? this.bs.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "State{" + "bs=" + bs + '}';
    }
}
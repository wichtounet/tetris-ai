package code;

public class BlockPosition {
    byte bx;
    byte rot;
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BlockPosition other = (BlockPosition) obj;
        if (this.bx != other.bx) {
            return false;
        }
        if (this.rot != other.rot) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + this.bx;
        hash = 71 * hash + this.rot;
        return hash;
    }

    @Override
    public String toString() {
        return "BlockPosition{" + "bx=" + bx + ", rot=" + rot + '}';
    }
}

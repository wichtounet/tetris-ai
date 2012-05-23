package code;

//A single tuple for the function value
class StateAction {
    private State state;
    private BlockPosition action;
    
    public StateAction(State state, BlockPosition action) {
        this.state = state;
        this.action = action;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StateAction other = (StateAction) obj;
        if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
            return false;
        }
        if (this.action != other.action && (this.action == null || !this.action.equals(other.action))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.state != null ? this.state.hashCode() : 0);
        hash = 17 * hash + (this.action != null ? this.action.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "StateAction{" + "state=" + state + ", action=" + action + '}';
    }
}

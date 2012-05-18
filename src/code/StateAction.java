package code;

//A single tuple for the function value
class StateAction {
    private State state;
    private BlockPosition action;
    
    public StateAction(State state, BlockPosition action) {
        this.state = state;
        this.action = action;
    }
    
    //TODO Hash and so on
}

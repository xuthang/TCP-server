public enum DIRECTION {
    LEFT, UP, RIGHT, DOWN;

    static public DIRECTION nextDirection(DIRECTION cur, DIRECTION turn)
    {
        if(turn == DIRECTION.UP || turn == DIRECTION.DOWN)
            return cur;

        if(turn == DIRECTION.LEFT)
        {
            return switch (cur) {
                case UP -> LEFT;
                case LEFT -> DOWN;
                case DOWN -> RIGHT;
                case RIGHT -> UP;
            };
        }
        else
        {
            return switch (cur) {
                case UP -> RIGHT;
                case RIGHT -> DOWN;
                case DOWN -> LEFT;
                case LEFT -> UP;
            };
        }
    }


}

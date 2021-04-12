import java.util.Objects;

public class Coordinates {
    public int xPos, yPos;

    public Coordinates(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return xPos == that.xPos && yPos == that.yPos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xPos, yPos);
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "xPos=" + xPos +
                ", yPos=" + yPos +
                '}';
    }
}
